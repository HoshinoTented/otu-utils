package com.github.hoshinotented.osuutils.dump.deser

import com.github.hoshinotented.osuutils.data.Mod
import com.google.common.io.LittleEndianDataInputStream
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import kala.collection.mutable.MutableMap
import java.io.ByteArrayInputStream
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.findAnnotations
import kotlin.time.Instant

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
        // Don't use ImmutableSeq in database structure directly when the element type has fixed size,
        // especially when the content may be large.
        // This will slow down the deserialization, use LazySeq if possible.
        val elemTyProj = typeArgs[0]
        val elemTy = elemTyProj.type ?: throw IllegalArgumentException("Unable to decode a star type")
        return bytes.readMany { _, _ -> parse(elemTy, this, null) }
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


    register(ModStarCache::class, object : Deserializer<ModStarCache> {
      override fun decode(
        typeArgs: List<KTypeProjection>,
        bytes: LittleEndianDataInputStream,
      ): ModStarCache {
        var pair = bytes.readIntFloatPair()
        return ModStarCache(Mod.asSet(pair.int), pair.float)
      }
    })

    register(ByteArray::class, object : Deserializer<ByteArray> {
      override fun decode(typeArgs: List<KTypeProjection>, bytes: LittleEndianDataInputStream): ByteArray? {
        val length = bytes.readInt()
        if (length == -1) return null
        return bytes.readNBytes(length)
      }
    })

    register(LazySeq::class, object : Deserializer<LazySeq<*>> {
      override fun decode(
        typeArgs: List<KTypeProjection>,
        bytes: LittleEndianDataInputStream,
      ): LazySeq<*> {
        val proj = typeArgs[0]
        val ty = proj.type ?: throw IllegalArgumentException("Unable to decode a star type")
        val sized = (ty.classifier as KClass<*>)
          .findAnnotations(Sized::class)
          .getOrNull(0) ?: throw IllegalArgumentException("Element in LazySeq must be Sized")
        val count = bytes.readInt()
        val totalSize = count * sized.bytes
        val buffer = bytes.readNBytes(totalSize)

        return LazySeq(lazy {
          val result = FreezableMutableList.create<Any?>()
          val stream = LittleEndianDataInputStream(ByteArrayInputStream(buffer))

          repeat(count) {
            result.append(parse(ty, stream, null))
          }

          result.freeze()
        })
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