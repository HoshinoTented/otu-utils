package com.github.hoshinotented.osuutils

import com.github.hoshinotented.osuutils.api.Beatmaps
import com.github.hoshinotented.osuutils.api.Beatmaps.beatmapScores
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.data.ScoreHistory
import com.github.hoshinotented.osuutils.data.User
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ScoreAnalyzer(
  val application: OsuApplication,
  val user: User,
  val histories: ImmutableSeq<ScoreHistory>,
  val analyzeId: Int,
) {
  /**
   * @param history new history with new scores added, if [playCount] == 0, then this is the original history
   * @param report null if [playCount] is `0`
   */
  data class AnalyzeReport(
    val history: ScoreHistory,
    val playCount: Int,
    val report: Report?,
  )
  
  /**
   * It is possible that [bestScore] == [worstScore] when `playCount == 1`, also [bestScore] may also be [ScoreHistory.best].
   *
   * @param bestScore the best score ever played
   * @param recentBest the best score recently played, nullable
   * @param recentBestCompare compare [bestScore] to recent best, this field is `0.0F` when [recentBest] is `null`
   * @param historyBestCompare compare [bestScore] to history best, this field is `0.0F` when [ScoreHistory.best] is `null`
   */
  data class Report(
    val bestScore: Score,
    val worstScore: Score,
    val recentBest: Score?,
    val recentBestCompare: Float,
    val historyBestCompare: Float,
  ) {
    companion object {
      private fun StringBuilder.simplePrettyScore(score: Score, diff: Float? = null) {
        val acc = Score.prettyAcc(score.accuracy)
        append(acc)
        if (diff != null) {
          append("(")
          append(Score.prettyDiff(diff))
          append(")")
        }
        
        if (score.mods.isNotEmpty) {
          append(" ")
          append(prettyMods(MutableEnumSet.from(Mod::class.java, score.mods)))
        }
        
        append(" created at ")
        append(prettyTime(score.createdAt))
      }
      
      private fun StringBuilder.makeScoreLink(pre: String, score: Score?, post: String) {
        append(pre)
        val scoreId = score?.currentUserAttribute?.pin?.scoreId
        if (scoreId == null) {
          append("score")
        } else {
          append("[score](")
          append(Beatmaps.makeScoreUrl(scoreId))
          append(")")
        }
        append(post)
      }
      
    }
    
    fun pretty(historyBest: Score?): String = buildString {
      makeScoreLink("Best ", bestScore, ": ")
      simplePrettyScore(bestScore)
      appendLine()
      
      // even though this is equivalent to reference comparing, but we still compare id in case some bad thing happens
      if (bestScore.id != worstScore.id) {
        makeScoreLink("Worst ", worstScore, ": ")
        simplePrettyScore(worstScore)
        appendLine()
      }
      
      makeScoreLink("Compare to recent best ", recentBest, ": ")
      if (recentBest != null) {
        simplePrettyScore(recentBest, recentBestCompare)
      } else {
        append("not recently played")
      }
      appendLine()
      
      val isRecentHistoryBest = historyBest != null && historyBest.id == bestScore.id
      
      makeScoreLink("Compare to history best ", historyBest?.takeIf { !isRecentHistoryBest }, ": ")
      if (historyBest != null && !isRecentHistoryBest) {
        simplePrettyScore(historyBest, historyBestCompare)
      } else {
        append("not played before")
      }
      appendLine()
    }
  }
  
  /**
   * @param fallbackSince used when no score in history, this is very useful when initializing a new history, such as don't import old scores
   */
  fun analyze(now: Instant, fallbackSince: Instant? = null): ImmutableSeq<AnalyzeReport> {
    // fetch all score before analyze, as fetching can fail, then we won't get inconsistent data
    val scoreses = histories.map {
      application.beatmapScores(user, it.beatmapId)
    }
    
    return histories.zip(scoreses) { history, scores ->
      analyze(now, history, scores, fallbackSince = fallbackSince)
    }
  }
  
  /**
   * Produce an [AnalyzeReport] from given [scores] and [history]. This function is pure.
   */
  fun analyze(
    now: Instant,
    history: ScoreHistory,
    scores: ImmutableSeq<Score>,
    fallbackSince: Instant? = null,
  ): AnalyzeReport {
    val since = history.scores.lastOrNull
      ?.createdAt?.plus(1.seconds) // hope there is no song with 1 second long
      ?: fallbackSince
    
    val scoreSince = if (since == null) scores
    else ScoreHistory.binaryAnswer(scores, since).toSeq()
    
    val playCount = scoreSince.size()
    if (playCount == 0) return AnalyzeReport(history, 0, null)
    
    // I know we can do it in O(2N), but i am too lazy
    val sortedScores = scoreSince.sorted(Score.AccComparator)
    val worstScore = sortedScores.first
    val bestScore = sortedScores.last
    
    val recentBest = history.recentBest(now.minus(history.recentWindow))
    
    val recentBestCompare = recentBest?.let { compareScore(bestScore, it) } ?: 0.0F
    val historyBestCompare = history.best?.let { compareScore(bestScore, it) } ?: 0.0F
    
    val newBest = if (history.best == null || historyBestCompare >= 0) {
      bestScore
    } else {
      history.best
    }
    
    return AnalyzeReport(
      history.addScores(scoreSince, newBest, analyzeId), playCount, Report(
        bestScore, worstScore, recentBest, recentBestCompare, historyBestCompare
      )
    )
  }
  
  fun compareScore(l: Score, r: Score): Float {
    return l.accuracy - r.accuracy
  }
}

fun OsuApplication.initializeScoreHistory(user: User, beatmapId: BeatmapId): ScoreHistory {
  return with(Beatmaps) {
    val score = bestScore(user, beatmapId)
    
    // scores are always empty, even [score] is available, cause [scores] is considered the list of analyzed scores,
    // but [score] can be very new
    ScoreHistory(beatmapId, ImmutableSeq.empty(), intArrayOf(), intArrayOf(), score)
  }
}