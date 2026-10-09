package com.github.hoshinotented.osuutils.dump.cli

import com.github.hoshinotented.osuutils.data.Mod.Companion.toString
import com.github.hoshinotented.osuutils.dump.OsuParseException
import com.github.hoshinotented.osuutils.dump.deser.Deserializers
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readBoolean
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readByte
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readDouble
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readEnum
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readFloat
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readInstant
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readInt
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readLong
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readModStarCache
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readShort
import com.github.hoshinotented.osuutils.dump.deser.Deserializers.readString
import com.github.hoshinotented.osuutils.dump.deser.IntFloatPair
import com.github.hoshinotented.osuutils.dump.deser.LazySeq
import com.github.hoshinotented.osuutils.dump.deser.ModStarCache
import com.google.common.io.LittleEndianDataInputStream
import com.google.gson.stream.JsonWriter
import kala.collection.immutable.ImmutableSeq
import java.io.IOException
import java.io.UnsupportedEncodingException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.time.Instant

class DumpToJson(
  val `in`: LittleEndianDataInputStream,
  val out: JsonWriter,
  val dumpBinary: Boolean,
) {
  @Throws(IOException::class)
  fun readByte() {
    val result = readByte(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readShort() {
    val result = readShort(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readInt() {
    val result = readInt(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readLong() {
    val result = readLong(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readFloat() {
    val result = readFloat(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readDouble() {
    val result = readDouble(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readBoolean() {
    val result = readBoolean(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readString() {
    val result = readString(`in`)
    out.value(result)
  }
  
  @Throws(IOException::class)
  fun readList(ty: KType) {
    out.beginArray()
    val size = `in`.readInt()
    
    for (i in 0..<size) {
      read(ty)
    }
    
    out.endArray()
  }
  
  @Throws(IOException::class)
  fun readLazyList(ty: KType) {
    readList(ty)
  }
  
  @Throws(IOException::class)
  fun readByteArray() {
    var size = `in`.readInt()
    if (dumpBinary) {
      out.beginArray()

      if (size < 0) size = 0
      for (i in 0..<size) {
        readByte()
      }

      out.endArray()
    } else {
      out.value(size)
    }
  }
  
  @Throws(IOException::class)
  fun readIntFloat() {
//    Deserializers.readIntFloat(in);
    throw UnsupportedEncodingException("Unreachable for now")
  }
  
  @Throws(IOException::class)
  fun readInstant() {
    val result = readInstant(`in`)
    out.value(result.toString())
  }
  
  @Throws(IOException::class)
  fun readModStarCache() {
    val result = readModStarCache(`in`)
    
    out.beginObject()
    out.name("mods")
    out.value(toString(result.mods))
    out.name("star")
    out.value(result.difficulty)
    out.endObject()
  }
  
  @Throws(IOException::class)
  fun readEnum(clazz: Class<*>) {
    val result = readEnum(clazz, `in`)
    out.value(result.toString())
  }
  
  @Throws(IOException::class)
  fun tryReadPrimitive(ty: KType): Boolean {
    val clazz = ty.classifier as KClass<*>

    when (clazz) {
      Byte::class -> readByte()
      Short::class -> readShort()
      Int::class -> readInt()
      Long::class -> readLong()
      Float::class -> readFloat()
      Double::class -> readDouble()
      Boolean::class -> readBoolean()
      String::class -> readString()
      ByteArray::class -> readByteArray()
      IntFloatPair::class -> readIntFloat()
      Instant::class -> readInstant()
      ModStarCache::class -> readModStarCache()
      ImmutableSeq::class -> readList(ty.arguments[0].type!!)
      LazySeq::class -> readLazyList(ty.arguments[0].type!!)
      else -> return false
    }
    
    return true
  }
  
  @Throws(IOException::class)
  fun readAny(clazz: KClass<*>) {
    val con = clazz.primaryConstructor
      ?: throw IllegalArgumentException("Must have primary constructor")
    val params = con.parameters

    out.beginObject()

    for (i in params.indices) {
      val p = params[i]
      val pTy = p.type

      try {
        val name = p.name ?: throw IllegalArgumentException("Must have name")
        out.name(name)
        read(pTy)
      } catch (e: OsuParseException) {
        throw e
      }
    }
    
    out.endObject()
  }

  @Throws(IOException::class)
  fun read(ty: Class<*>) {
    read(ty.kotlin.starProjectedType)
  }
  
  @Throws(IOException::class)
  fun read(ty: KType) {
    val clazz = ty.classifier
    if (clazz !is KClass<*>) throw IllegalArgumentException("Must be KClass")

    val jClz: Class<*> = clazz.java
    
    if (tryReadPrimitive(ty)) return

    if (jClz.isEnum) {
      readEnum(jClz)
      return
    }
    
    readAny(clazz)
  }
}
