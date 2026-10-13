package com.github.hoshinotented.osuutils.dump.deser

import com.github.hoshinotented.osuutils.dump.OsuParseException
import com.google.common.io.LittleEndianDataInputStream
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import java.nio.charset.Charset
import kotlin.time.Instant

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

fun LittleEndianDataInputStream.readString(): String {
  val indicator = this.read()
  if (indicator == 0x00) return ""
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

private const val UNIX_EPOCH_MILLISECONDS = 62135596800000L

fun fromWindowsTicks(ticks: Long): Instant {
  // 10000 windows ticks = 1 milliseconds
  // 0 windows ticks is 0001-01-01T00:00:00.000Z, so we subtract [UNIX_EPOCH_MILLISECONDS]
  return Instant.fromEpochMilliseconds(ticks / 10000L - UNIX_EPOCH_MILLISECONDS)
}