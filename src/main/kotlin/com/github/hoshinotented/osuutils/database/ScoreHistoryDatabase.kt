@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.data.ScoreHistory
import com.github.hoshinotented.osuutils.io.FileIO
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.nio.file.Path

/**
 * @param historyDir must exist
 */
class ScoreHistoryDatabase(val historyDir: Path, val io: FileIO) {
  @Serializable
  data class TrackBeatmap(val id: BeatmapId, val comment: String?)
  
  inner class TrackBeatmaps(
    val beatmaps: ImmutableSeq<TrackBeatmap>,
  ) : AutoCloseable {
    override fun close() {
      io.writeText(trackingMapsPath, commonSerde.encodeToString(beatmaps))
    }
  }
  
  val trackingMapsPath: Path = historyDir.resolve("tracking_beatmaps.json")
  
  lateinit var trackBeatmapsCache: TrackBeatmaps
  val scoreHistoryCache = MutableMap.create<BeatmapId, ScoreHistory>()
  
  fun tracking(): TrackBeatmaps {
    if (::trackBeatmapsCache.isInitialized) return trackBeatmapsCache
    
    if (!io.exists(trackingMapsPath)) {
      return TrackBeatmaps(ImmutableSeq.empty())
    }
    
    val json = io.readText(trackingMapsPath)
    val data = commonSerde.decodeFromString(SeqSerializer(TrackBeatmap.serializer()), json)
    return TrackBeatmaps(data)
  }
  
  fun load(beatmapId: BeatmapId): ScoreHistory {
    val cache = scoreHistoryCache.getOrNull(beatmapId)
    if (cache != null) return cache
    
    val path = historyDir.resolve("$beatmapId.json")
    if (!io.exists(path)) return ScoreHistory(beatmapId, ImmutableSeq.empty(), intArrayOf(), intArrayOf(), null)
    
    val history = commonSerde.decodeFromString<ScoreHistory>(io.readText(path))
    scoreHistoryCache.put(beatmapId, history)
    return history
  }
  
  fun save(history: ScoreHistory) {
    scoreHistoryCache.put(history.beatmapId, history)
    io.writeText(historyDir.resolve("${history.beatmapId}.json"), commonSerde.encodeToString(history))
  }
  
  /**
   * In the same order and size as [tracking]
   */
  fun loadAll(): ImmutableSeq<ScoreHistory> {
    return tracking().beatmaps.map {
      load(it.id)
    }
  }
}