// https://github.com/ppy/osu/wiki/Legacy-database-file-structure
// we assume the database comes from newest version, thus version related format problem is gone

package com.github.hoshinotented.osuutils.osudb

import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.api.endpoints.UserId
import com.github.hoshinotented.osuutils.data.IBeatmap
import kala.collection.SeqView
import kala.collection.immutable.ImmutableMap
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import kotlin.time.Instant

annotation class Sized(val bytes: Int)

class OsuParseException(msg: String) : RuntimeException(msg)

data class IntFloatPair(val int: Int, val float: Float)

@Sized(bytes = 10)    // 4 + 4 + 2 where 2 is for two indicators
data class ModStarCache(val mods: MutableEnumSet<Mod>, val difficulty: Float)

@Sized(bytes = 17)
data class TimePoint(val bpm: Double, val offset: Double, val inherit: Boolean)

/**
 * @param T must be [Sized]
 */
class LazySeq<T>(val initializer: Lazy<ImmutableSeq<T>>) : SeqView<T> {
  /**
   * May fail
   */
  val value: ImmutableSeq<T> get() = initializer.value
  
  override fun iterator(): MutableIterator<T> {
    return value.iterator()
  }
}

// Almost everything (stdModdedStarCache and timePoints) with ImmutableSeq is deserialized in a slow speed, consider just read bytes and delay the deserialization?
data class LocalBeatmap(
  val artist: String,
  val artistUnicode: String?,
  val title: String,
  val titleUnicode: String?,
  val creator: String,
  val difficultyName: String,
  val audioFileName: String,
  val md5Hash: String,
  val osuFileName: String,      // .osu file
  val rankedStatus: Byte,
  val circleAmount: Short,
  val sliderAmount: Short,
  val spinnerAmount: Short,
  val lastModified: Instant,
  val ar: Float,
  val cs: Float,
  val hp: Float,
  val od: Float,
  val sliderVelocity: Double,
  val stdModdedStarCache: LazySeq<ModStarCache>,
  val taikoModdedStarCache: LazySeq<ModStarCache>,
  val ctbModdedStarCache: LazySeq<ModStarCache>,
  val maniaModdedStarCache: LazySeq<ModStarCache>,
  val drainTimeSeconds: Int,
  val totalTimeMilliseconds: Int,
  val someMysteryTimeWhichIDontKnowInMilliseconds: Int,
  val timePoints: LazySeq<TimePoint>,
  val beatmapId: Int,
  val beatmapSetId: Int,
  val threadId: Int,
  val stdGrade: Byte,
  val taikoGrade: Byte,
  val ctbGrade: Byte,
  val maniaGrade: Byte,
  val userOffset: Short,
  val stackLeniency: Float,
  val gameplayMode: Byte,
  val songSource: String,
  val songTags: String,
  val onlineOffset: Short,
  val titleFont: String,
  val unplayed: Boolean,
  val lastTimePlayed: Instant,
  val isOsz2: Boolean,
  val folderName: String,
  val lastTimeChecked: Instant,
  val ignoreSound: Boolean,
  val ignoreSkin: Boolean,
  val disableStoryboard: Boolean,
  val disableVideo: Boolean,
  val visualOverride: Boolean,
  // val unknown: Short,
  val lastModifiedTime: Int,
  val maniaScrollSpeed: Byte,
) : IBeatmap {
  companion object {
    enum class RankedStatus {
      Unknown, Unsubmitted, Graveyard, Unused, Ranked, Approved, Qualified, Loved
    }
    
    enum class GameplayMode {
      Std, Taiko, Ctb, Mania
    }
  }
  
  fun starRate(mods: ImmutableSeq<Mod> = ImmutableSeq.empty()): Float {
    val modSet = MutableEnumSet.from(Mod::class.java, mods)
    return stdModdedStarCache.value
      .find { modSet == it.mods }
      .map { it.difficulty }
      .getOrDefault(0.0F)
  }
  
  fun toBeatmap(mods: ImmutableSeq<Mod> = ImmutableSeq.empty()): Beatmap {
    val star = starRate(mods)
    
    return Beatmap(
      beatmapSetId.toLong(), beatmapId.toLong(), star, difficultyName,
      BeatmapSet(beatmapSetId.toLong(), title, titleUnicode ?: title, null),
      null
    )
  }
  
  override fun beatmapId(): BeatmapId = beatmapId.toLong()
  
  override fun title(): String = titleUnicode ?: title
  
  override fun difficultyName(): String = difficultyName
  
  override fun starRate(): Float = starRate(ImmutableSeq.empty())
  
  override fun md5Hash(): String = md5Hash
}

