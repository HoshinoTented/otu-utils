@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.data.ScoreHistory
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * @param historyDir must exist
 */
class ScoreHistoryDatabase(val historyDir: Path) {
  @Serializable
  data class TrackBeatmap(val id: BeatmapId, val comment: String?)
  
  inner class TrackBeatmaps(
    val beatmaps: ImmutableSeq<TrackBeatmap>,
  ) : AutoCloseable {
    override fun close() {
      trackingMapsPath.writeText(commonSerde.encodeToString(beatmaps))
    }
  }
  
  val trackingMapsPath: Path = historyDir.resolve("tracking_beatmaps.json")
  
  lateinit var trackBeatmapsCache: TrackBeatmaps
  val scoreHistoryCache = MutableMap.create<BeatmapId, ScoreHistory>()
  
  fun tracking(): TrackBeatmaps {
    if (::trackBeatmapsCache.isInitialized) return trackBeatmapsCache
    
    if (!trackingMapsPath.exists()) {
      return TrackBeatmaps(ImmutableSeq.empty())
    }
    
    val json = trackingMapsPath.readText()
    val data = commonSerde.decodeFromString(SeqSerializer(TrackBeatmap.serializer()), json)
    return TrackBeatmaps(data)
  }
  
  fun load(beatmapId: BeatmapId): ScoreHistory {
    val cache = scoreHistoryCache.getOrNull(beatmapId)
    if (cache != null) return cache
    
    val path = historyDir.resolve("$beatmapId.json")
    if (!path.exists()) return ScoreHistory(beatmapId, ImmutableSeq.empty(), intArrayOf(), intArrayOf(), null)
    
    val history = commonSerde.decodeFromString<ScoreHistory>(path.readText())
    scoreHistoryCache.put(beatmapId, history)
    return history
  }
  
  fun save(history: ScoreHistory) {
    scoreHistoryCache.put(history.beatmapId, history)
    historyDir.resolve("${history.beatmapId}.json")
      .writeText(commonSerde.encodeToString(history))
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