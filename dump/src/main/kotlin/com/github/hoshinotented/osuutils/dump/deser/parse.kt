package com.github.hoshinotented.osuutils.dump.deser

import com.github.hoshinotented.osuutils.dump.OsuParseException
import com.google.common.io.LittleEndianDataInputStream
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import kala.control.Option
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Instant

@FunctionalInterface
interface Deserializer<T : Any> {
  fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): T?
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
 * First parameter is index, the second one is amount.
 */
fun <R> LittleEndianDataInputStream.readMany(builder: LittleEndianDataInputStream.(Int, Int) -> R): ImmutableSeq<R> {
  val many = readInt()
  val buffer = FreezableMutableList.create<R>()
  
  for (i in 0 until many) {
    buffer.append(builder(i, many))
  }
  
  return buffer.freeze()
}

fun LittleEndianDataInputStream.readDateTime(): Instant {
  val ticks = readLong()
  return fromWindowsTicks(ticks)
}

fun LittleEndianDataInputStream.skipString() {
  val indicator = this.read()
  if (indicator == 0x00) return
  if (indicator != 0x0B) throw OsuParseException("Illegal format, unexpected byte: 0x${indicator.toHexString()}")
  
  val ulength = readULEB128()
  if (ulength > Int.MAX_VALUE.toULong()) throw OsuParseException("String too long: $ulength")
  val length = ulength.toInt()
  skipBytes(length)
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

@Suppress("UNCHECKED_CAST")
fun <T : Any> parse(type: KClass<T>, bytes: LittleEndianDataInputStream, handler: CorruptedHandler<T?>?): T? {
  return parse(type.createType(), bytes, handler) as T?
}

fun tryParse(clazz: KClass<*>, type: KType, bytes: LittleEndianDataInputStream): Option<Option<Any>> {
  val found = Deserializers.find(clazz)
  if (found != null) {
    val decoded = found.decode(type.arguments, bytes)
    if (!type.isMarkedNullable && decoded == null) {
      throw OsuParseException("$type is not null while a null value is parsed.")
    }

    return Option.some(Option.ofNullable(decoded))
  }

  return Option.none()
}

fun parse(type: KType, bytes: LittleEndianDataInputStream, handler: CorruptedHandler<*>?): Any? {
  // pre parse
  val clazz = type.classifier ?: throw IllegalArgumentException("null")
  if (clazz !is KClass<*>) throw IllegalArgumentException("Must be KClass")
  
  tryParse(clazz, type, bytes).orNull?.let {
    return it.orNull
  }

  val backing = clazz.findAnnotation<BackingType>()
  if (backing != null) {
    val isEnum = clazz.isSubclassOf(Enum::class)
    if (! isEnum) throw IllegalArgumentException("BackingType can only be used on enum class")

    val backing = backing.value

    val isNumber = backing.isSubclassOf(Number::class)
    if (! isNumber) throw IllegalArgumentException("Backing type must be number")

    val result = tryParse(backing, type, bytes).orNull
      ?: throw IllegalArgumentException("Deserializer for backing type $backing of $type is not found")

    val backingResult = (result.get() as Number).toInt()
    val constant = clazz.java.enumConstants[backingResult]

    return constant
  }
  
  val primeCon = clazz.primaryConstructor ?: throw IllegalArgumentException("Must have primary constructor")
  val params = primeCon.parameters
  val args = arrayOfNulls<Any?>(params.size)

  for (i in params.indices) {
    val p = params[i]
    try {
      args[i] = parse(p.type, bytes, null)
    } catch (e: OsuParseException) {
      if (handler != null) {
        return handler.onCorrupted(args)
      } else {
        throw e
      }
    }
  }

  return primeCon.call(*args)
}

private const val UNIX_EPOCH_MILLISECONDS = 62135596800000L

fun fromWindowsTicks(ticks: Long): Instant {
  // 10000 windows ticks = 1 milliseconds
  // 0 windows ticks is 0001-01-01T00:00:00.000Z, so we subtract [UNIX_EPOCH_MILLISECONDS]
  return Instant.fromEpochMilliseconds(ticks / 10000L - UNIX_EPOCH_MILLISECONDS)
}