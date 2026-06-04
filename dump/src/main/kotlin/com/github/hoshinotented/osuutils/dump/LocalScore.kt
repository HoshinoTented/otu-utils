package com.github.hoshinotented.osuutils.dump

import com.github.hoshinotented.osuutils.data.Mod
import com.github.hoshinotented.osuutils.data.UserId
import kotlin.time.Instant

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
}