package com.github.hoshinotented.osuutils.cli.action

import com.github.hoshinotented.osuutils.api.Beatmaps
import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.data.BeatmapCollection
import com.github.hoshinotented.osuutils.osudb.LocalBeatmap
import com.github.hoshinotented.osuutils.osudb.LocalOsu
import com.github.hoshinotented.osuutils.osudb.LocalScore
import com.github.hoshinotented.osuutils.osudb.LocalScores
import com.github.hoshinotented.osuutils.osudb.findReplay
import com.github.hoshinotented.osuutils.prettyBeatmap
import com.github.hoshinotented.osuutils.prettyMods
import com.github.hoshinotented.osuutils.providers.BeatmapProvider
import com.github.hoshinotented.osuutils.util.ProgressIndicator
import kala.collection.immutable.ImmutableSeq
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class BeatmapCollectionActions(
  val collection: BeatmapCollection,
  val beatmapProvider: BeatmapProvider,
  val progressIndicator: ProgressIndicator,
) {
  companion object {
    const val TITLE_INFO = "Fetch Beatmap Data"
    const val TITLE_EXPORT_FIND = "Find Highest Score"
    const val TITLE_EXPORT_EXPORT = "Export Highest Score"
    
    fun replayFileName(beatmap: LocalBeatmap, score: LocalScore): String {
      val components = ImmutableSeq.of(
        score.playerName,
        score.score.toString(),
        Score.rawAcc(score.accuracy),
        prettyMods(Mod.asSeq(score.mods), prefix = ""),
        beatmap.beatmapId.toString(),
        beatmap.title,
        beatmap.difficultyName
      )
      
      return components.joinToString("-")
    }
  }
  
  /**
   * @return if success
   */
  fun info(): Boolean {
    println("Collection name: ${collection.name}")
    println("Author: ${collection.author}")
    
    // let user know what we doing
    progressIndicator.progress(1, collection.beatmaps.size(), TITLE_INFO, null)
    
    val beatmap = collection.beatmaps.mapIndexed { idx, it ->
      val beatmap = beatmapProvider.beatmap(it.id)
      val beatmapDisplay = if (beatmap == null) "FAILED" else {
        // beatmap set never null since the property of [BeatmapProvider.beatmap]
        prettyBeatmap(beatmap.beatmapSet!!, beatmap)
      }
      
      progressIndicator.progress(idx, collection.beatmaps.size(), TITLE_INFO, beatmapDisplay)
      beatmap
    }
    
    var success = true
    
    beatmap.forEachIndexed { idx, it ->
      if (it == null) {
        System.err.println("Invalid beatmap id: ${collection.beatmaps[idx]}")
        success = false
      } else {
        println("[${prettyBeatmap(it.beatmapSet!!, it)}](${Beatmaps.makeBeatmapUrl(it.id)})")
      }
    }
    
    return success
  }
  
  /**
   * @param filter scores with at least those [Mod]s on will be counted
   * @return if success
   */
  fun export(
    localOsuPath: Path,
    localOsu: LocalOsu,
    localScores: LocalScores,
    outDir: Path,
    filter: ImmutableSeq<Mod>, // we DO can use EnumSet, but i just don't
  ): Boolean {
    if (outDir.exists() && Files.newDirectoryStream(outDir).use { it.iterator().hasNext() }) {
      // outDir is dirty
      throw IOException("Output directory $outDir exists and is not empty, abort.")
    }
    
    val byId = localOsu.beatmaps.associateBy { it.beatmapId }
    val byHash = localScores.scoredBeatmaps.associateBy { it.md5Hash }
    val filterMask = Mod.toBitMask(filter)
    
    var success = true
    
    progressIndicator.progress(1, collection.beatmaps.size(), TITLE_EXPORT_FIND, null)
    
    val highestScores = collection.beatmaps.mapIndexed { idx, it ->
      val localMap = byId.getOrNull(it.id.toInt())
      if (localMap == null) {
        System.err.println("Beatmap with id $it is not found in local osu! database, skipped.")
        progressIndicator.progress(idx + 1, collection.beatmaps.size(), TITLE_EXPORT_FIND, "Skip")
        success = false
        null
      } else {
        val scores = byHash.getOrNull(localMap.md5Hash)
        if (scores == null) {
          System.err.println("Beatmap with id $it has no score.")
          progressIndicator.progress(idx + 1, collection.beatmaps.size(), TITLE_EXPORT_FIND, "Skip")
          success = false
          null
        } else {
          val highest = scores.scores
            .filter { (it.mods and filterMask) == filterMask }
            .maxBy { it.score }
          
          val subtitle = Score.prettyScore(highest.score, highest.accuracy, Mod.asSeq(highest.mods), localMap)
          
          progressIndicator.progress(idx + 1, collection.beatmaps.size(), TITLE_EXPORT_FIND, subtitle)
          highest
        }
      }
    }
    
    outDir.createDirectories()
    
    progressIndicator.progress(1, collection.beatmaps.size(), TITLE_EXPORT_EXPORT, null)
    
    highestScores.forEachIndexed { idx, it ->
      if (it != null) {
        val beatmapInCollection = collection.beatmaps[idx]
        val beatmap = byId[beatmapInCollection.id.toInt()]   // never fail, see how a not null highestScore is produced
        
        val found = findReplay(localOsuPath, it.beatmapMd5Hash, it.replayMd5Hash)
        if (found == null) {
          System.err.println(
            "Cannot find replay for score: ${
              Score.prettyScore(
                it.score,
                it.accuracy,
                Mod.asSeq(it.mods),
                beatmap
              )
            }"
          )
          progressIndicator.progress(idx + 1, collection.beatmaps.size(), TITLE_EXPORT_EXPORT, "Skip")
          success = false
        } else {
          val target = replayFileName(beatmap, it) + ".osr"
          found.copyTo(outDir.resolve(target))
          progressIndicator.progress(idx + 1, collection.beatmaps.size(), TITLE_EXPORT_EXPORT, target)
        }
      }
    }
    
    println("Your scores:")
    var totalScore: Int = 0
    
    highestScores.forEachWith(collection.beatmaps) { score, beatmap ->
      val tag = beatmap.tag
      val prefix = if (tag == null) {
        beatmap.id.toString()
      } else {
        "$tag(${beatmap.id})"
      }
      
      val score = if (score == null) "No score" else {
        totalScore += score.score
        "${score.score} with accuracy ${Score.prettyAcc(score.accuracy)}"
      }
      
      println("$prefix: $score")
    }
    
    println("Total score: $totalScore")
    
    return success
  }
}