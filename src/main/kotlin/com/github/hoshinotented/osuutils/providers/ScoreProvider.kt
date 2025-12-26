package com.github.hoshinotented.osuutils.providers

import com.github.hoshinotented.osuutils.api.Beatmaps.beatmapScores
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.osudb.LocalOsu
import com.github.hoshinotented.osuutils.osudb.LocalScores
import kala.collection.immutable.ImmutableSeq

interface ScoreProvider {
  fun beatmapScores(user: User, beatmapId: BeatmapId): ImmutableSeq<Score>?
}

class OnlineScoreProvider(val application: OsuApplication) : ScoreProvider {
  override fun beatmapScores(
    user: User,
    beatmapId: BeatmapId,
  ): ImmutableSeq<Score>? {
    return application.beatmapScores(user, beatmapId)
  }
}

// TODO: we need something that combines online score and local (v2) score
class LocalOsuScoreProvider(osu: LocalOsu, scores: LocalScores) : ScoreProvider {
  private val byBeatmapId = osu.beatmaps.associateBy { it.beatmapId }
  private val beatmapByHash = osu.beatmaps.associateBy { it.md5Hash }
  private val byHash = scores.scoredBeatmaps.associateBy { it.md5Hash }
  
  // TODO: filter by [user], since not all local score belongs to [user] (although it is true in most case)
  override fun beatmapScores(
    user: User,
    beatmapId: BeatmapId,
  ): ImmutableSeq<Score>? {
    val beatmap = byBeatmapId[beatmapId.toInt()] ?: return null
    val hash = beatmap.md5Hash
    val scores = byHash[hash] ?: return ImmutableSeq.empty()
    return ImmutableSeq.from(scores.scores.map {
      it.toScore(user.player.id) { hash ->
        beatmapByHash[hash]?.toBeatmap()
      }
    })
  }
}