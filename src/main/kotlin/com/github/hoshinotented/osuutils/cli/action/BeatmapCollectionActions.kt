package com.github.hoshinotented.osuutils.cli.action

import com.github.hoshinotented.osuutils.api.Beatmaps
import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.data.BeatmapCollection
import com.github.hoshinotented.osuutils.data.BeatmapInCollection
import com.github.hoshinotented.osuutils.data.BeatmapInfoCache
import com.github.hoshinotented.osuutils.data.IBeatmap
import com.github.hoshinotented.osuutils.osudb.*
import com.github.hoshinotented.osuutils.prettyBeatmap
import com.github.hoshinotented.osuutils.prettyMods
import com.github.hoshinotented.osuutils.providers.BeatmapProvider
import com.github.hoshinotented.osuutils.util.AccumulateProgressIndicator
import com.github.hoshinotented.osuutils.util.MCExpr
import com.github.hoshinotented.osuutils.util.MCExpr.Companion.test
import com.github.hoshinotented.osuutils.util.ModRestriction
import com.github.hoshinotented.osuutils.util.ProgressIndicator
import kala.collection.immutable.ImmutableSeq
import java.io.IOException
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrNull

/**
 * @param collectionFilePath the path used for writing new [BeatmapCollection], this is not necessary the path
 *                           where [collection] is deserialized.
 */
