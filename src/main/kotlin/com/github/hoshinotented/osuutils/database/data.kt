package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.data.BeatmapCheckSum
import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetListed
import com.github.hoshinotented.osuutils.api.data.Mod
import com.github.hoshinotented.osuutils.api.data.Score
import com.github.hoshinotented.osuutils.api.data.ScoreId
import com.github.hoshinotented.osuutils.api.data.ScoreUserAttribute
import com.github.hoshinotented.osuutils.api.data.UserId
import kala.collection.immutable.ImmutableSeq
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

open class BeatmapInDb(
  var id: BeatmapId = 0,
  var setId: BeatmapSetId = 0,
  var difficulty: String? = null,
  var starRate: Float = 0.0F,
  var checksum: String? = null,
) {
  companion object {
    fun from(map: BeatmapCheckSum): BeatmapInDb = BeatmapInDb(
      map.id,
      map.setId,
      map.difficulty,
      map.starRate,
      map.checksum,
    )
  }

  fun toBeatmap(): BeatmapCheckSum.Impl = BeatmapCheckSum.Impl(
    id, setId, difficulty!!, starRate, checksum!!
  )
}

open class BeatmapSetInDb(
  var id: BeatmapSetId = 0,
  var title: String? = null,
  var titleUnicode: String? = null,
  var beatmaps: List<BeatmapInDb>? = null,
) {
  companion object {
    fun from(set: BeatmapSetListed): BeatmapSetInDb = BeatmapSetInDb(
      set.id, set.title, set.titleUnicode, set.beatmaps.map { BeatmapInDb.from(it) }.asJava()
    )
  }

  fun toBeatmapSet(): BeatmapSetListed.Impl = BeatmapSetListed.Impl(
    id, title!!, titleUnicode ?: title!!, ImmutableSeq.from(beatmaps!!).map { it.toBeatmap() }
  )
}

/**
 * @param createAt primary key, this application is not designed for multi-user (even it is, the clash should be almost impossible)
 * @param mods see [com.github.hoshinotented.osuutils.api.data.Mod.toBitMask]
 * @param websiteScoreId 0 for null
 */
open class ScoreInDb(
  var createAt: Instant? = null,
  var scoreId: ScoreId = 0,
  var beatmapId: BeatmapId = 0,
  var accuracy: Float = 0.0F,
  var mods: Int = 0,
  var userId: UserId = 0,
  var websiteScoreId: Long = 0
) {
  companion object {
    fun from(score: Score, beatmapId: BeatmapId): ScoreInDb = ScoreInDb(
      score.createdAt.toJavaInstant(),
      score.id, beatmapId, score.accuracy, Mod.toBitMask(score.mods), score.userId,
      score.currentUserAttribute?.pin?.scoreId ?: 0
    )
  }

  fun toScore(): Score = Score(
    accuracy, createAt!!.toKotlinInstant(), scoreId, Mod.asSeq(mods), userId,
    websiteScoreId.takeIf { it != 0L }
      ?.let { ScoreUserAttribute(ScoreUserAttribute.Pin(it)) }
  )
}

/**
 * @param activeAnalyzeId last analyze id on this history, 0 for null
 */
open class ScoreHistoryInDb(
  var id: Int = 0,
  var beatmapId: BeatmapId = 0,
  var activeAnalyzeId: Int = 0
)

/**
 * @param analyzeTime when this analyzation occur
 * @param prevAnalyzeId previous analyze id, this forms a linked list
 * @param bestScoreId best score after this analyzing
 */
open class AnalyzeRecordInDb(
  var id: Int = 0,
  var historyId: Int = 0,
  var prevAnalyzeId: Int = 0,
  var analyzeTime: Instant? = null,
  var bestScoreId: ScoreId = 0,
  var valid: Boolean = false,
)