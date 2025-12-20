// https://github.com/ppy/osu/wiki/Legacy-database-file-structure
// we assume the database comes from newest version, thus version related format problem is gone

package com.github.hoshinotented.osuutils.osudb

import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.lang.RuntimeException
import java.nio.charset.Charset
import kotlin.time.Instant

annotation class Padding(val bytes: Int)

class OsuParseException(msg: String) : RuntimeException(msg)

data class IntFloatPair(val int: Int, val float: Float)

data class TimePoint(val bpm: Double, val offset: Double, val inherit: Boolean)

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
  val lastModified: Long,   // windows ticks
  val ar: Float,
  val cs: Float,
  val hp: Float,
  val difficulty: Float,
  val sliderVelocity: Double,
  val stdModdedStarCache: ImmutableSeq<IntFloatPair>,
  val taikoModdedStarCache: ImmutableSeq<IntFloatPair>,
  val ctbModdedStarCache: ImmutableSeq<IntFloatPair>,
  val maniaModdedStarCache: ImmutableSeq<IntFloatPair>,
  val drainTimeSeconds: Int,
  val totalTimeMilliseconds: Int,
  val someMysteryTimeWhichIDontKnowInMilliseconds: Int,
  val timePoints: ImmutableSeq<TimePoint>,
  val difficultyId: Int,
  val beatmapId: Int,
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
  val lastTimePlayed: Long,
  val isOsz2: Boolean,
  val folderName: String,
  val lastTimeChecked: Long,
  val ignoreSound: Boolean,
  val ignoreSkin: Boolean,
  val disableStoryboard: Boolean,
  val disableVideo: Boolean,
  val visualOverride: Boolean,
  // val unknown: Short,
  val lastModifiedTime: Int,
  val maniaScrollSpeed: Byte,
) {
  companion object {
    const val RANKED_STATUS_UNKNOWN: Byte = 0
    const val RANKED_STATUS_UNSUBMITTED: Byte = 1
    const val RANKED_STATUS_GRAVEYARD: Byte = 2
    const val RANKED_STATUS_UNUSED: Byte = 3
    const val RANKED_STATUS_RANKED: Byte = 4
    const val RANKED_STATUS_APPROVED: Byte = 5
    const val RANKED_STATUS_QUALIFIED: Byte = 6
    const val RANKED_STATUS_LOVED: Byte = 7
    
    const val GAMEPLAY_MODE_STD = 0
    const val GAMEPLAY_MODE_TAIKO = 1
    const val GAMEPLAY_MODE_CTB = 2
    const val GAMEPLAY_MODE_MANIA = 3
  }
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
    const val PERMISSION_NORMAL: Int = 0b1
    const val PERMISSION_MODERATOR: Int = 0b10
    const val PERMISSION_SUPPORTER: Int = 0b100
    const val PERMISSION_FRIEND: Int = 0b1000
    const val PERMISSION_PEPPY: Int = 0b10000   // ??
    const val PERMISSION_STAFF: Int = 0b100000
  }
}

fun parseOsu(bytes: ByteArrayInputStream) {

}