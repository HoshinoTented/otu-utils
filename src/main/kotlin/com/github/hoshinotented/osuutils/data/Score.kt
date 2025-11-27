@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.SeqView
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * @param scores the scores, from oldest to newest
 * @param groups the index of each group begins, a score group is never empty. This allow the user to re-analyze recent scores
 * @param analyzeIds analyze id that introduce the group, should have the same size as [groups]
 * @param recentBest the best (by acc) score recently (5 days)
 * @param best the best (by acc) score
 * @param recentWindow TODO: move to somewhere else
 */
@Serializable
data class ScoreHistory(
  val beatmapId: BeatmapId,
  val scores: ImmutableSeq<Score>,
  val groups: IntArray,
  val analyzeIds: IntArray?,
  val best: Score?,
  val recentWindow: Duration = 5.days,
) {
  companion object {
    /**
     * Find all scores created since [since].
     * @param scores must be ordered by [Score.CreateTimeComparator]
     */
    fun binaryAnswer(scores: ImmutableSeq<Score>, since: Instant): SeqView<Score> {
      val dummyScore = Score(0.0F, since, 0, ImmutableSeq.empty(), 0, null, null, null)
      val result = scores.binarySearch(dummyScore) { l, r ->
        // [r] is always dummyScore
        val cmp = l.createdAt.compareTo(r.createdAt)
        if (cmp == 0) {
          // in case they are equal, we treat [l] is later than [r] thus we include [l] in the result
          1
        } else cmp
      }
      
      // result is never positive, since our comparator never equal
      assert(result < 0)
      val insertPoint = -result - 1
      return scores.sliceView(insertPoint, 0.inv())
    }
  }
  
  fun addScores(scores: ImmutableSeq<Score>, newBest: Score?, analyzeId: Int): ScoreHistory {
    if (scores.isEmpty) return this
    
    val newGroups = IntArray(groups.size + 1) {
      if (it == groups.size) {
        this.scores.size()
      } else {
        groups[it]
      }
    }
    
    val newBest = newBest ?: this.best
    val newIds = if (this.analyzeIds == null) IntArray(groups.size + 1) {
      if (it == groups.size) analyzeId else 0
    } else {
      this.analyzeIds.copyOf(this.analyzeIds.size + 1).apply {
        this[this.lastIndex] = analyzeId
      }
    }
    
    return copy(groups = newGroups, analyzeIds = newIds, scores = this.scores.appendedAll(scores), best = newBest)
  }
  
  fun scoreSince(since: Instant): SeqView<Score> {
    return binaryAnswer(this.scores, since)
  }
  
  /**
   * Find the best score from [since] to now
   * @return null if no score since [since]
   */
  fun recentBest(since: Instant): Score? {
    val view = binaryAnswer(scores, since)
    return if (view.isNotEmpty) {
      view.reduce { l, r ->
        if (l.accuracy >= r.accuracy) l else r
      }
    } else null
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    
    other as ScoreHistory
    
    if (beatmapId != other.beatmapId) return false
    if (scores != other.scores) return false
    if (!groups.contentEquals(other.groups)) return false
    if (!analyzeIds.contentEquals(other.analyzeIds)) return false
    if (best != other.best) return false
    if (recentWindow != other.recentWindow) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    var result = beatmapId.hashCode()
    result = 31 * result + scores.hashCode()
    result = 31 * result + groups.contentHashCode()
    result = 31 * result + analyzeIds.contentHashCode()
    result = 31 * result + (best?.hashCode() ?: 0)
    result = 31 * result + recentWindow.hashCode()
    return result
  }
}

