package com.github.hoshinotented.osuutils.dump

import com.github.hoshinotented.osuutils.api.data.BeatmapExtended
import com.github.hoshinotented.osuutils.api.data.BeatmapSet
import com.github.hoshinotented.osuutils.api.data.Score
import com.github.hoshinotented.osuutils.data.Mod
import com.github.hoshinotented.osuutils.data.UserId
import kala.collection.immutable.ImmutableSeq

fun LocalBeatmap.toBeatmap(): BeatmapExtended {
  val star = starRate(ImmutableSeq.empty())

  return BeatmapExtended.Impl(
    beatmapSetId.toLong(), beatmapId.toLong(), difficultyName, star,
    md5Hash(),
    BeatmapSet.Impl(beatmapSetId.toLong(), title, titleUnicode ?: title)
  )
}


/**
 * @param beatmapProvider find [Beatmap] by md5 hash
 */
fun LocalScore.toScore(userId: UserId): Score {
  return Score(
    accuracy, createTime,
    scoreId, Mod.asSeq(mods), userId,
    null
  )
}