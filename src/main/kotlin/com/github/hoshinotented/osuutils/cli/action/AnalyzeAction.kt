package com.github.hoshinotented.osuutils.cli.action

import com.github.hoshinotented.osuutils.ScoreAnalyzer
import com.github.hoshinotented.osuutils.api.Beatmaps
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.data.ScoreHistory
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.BeatmapDatabase
import com.github.hoshinotented.osuutils.database.ScoreHistoryDatabase
import com.github.hoshinotented.osuutils.prettyBeatmap
import com.github.hoshinotented.osuutils.prettyTime
import com.github.hoshinotented.osuutils.providers.BeatmapProvider
import kala.collection.mutable.MutableList
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class AnalyzeAction(
  val application: OsuApplication,
  val user: User,
  val historyDatabase: ScoreHistoryDatabase,
  beatmapDatabase: BeatmapDatabase,
  val options: Options = Options(false),
) {
  data class Options(val showRecentUnplayed: Boolean)
  
  val beatmapProvider = BeatmapProvider(application, user, beatmapDatabase)
  
  fun analyze(): String {
    val reportBuffer = StringBuilder()
    val trackingBeatmaps = historyDatabase.tracking().beatmaps
    val histories = historyDatabase.loadAll()
    
    val analyzer = ScoreAnalyzer(application, user, histories)
    val now = Clock.System.now()
    val reports = analyzer.analyze(now, now - 30.days)
    val unplayed = MutableList.create<ScoreHistory>()
    
    reports.forEachIndexed { idx, report ->
      historyDatabase.save(report.history)
      
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
  
  fun removeLastAnalyze() {
    val histories = historyDatabase.loadAll()
    histories.forEach {
      val beatmap = beatmapProvider.beatmap(it.beatmapId)!!
      val set = beatmapProvider.beatmapSet(beatmap.beatmapSetId)!!
      println("Removing the last group of ${prettyBeatmap(set, beatmap)}")
      if (it.groups.isNotEmpty()) {
        val index = it.groups.last()
        val newHistory = it.copy(scores = it.scores.slice(0, index), groups = it.groups.copyOf(it.groups.size - 1))
        historyDatabase.save(newHistory)
        val firstScoreInGroup = it.scores.get(index)
        println("Removed all scores since " + prettyTime(firstScoreInGroup.createdAt))
      }
    }
  }
}