data class LocalOsu(
  val version: Int,
  val folderCount: Int,
  val unlocked: Boolean,
  val unlockedTime: Instant,
  val playerName: String,
  // dont add this field
  //  val beatmapCount: Int,
  val beatmaps: ImmutableSeq<LocalBeatmap>,
  val userPermission: Int,
) {
  companion object {
    enum class Permission {
      Normal, Moderator, Supporter, Friend, Who, Staff
    }
  }

  val beatmapByHash: ImmutableMap<String, LocalBeatmap> by lazy { beatmaps.associateBy { it.md5Hash } }
  val beatmapById: ImmutableMap<BeatmapId, LocalBeatmap> by lazy { beatmaps.associateBy { it.beatmapId.toLong() } }
}

data class LocalScore(
  val gameplayMode: Byte,
  val version: Int,
  val beatmapMd5Hash: String,
  val playerName: String,
  val replayMd5Hash: String,
  // 300
  val greatAmount: Short,
  // 100
  val okAmout: Short,
  // 50
  val mehAmount: Short,
  // blue 激 in std, max 300 in mania
  val gekisAmount: Short,
  // green 喝 in std, 200 in mania
  val katusAmount: Short,
  val missAmount: Short,
  val score: Int,
  val maxCombo: Short,
  val isPFC: Boolean,
  val mods: Int,
  // a string that always empty, this is used in .osr file
  val _unused0: String?,
  val createTime: Instant,   // windows ticks
  // always null, only used in .osr file
  val replay: ByteArray?,
  val scoreId: Long,
//  val someLazerShit: Double,
) {
  val accuracy: Float by lazy {
    // unfortunately we only consider osu!std, thus only 300, 100, 50 and miss involve acc calculation
    val noteCounts = greatAmount.toInt() + okAmout.toInt() + mehAmount.toInt() + missAmount.toInt()
    val b = noteCounts * 6    // 1 for 50, 2 for 100, and 6 for 300
    val a = greatAmount.toInt() * 6 + okAmout.toInt() * 2 + mehAmount.toInt()
    a.toFloat() / b
  }
  
  /**
   * @param beatmapProvider find [Beatmap] by md5 hash
   */
  fun toScore(userId: UserId, beatmapProvider: (String) -> Beatmap?): Score {
    val beatmap = beatmapProvider(beatmapMd5Hash)
    if (beatmap != null && beatmap.checksum != null && beatmap.checksum != beatmapMd5Hash) {
      throw IllegalArgumentException(
        """
        Beatmap checksum mismatch.
        Expected: $beatmapMd5Hash
        Actual: ${beatmap.checksum}
      """.trimIndent()
      )
    }

    return Score(
      accuracy, createTime,
      scoreId, Mod.asSeq(mods), userId,
      beatmap,
      null,
      null
    )
  }
}

data class LocalScoredBeatmap(val md5Hash: String, val scores: ImmutableSeq<LocalScore>)

data class LocalScores(
  val version: Int,
  val scoredBeatmaps: ImmutableSeq<LocalScoredBeatmap>,
)

data class LocalCollection(val name: String, val beatmaps: ImmutableSeq<String>)

data class LocalCollections(
  val version: Int,
  val collections: ImmutableSeq<LocalCollection>
)