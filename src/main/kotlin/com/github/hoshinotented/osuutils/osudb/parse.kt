package com.github.hoshinotented.osuutils.osudb

import com.google.common.io.LittleEndianDataInputStream
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import kala.collection.mutable.MutableMap
import java.nio.charset.Charset
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Instant

@FunctionalInterface
interface Deserializer<T : Any> {
  fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): T?
}

object Deserializers {
  private val deserializers: MutableMap<KClass<*>, Deserializer<*>> = MutableMap.create()
  
  init {
    register(Byte::class, object : Deserializer<Byte> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): Byte = bytes.readByte()
    })
    
    register(Short::class, object : Deserializer<Short> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): Short =
        bytes.readShort()
    })
    
    register(Int::class, object : Deserializer<Int> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): Int = bytes.readInt()
    })
    
    register(Long::class, object : Deserializer<Long> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): Long = bytes.readLong()
    })
    
    register(Float::class, object : Deserializer<Float> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): Float =
        bytes.readFloat()
    })
    
    register(Double::class, object : Deserializer<Double> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): Double =
        bytes.readDouble()
    })
    
    register(Boolean::class, object : Deserializer<Boolean> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): Boolean =
        bytes.readBoolean()
    })
    
    register(String::class, object : Deserializer<String> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): String? {
        return bytes.readString()
      }
    })
    
    register(ImmutableSeq::class, object : Deserializer<ImmutableSeq<*>> {
      override fun decode(
        typeArgs: List<KTypeProjection>,
        bytes: LittleEndianDataInputStream,
      ): ImmutableSeq<*> {
        val elemTyProj = typeArgs[0]
        val elemTy = elemTyProj.type ?: throw IllegalArgumentException("Unable to decode a star type")
        return bytes.readMany { parse(elemTy, this) }
      }
    })
    
    register(IntFloatPair::class, object : Deserializer<IntFloatPair> {
      override fun decode(
        typeArgs: List<KTypeProjection>,
        bytes: LittleEndianDataInputStream,
      ): IntFloatPair {
        return bytes.readIntFloatPair()
      }
    })
    
    register(Instant::class, object : Deserializer<Instant> {
      override fun decode(
        typeArgs: List<KTypeProjection>,
        bytes: LittleEndianDataInputStream,
      ): Instant {
        return bytes.readDateTime()
      }
    })
  }
  
  fun <T : Any> register(clazz: KClass<T>, deserializer: Deserializer<T>) {
    val exists = deserializers.put(clazz, deserializer)
    if (exists.isDefined) throw IllegalStateException("Duplicated deserializer: $clazz")
  }
  
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> find(clazz: KClass<T>): Deserializer<T>? {
    val de = deserializers.getOrNull(clazz) ?: return null
    return de as Deserializer<T>
  }
}

fun LittleEndianDataInputStream.readIntFloatPair(): IntFloatPair {
  val intIndicator = readByte()
  if (intIndicator != 0x08.toByte()) throw OsuParseException("Illegal format of Int-Float pair: ${intIndicator.toHexString()}")
  
  val int = readInt()
  
  val floatIndicator = readByte()
  if (floatIndicator != 0x0c.toByte()) throw OsuParseException("Illegal format of Int-Float pair: ${floatIndicator.toHexString()}")
  
  val float = readFloat()
  
  return IntFloatPair(int, float)
}

/**
 * Read many [R] from current stream, the next [Int] must indicates the number of [R]
 */
fun <R> LittleEndianDataInputStream.readMany(builder: LittleEndianDataInputStream.() -> R): ImmutableSeq<R> {
  val many = readInt()
  val buffer = FreezableMutableList.create<R>()
  
  for (i in 0 until many) {
    buffer.append(builder())
  }
  
  return buffer.freeze()
}

fun LittleEndianDataInputStream.readDateTime(): Instant {
  val ticks = readLong()
  // 1 ticks = 100 nano seconds
  // 1 milliseconds = 10000 ticks
  return Instant.fromEpochMilliseconds(ticks / 10000)
}

fun LittleEndianDataInputStream.readString(): String? {
  val indicator = this.read()
  if (indicator == 0x00) return null
  if (indicator != 0x0B) throw OsuParseException("Illegal format, unexpected byte: 0x${indicator.toHexString()}")
  
  val ulength = readULEB128()
  if (ulength > Int.MAX_VALUE.toULong()) throw OsuParseException("String too long: $ulength")
  val length = ulength.toInt()
  val bytes = readNBytes(length)
  
  return String(bytes, Charset.forName("UTF-8"))
}

/**
 * We assume all ULEB128 won't larger than [Long]
 */
fun LittleEndianDataInputStream.readULEB128(): ULong {
  var result: ULong = 0x0U
  var bitsRead = 0
  var byte: Int
  
  do {
    // max acceptable bitsRead is 70 for 64-bit long Long
    if (bitsRead > 64) {
      throw OsuParseException("Number too large, bits: $bitsRead")
    }
    
    byte = this.read()
    result = result or ((byte.toULong() and 0x7FU) shl bitsRead)
    bitsRead += 7
  } while (byte and 0x80 != 0)
  
  return result
}

fun parse(type: KType, bytes: LittleEndianDataInputStream): Any? {
  // pre parse
  val clazz = type.classifier ?: throw IllegalArgumentException("null")
  if (clazz !is KClass<*>) throw IllegalArgumentException("Must be KClass")
  
  val found = Deserializers.find(clazz)
  if (found != null) {
    val decoded = found.decode(type.arguments, bytes)
    if (!type.isMarkedNullable && decoded == null) {
      throw IllegalArgumentException("$type is not null while a null value is parsed.")
    }
    
    return decoded
  }
  
  val primeCon = clazz.primaryConstructor ?: throw IllegalArgumentException("Must have primary constructor")
  val params = primeCon.parameters
  val args = Array(params.size) { i ->
    val p = params[i]
    parse(p.type, bytes)
  }
  
  return primeCon.call(*args)
}