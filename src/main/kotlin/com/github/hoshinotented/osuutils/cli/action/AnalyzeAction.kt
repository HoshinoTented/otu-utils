package com.github.hoshinotented.osuutils.cli.action

import com.github.hoshinotented.osuutils.ScoreAnalyzer
import com.github.hoshinotented.osuutils.api.Beatmaps
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.data.AnalyzeRecord
import com.github.hoshinotented.osuutils.data.ScoreHistory
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.AnalyzeDatabase
import com.github.hoshinotented.osuutils.database.ScoreHistoryDatabase
import com.github.hoshinotented.osuutils.prettyBeatmap
import com.github.hoshinotented.osuutils.prettyTime
import com.github.hoshinotented.osuutils.providers.BeatmapProvider
import com.github.hoshinotented.osuutils.providers.ScoreHistoryProvider
import com.github.hoshinotented.osuutils.providers.ScoreProvider
import com.github.hoshinotented.osuutils.util.AccumulateProgressIndicator
import com.github.hoshinotented.osuutils.util.ProgressIndicator
import kala.collection.mutable.MutableList
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class AnalyzeAction(
  application: OsuApplication,
  val user: User,
  val analyzeDatabase: AnalyzeDatabase,
  historyDatabase: ScoreHistoryDatabase,
  val beatmapProvider: BeatmapProvider,
  val scoreProvider: ScoreProvider,
  val progressIndicator: ProgressIndicator,
  val options: Options = Options(false),
) {
  data class Options(val showRecentUnplayed: Boolean)
  
  val historyProvider = ScoreHistoryProvider(application, user, historyDatabase)
  
  fun analyze(): String {
    val reportBuffer = StringBuilder()
    val trackingBeatmaps = historyProvider.historyDB.tracking().beatmaps
    val histories = historyProvider.histories()
    val analyzeId = analyzeDatabase.load().lastId + 1
    val pi = AccumulateProgressIndicator(progressIndicator)
    pi.init(histories.size(), "Fetch Scores", null)
    
    val scores = histories.map {
      val scores = scoreProvider.beatmapScores(user, it.beatmapId)
        ?: throw IllegalArgumentException("No such beatmap ${it.beatmapId}, make sure you have correct beatmap id in the tracking list.")
      pi.progress(it.beatmapId.toString())
      scores
    }
    
    val analyzer = ScoreAnalyzer(histories, scores, analyzeId)
    val now = Clock.System.now()
    val reports = analyzer.analyze(now, now - 30.days)
    val unplayed = MutableList.create<ScoreHistory>()
    val analyzeRecord = AnalyzeRecord(analyzeId, now)
    
    // TODO: maybe we can save data after a report generation? cause report generation may fail
    analyzeDatabase.save(analyzeDatabase.load().addRecord(analyzeRecord))
    
    reports.forEachIndexed { idx, report ->
      historyProvider.historyDB.save(report.history)
      
      if (report.playCount == 0) {
        unplayed.append(report.history)
        return@forEachIndexed
      }
      
      val track = trackingBeatmaps.get(idx)
      val oldHistory = histories.get(idx)
      val comment = track.comment
      
      val map = beatmapProvider.beatmap(report.history.beatmapId)!!
      val mapSet = beatmapProvider.beatmapSet(map.beatmapSetId)!!
      
      reportBuffer.appendLine(
        "Report of beatmap: [${prettyBeatmap(mapSet, map)}](${
          Beatmaps.makeBeatmapUrl(
            map.id
          )
        })"
      )
      if (comment != null) {
        reportBuffer.appendLine("> $comment")
        reportBuffer.appendLine()
      }
      
      reportBuffer.appendLine("Recent play count: ${report.playCount}")
      // We can use new history, as the best score may be updated
      // we already rule out the case of playCount == 0
      reportBuffer.appendLine(report.report!!.pretty(oldHistory.best))
    }
    
    val recentPlayedUnplayed = if (options.showRecentUnplayed) {
      unplayed
    } else unplayed.filter { it.scoreSince(now - it.recentWindow).isNotEmpty }
    
    if (recentPlayedUnplayed.isNotEmpty) {
      reportBuffer.appendLine()
      reportBuffer.appendLine("The following beatmaps are not played since last analyzing:")
      recentPlayedUnplayed.forEach {
        val id = it.beatmapId
        val map = beatmapProvider.beatmap(id)!!
        val mapSet = beatmapProvider.beatmapSet(map.beatmapSetId)!!
        
        reportBuffer.appendLine("[${prettyBeatmap(mapSet, map)}](${Beatmaps.makeBeatmapUrl(map.id)})")
      }
    }
    
    return reportBuffer.toString()
  }
  
  // TODO: not good, if a score history is not updated in previous analyze, it will also be modified
  //       I guess we need to store the time when we analyze, or an id
  fun removeLastAnalyze(analyzeId: Int?) {
    val histories = historyProvider.histories()
    val analyzeId = analyzeId ?: analyzeDatabase.load().lastId
    // we don't remove the analyze record in analyzeDatabase
    
    histories.forEach {
      val beatmap = beatmapProvider.beatmap(it.beatmapId)!!
      val set = beatmapProvider.beatmapSet(beatmap.beatmapSetId)!!
      println("Removing the last group of ${prettyBeatmap(set, beatmap)}")
      val ids = it.analyzeIds
      
      if (ids == null) {
        println("Analyze id not found")
        return@forEach
      }
      
      if (ids.isNotEmpty() && ids.last() == analyzeId) {
        val index = it.groups.last()
        val newHistory = it.copy(
          scores = it.scores.slice(0, index),
          groups = it.groups.copyOf(it.groups.size - 1),
          analyzeIds = it.analyzeIds.copyOf(it.analyzeIds.size - 1)
        )
        historyProvider.historyDB.save(newHistory)
        val firstScoreInGroup = it.scores.get(index)
        println("Removed all scores since " + prettyTime(firstScoreInGroup.createdAt))
      } else {
        println("No score is removed, could be either there is no score or the analyze id doesn't match")
      }
    }
  }
}