class BeatmapCollectionActions(
  val collectionFilePath: Path?,
  val force: Boolean,
  val fillMods: Boolean,
  val collection: BeatmapCollection,
  val progressIndicator: ProgressIndicator,
) {
  companion object {
    const val TITLE_INFO = "Fetch Beatmap Data"
    const val TITLE_EXPORT_FIND = "Find Highest Score"
    const val TITLE_EXPORT_EXPORT = "Export Highest Score"
    const val TITLE_DOWNLOAD = "Download Beatmap"
    
    val TAG_PATTERN = Regex("([a-zA-Z]+)(\\d+)")
    
    fun replayFileName(beatmap: IBeatmap, score: LocalScore): String {
      val components = ImmutableSeq.of(
        score.playerName,
        score.score.toString(),
        Score.rawAcc(score.accuracy),
        prettyMods(Mod.asSeq(score.mods), prefix = ""),
        beatmap.beatmapId().toString(),
        // i don't want to \escape those stupid character
//        beatmap.title(),
//        beatmap.difficultyName()
      )
      
      return components.joinToString("-")
    }
    
    fun tagToMods(tag: String): String? {
      return when (tag.uppercase(Locale.ENGLISH)) {
        "NM" -> "NM"
        "HD" -> "HD"
        "HR" -> "HR"
        "DT" -> "DT|NC"
        "FM" -> "{ HD, HR }"
        "TB" -> "{ HD, HR }"
        else -> null
      }
    }
  }
  
  /**
   * Fill [com.github.hoshinotented.osuutils.data.BeatmapInCollection.cache],
   * note that it is possible that some [BeatmapInCollection.cache] is still null,
   * which means the beatmap id is invalid
   *
   * TODO: we can make a BeatmapProvider by ImmutableSeq<BeatmapInCollection>
   */
  fun prepare(beatmapProvider: BeatmapProvider): ImmutableSeq<BeatmapInCollection> {
    val pi = AccumulateProgressIndicator(progressIndicator)
    pi.init(collection.beatmaps.size(), TITLE_INFO, null)
    
    var changed = false
    val result = collection.beatmaps.mapIndexed { idx, it ->
      var beatmap = it
      val tag = beatmap.tag
      if (fillMods && tag != null && it.mods == null) {
        val matchResult = TAG_PATTERN.matchEntire(tag)
        if (matchResult != null) {
          val modTag = matchResult.groups[1]!!.value
          val modRestriction = tagToMods(modTag)
          if (modRestriction != null) {
            changed = true
            beatmap = beatmap.copy(mods = ModRestriction(modRestriction))
          }
        }
      }
      
      if (!force && beatmap.cache != null) {
        pi.progress("Cache hit for $idx, skip")
        beatmap
      } else {
        val cache = beatmapProvider.beatmap(beatmap.id)?.let { beatmap ->
          val set = beatmap.beatmapSet!!
          BeatmapInfoCache(
            beatmap.id,
            beatmap.beatmapSetId,
            set.title,
            set.titleUnicode,
            beatmap.version,
            beatmap.difficulty,
            beatmap.checksum!!
          )
        }
        
        if (cache == null) {
          System.err.println("Cannot find beatmap $idx")
          pi.progress("Cannot find beatmap $idx")
          beatmap
        } else {
          pi.progress(prettyBeatmap(cache.titleUnicode ?: cache.title, cache.difficultyName, cache.starRate))
          changed = true
          beatmap.copy(cache = cache)
        }
      }
    }
    
    // shabi intellij
    return if (changed) result else collection.beatmaps
  }
  
  /**
   * This method will write a new BeatmapCollection
   */
  fun prepareDirty(beatmapProvider: BeatmapProvider): BeatmapCollection {
    val result = prepare(beatmapProvider)
    if (result === collection.beatmaps) return collection
    
    // some cache is filled
    val collection = collection.copy(beatmaps = result)
    collectionFilePath?.writeText(commonSerde.encodeToString(collection))
    return collection
  }
  
  /**
   * @return if success
   */
  fun info(beatmapProvider: BeatmapProvider): Boolean {
    println("Collection name: ${collection.name}")
    println("Author: ${collection.author}")
    
    val collection = prepareDirty(beatmapProvider)
    var success = true
    
    collection.beatmaps.forEach {
      val cache = it.cache
      if (cache == null) {
        // already report in [prepare]
        success = false
      } else {
        println(
          "[${
            prettyBeatmap(
              cache.titleUnicode ?: cache.title,
              cache.difficultyName,
              cache.starRate
            )
          }](${Beatmaps.makeBeatmapUrl(it.id)})"
        )
      }
    }
    
    return success
  }
  
  /**
   * @param filter scores with at least those [Mod]s on will be counted.
   * @return if success
   */
  fun scores(
    localOsuPath: Path,
    beatmapProvider: BeatmapProvider,
    localScores: LocalScores,
    outDir: Path?,
    filter: ImmutableSeq<Mod>, // we DO can use EnumSet, but i just don't
  ): Boolean {
    if (outDir != null && outDir.exists() && Files.newDirectoryStream(outDir).use { it.iterator().hasNext() }) {
      // outDir is dirty
      throw IOException("Output directory $outDir exists and is not empty, abort.")
    }
    
    val collection = prepareDirty(beatmapProvider)
    val byHash = localScores.scoredBeatmaps.associateBy { it.md5Hash }
    val filterMask = Mod.toBitMask(filter)
    
    var success = true
    
    val pi = AccumulateProgressIndicator(progressIndicator)
    pi.init(collection.beatmaps.size(), TITLE_EXPORT_FIND, null)
    
    val highestScores = collection.beatmaps.map {
      val cache = it.cache
      if (cache == null) {
        pi.progress("Skip")
        success = false
        null
      } else {
        val scores = byHash.getOrNull(cache.md5Hash)
        if (scores == null) {
          System.err.println("Beatmap with id ${it.id} has no score.")
          pi.progress("Skip")
          success = false
          null
        } else {
          val modRestriction = it.mods
          val highest = scores.scores
            .filter { s ->
              (s.mods and filterMask) == filterMask
                      && (modRestriction == null || modRestriction.compiled.test(Mod.asSeq(s.mods)) == MCExpr.TestResult.Success)
            }
            .takeIf { it.isNotEmpty }
            ?.maxBy { s -> s.score }
          
          if (highest == null) {
            System.err.println("Beatmap with id ${it.id} has no score with mod restriction: V2${it.mods?.code ?: ""}")
            pi.progress("Skip")
            success = false
            null
          } else {
            val subtitle = Score.prettyScore(highest.score, highest.accuracy, Mod.asSeq(highest.mods), cache)
            
            pi.progress(subtitle)
            highest
          }
        }
      }
    }
    
    if (outDir != null) {
      outDir.createDirectories()
      
      pi.init(collection.beatmaps.size(), TITLE_EXPORT_EXPORT, null)
      
      highestScores.forEachIndexed { idx, it ->
        if (it != null) {
          val beatmapInCollection = collection.beatmaps[idx]
          val beatmap = beatmapInCollection.cache!!   // never null, it != null implies cache != null
          
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
            pi.progress("Skip")
            success = false
          } else {
            val target = replayFileName(beatmap, it) + ".osr"
            found.copyTo(outDir.resolve(target))
            pi.progress(target)
          }
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
   * Download all beatmap in collection to [outDir]wata
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
          throw RuntimeException("Unexpected ${resp.statusCode()}, this could be sayobot changes their download strategy.")
        }
      }
    }
  }
}