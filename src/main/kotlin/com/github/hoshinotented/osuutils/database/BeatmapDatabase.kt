@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSetId
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.io.FileIO
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

// TODO: when do we use a real database?
/**
 * @param databaseDir must exist
 */
class BeatmapDatabase(val databaseDir: Path, val io: FileIO) {
  val metadataPath: Path = databaseDir.resolve("metadata.json")
  
  @Serializable
  data class ThinBeatmapSet(
    val id: BeatmapSetId, val title: String,
    @SerialName("title_unicode") val titleUnicode: String,
    val beatmaps: ImmutableSeq<BeatmapId>,
  )
  
  inner class Metadata(val map: MutableMap<BeatmapId, BeatmapSetId>) : AutoCloseable {
    override fun close() {
      io.writeText(metadataPath, commonSerde.encodeToString(map))
    }
  }
  
  private lateinit var metadataCache: Metadata
  
  /**
   * If you don't modify the metadata, then `.use` is not necessary
   */
  private fun metadata(): Metadata {
    if (::metadataCache.isInitialized) return metadataCache
    
    if (metadataPath.exists()) {
      val map = commonSerde.decodeFromString<MutableMap<BeatmapId, Long>>(metadataPath.readText())
      return Metadata(map)
    } else {
      return Metadata(mutableMapOf())
    }
  }
  
  fun listSets(): ImmutableSeq<BeatmapSetId> {
    val entries = databaseDir.listDirectoryEntries(glob = "*.json")
    val sets = entries.filter { it.fileName.name != "metadata.json" }
    return ImmutableSeq.from(sets).map {
      commonSerde.decodeFromString<ThinBeatmapSet>(it.readText()).id
    }
  }
  
  fun loadMaybe(beatmapId: BeatmapId): Beatmap? {
    val setId = metadata().use {
      if (it.map.containsKey(beatmapId)) {
        it.map.getValue(beatmapId)
      } else return null
    }
    
    val path = databaseDir.resolve("$setId").resolve("$beatmapId.json")
    return if (path.exists()) {
      // TODO: maybe cache?
      commonSerde.decodeFromString(path.readText())
    } else null
  }
  
  fun load(beatmapId: BeatmapId): Beatmap {
    return loadMaybe(beatmapId)
      ?: throw IllegalStateException("Unable to load beatmap $beatmapId from database, the database may be corrupted.")
  }
  
  fun save(map: Beatmap) {
    val setPath = databaseDir.resolve("${map.beatmapSetId}")
    if (!setPath.exists()) {
      if (!setPath.toFile().mkdirs()) throw IOException("Unable to create directories: $setPath")
    }
    
    setPath.resolve("${map.id}.json")
      .writeText(commonSerde.encodeToString(map))
  }
  
  fun loadSetMaybe(beatmapSetId: BeatmapSetId): BeatmapSet? {
    val path = databaseDir.resolve("$beatmapSetId.json")
    if (!path.exists()) return null
    
    val thin = commonSerde.decodeFromString<ThinBeatmapSet>(path.readText())
    val beatmaps = thin.beatmaps.map { load(it) }
    
    return BeatmapSet(thin.id, thin.title, thin.titleUnicode, beatmaps)
  }
  
  fun loadSet(beatmapSetId: BeatmapSetId): BeatmapSet {
    return loadSetMaybe(beatmapSetId)
      ?: throw IllegalStateException("Unable to load beatmap set $beatmapSetId from database, the database may be corrupted.")
  }
  
  fun saveSet(set: BeatmapSet) {
    val beatmaps = set.beatmaps ?: throw IllegalArgumentException("Cannot save beatmap set with no beatmaps")
    val thin = ThinBeatmapSet(set.id, set.title, set.titleUnicode, set.beatmaps.map { it.id })
    
    databaseDir.resolve("${set.id}.json").writeText(commonSerde.encodeToString(thin))
    
    // save beatmaps
    beatmaps.forEach { save(it) }
    
    // update metadata
    metadata().use { metadata ->
      beatmaps.forEach { map ->
        metadata.map[map.id] = set.id
      }
    }
  }
}