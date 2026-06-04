package com.github.hoshinotented.osuutils.dump

import com.github.hoshinotented.osuutils.data.BeatmapId
import com.github.hoshinotented.osuutils.data.IBeatmap
import com.github.hoshinotented.osuutils.data.Mod
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import kotlin.time.Instant

// Almost everything (stdModdedStarCache and timePoints) with ImmutableSeq is deserialized in a slow speed, consider just read bytes and delay the deserialization?
data class LocalBeatmap(
  val artist: String,
  val artistUnicode: String?,
  val title: String,
  val titleUnicode: String?,
  val creator: String,
  val difficultyName: String,
  val audioFileName: String?,
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

  override fun beatmapId(): BeatmapId = beatmapId.toLong()

  override fun title(): String = titleUnicode ?: title

  override fun difficultyName(): String = difficultyName

  override fun starRate(): Float = starRate(ImmutableSeq.empty())

  override fun md5Hash(): String = md5Hash
}