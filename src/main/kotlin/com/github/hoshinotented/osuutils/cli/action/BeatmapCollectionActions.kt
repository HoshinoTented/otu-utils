package com.github.hoshinotented.osuutils.cli.action

import com.github.hoshinotented.osuutils.api.Beatmaps
import com.github.hoshinotented.osuutils.api.HttpException
import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.endpoints.EndpointRequest.Companion.initCommon
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
import com.github.hoshinotented.osuutils.util.AccumulateProgressIndicator
import com.github.hoshinotented.osuutils.util.ProgressIndicator
import kala.collection.immutable.ImmutableSeq
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.jvm.optionals.getOrNull

class BeatmapCollectionActions(
  val collection: BeatmapCollection,
  val progressIndicator: ProgressIndicator,
) {
  companion object {
    const val TITLE_INFO = "Fetch Beatmap Data"
    const val TITLE_EXPORT_FIND = "Find Highest Score"
    const val TITLE_EXPORT_EXPORT = "Export Highest Score"
    const val TITLE_DOWNLOAD = "Download Beatmap"
    
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
  fun info(beatmapProvider: BeatmapProvider): Boolean {
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
    
    if (collection.beatmaps.size() != 0) {
      progressIndicator.progress(1, collection.beatmaps.size(), TITLE_EXPORT_FIND, null)
    }
    
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
  
  /**
   * Download all beatmap in collection to [outDir]
   * @param outDir target directory, can be absent or dirty
   */
  fun download(outDir: Path, beatmapProvider: BeatmapProvider) {
    if (!outDir.exists()) {
      outDir.createDirectories()
    } else if (!outDir.isDirectory()) {
      throw IllegalArgumentException("Cannot download to a file: $outDir")
    }
    
    val pi = AccumulateProgressIndicator(progressIndicator)
    pi.init(collection.beatmaps.size(), TITLE_DOWNLOAD, null)
    
    collection.beatmaps.forEach {
      
      val map = beatmapProvider.beatmap(it.id)
      if (map == null) {
        pi.progress("Beatmap ${it.id} is not found, skip.")
      } else {
        val set = map.beatmapSet!!
        pi.progress(prettyBeatmap(set, map))
        // TODO: maybe skip when outDir contains such file
        // sayobot doesn't provide valid "location" which whitespace is not url encoded
        val resp = OsuApi.client.send(
          HttpRequest.newBuilder(URI.create(Beatmaps.makeBeatmapDownloadUrlSayobot(set.id)))
            .GET()
            .build(), HttpResponse.BodyHandlers.ofByteArray()
        )
        
        if (resp.statusCode() == 302) {
          val location = resp.headers().firstValue("Location").getOrNull()
            ?: throw RuntimeException("Expecting location header field")
          
          // trash code, how to eliminate??
          val target = location.replace(" ", "%20")
          OsuApi.client.send(
            HttpRequest.newBuilder(URI.create(target))
              .GET()
              .build(),
            HttpResponse.BodyHandlers.ofFileDownload(
              outDir,
              StandardOpenOption.WRITE,
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING,
            )
          )
        } else {
          throw RuntimeException("Unexpected ${resp.statusCode()}, this could be sayobot changes their download strategy, skip.")
        }
      }
    }
  }
}