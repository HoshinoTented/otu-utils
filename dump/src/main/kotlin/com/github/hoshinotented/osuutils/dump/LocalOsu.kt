package com.github.hoshinotented.osuutils.dump

import com.github.hoshinotented.osuutils.data.BeatmapId
import kala.collection.immutable.ImmutableMap
import kala.collection.immutable.ImmutableSeq
import kotlin.time.Instant

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

  val beatmapById: ImmutableMap<BeatmapId, LocalBeatmap> by lazy { beatmaps.associateBy { it.beatmapId.toLong() } }
}