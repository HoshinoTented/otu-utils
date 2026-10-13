package com.github.hoshinotented.osuutils.dump.deser

import com.github.hoshinotented.osuutils.data.Mod
import com.github.hoshinotented.osuutils.dump.OsuParseException
import com.google.common.io.LittleEndianDataInputStream
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import kala.collection.mutable.MutableMap
import kala.control.Option
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.time.Instant

/**
 * Throwing [OsuParseException] when the data itself is corrupted;
 * Throwing [IllegalArgumentException] when the data format is illegal.
 */
object Deserializers {
  @JvmStatic
  @Throws(IOException::class)
  fun readByte(bytes: LittleEndianDataInputStream): Byte {
    return bytes.readByte()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readShort(bytes: LittleEndianDataInputStream): Short {
    return bytes.readShort()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readInt(bytes: LittleEndianDataInputStream): Int {
    return bytes.readInt()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readLong(bytes: LittleEndianDataInputStream): Long {
    return bytes.readLong()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readFloat(bytes: LittleEndianDataInputStream): Float {
    return bytes.readFloat()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readDouble(bytes: LittleEndianDataInputStream): Double {
    return bytes.readDouble()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readBoolean(bytes: LittleEndianDataInputStream): Boolean {
    return bytes.readBoolean()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readString(bytes: LittleEndianDataInputStream): String {
    return bytes.readString()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readList(ty: KType, bytes: LittleEndianDataInputStream): ImmutableSeq<*> {
    return bytes.readMany { _, _ -> read(ty, bytes) }
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readLazyList(ty: KType, bytes: LittleEndianDataInputStream): LazySeq<*> {
    val sized = (ty.classifier as KClass<*>).findAnnotation<Sized>()
      ?: throw IllegalArgumentException("Element in LazySeq must be Sized: $ty")
    val count = bytes.readInt()
    val totalSize = count * sized.bytes
    val buffer = bytes.readNBytes(totalSize)

    return LazySeq(lazy {
      val result = FreezableMutableList.create<Any?>()
      val stream = LittleEndianDataInputStream(ByteArrayInputStream(buffer))

      repeat(count) {
        result.append(read(ty, stream))
      }

      result.freeze()
    })
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readByteArray(bytes: LittleEndianDataInputStream): ByteArray {
    val length = bytes.readInt()
    if (length == -1) return ByteArray(0)
    return bytes.readNBytes(length)
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readIntFloat(bytes: LittleEndianDataInputStream): IntFloatPair {
    return bytes.readIntFloatPair()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readInstant(bytes: LittleEndianDataInputStream): Instant {
    return bytes.readDateTime()
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readModStarCache(bytes: LittleEndianDataInputStream): ModStarCache {
    val pair = bytes.readIntFloatPair()
    return ModStarCache(Mod.asSet(pair.int), pair.float)
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readEnum(ty: Class<*>, bytes: LittleEndianDataInputStream): Enum<*> {
    val backing = ty.getAnnotation(BackingType::class.java)
    if (backing != null) {
      val backing = backing.value

      val isNumber = backing.isSubclassOf(Number::class)
      if (!isNumber) throw IllegalArgumentException("Backing type must be number")

      val result = tryReadPrimitive(backing.starProjectedType, bytes).orNull
        ?: throw IllegalArgumentException("Deserializer for backing type $backing of $ty is not found")

      val backingResult = (result as Number).toInt()
      val constant = ty.enumConstants[backingResult]

      return constant as Enum<*>
    } else {
      throw IllegalArgumentException("No backing type for enum: ${ty.simpleName}")
    }
  }

  @JvmStatic
  @Throws(IOException::class)
  fun readAny(clazz: KClass<*>, bytes: LittleEndianDataInputStream): Any {
    val con = clazz.primaryConstructor
      ?: throw IllegalArgumentException("Must have primary constructor")
    val params = con.parameters
    val args = arrayOfNulls<Any?>(params.size)

    for (i in params.indices) {
      val p = params[i]

      try {
        val result = read(p.type, bytes)
        args[i] = result
      } catch (e: OsuParseException) {
        throw e
      }
    }

    return con.call(*args)
  }

  @JvmStatic
  @Throws(IOException::class)
  fun tryReadPrimitive(ty: KType, bytes: LittleEndianDataInputStream): Option<Any> {
    val clazz = ty.classifier as KClass<*>

    val obj = when (clazz) {
      Byte::class -> readByte(bytes)
      Short::class -> readShort(bytes)
      Int::class -> readInt(bytes)
      Long::class -> readLong(bytes)
      Float::class -> readFloat(bytes)
      Double::class -> readDouble(bytes)
      Boolean::class -> readBoolean(bytes)
      String::class -> readString(bytes)
      ByteArray::class -> readByteArray(bytes)
      IntFloatPair::class -> readIntFloat(bytes)
      Instant::class -> readInstant(bytes)
      ModStarCache::class -> readModStarCache(bytes)

      ImmutableSeq::class -> readList(ty.arguments[0].type!!, bytes)
      LazySeq::class -> readLazyList(ty.arguments[0].type!!, bytes)

      else -> {
        return Option.none()
      }
    }

    return Option.some(obj)
  }

  @JvmStatic
  @Suppress("UNCHECKED_CAST")
  @Throws(IOException::class)
  fun <T: Any> read(ty: KClass<T>, bytes: LittleEndianDataInputStream): T {
    return read(ty.starProjectedType, bytes) as T
  }


  @JvmStatic
  @Throws(IOException::class)
  fun read(ty: KType, bytes: LittleEndianDataInputStream): Any {
    val clazz = ty.classifier as KClass<*>
    val jClz = clazz.java

    tryReadPrimitive(ty, bytes).orNull?.let {
      return it
    }

    if (jClz.isEnum) {
      return readEnum(jClz, bytes)
    }

    return readAny(clazz, bytes)
  }
}