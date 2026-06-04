package com.github.hoshinotented.osuutils.dump.deser

import com.github.hoshinotented.osuutils.dump.LocalBeatmap
import com.github.hoshinotented.osuutils.dump.LocalOsu
import com.github.hoshinotented.osuutils.dump.OsuParseException
import com.google.common.io.LittleEndianDataInputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.reflect.full.primaryConstructor

interface LocalOsuParseListener {
  /**
   * If you want to interrupt the procedure, just throw an exception
   *
   * @param index the index of beatmap, or how many beatmap have been parsed
   */
  fun beforeParseBeatmap(index: Int, max: Int)
  fun afterParseBeatmap(index: Int, max: Int, beatmap: LocalBeatmap)
}

object BeatmapCorruptedHandler : CorruptedHandler<LocalBeatmap?> {
  val titleOffset = LocalBeatmap::class.primaryConstructor!!.parameters.indexOfFirst { it.name == "title" }

  override fun onCorrupted(data: Array<Any?>): LocalBeatmap? {
    val title = data[titleOffset].toString()    // possible null
    System.err.println("Beatmap \"$title\" is corrupted, skip.")
    return null
  }
}

fun parseLocalOsu(bytes: LittleEndianDataInputStream, listener: LocalOsuParseListener): LocalOsu {
  val version = bytes.readInt()
  val folderCount = bytes.readInt()
  val unlocked = bytes.readBoolean()
  val unlockedTime = bytes.readDateTime()
  val playerName = bytes.readString()!!
  val beatmaps = bytes.readMany { idx, max ->
    listener.beforeParseBeatmap(idx, max)
    val beatmap = parse(
      LocalBeatmap::class,
      this,
      BeatmapCorruptedHandler
    )!!
    // TODO: maybe move to finally block
    listener.afterParseBeatmap(idx, max, beatmap)
    beatmap
  }.filterNotNull().map { it !! }

  val permission = bytes.readInt()

  return LocalOsu(
    version,
    folderCount,
    unlocked,
    unlockedTime,
    playerName,
    beatmaps,
    permission
  )
}