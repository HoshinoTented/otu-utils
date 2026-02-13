package com.github.hoshinotented.osuutils.providers

import com.github.hoshinotented.osuutils.api.Beatmaps.beatmapScores
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.osudb.LocalOsu
import com.github.hoshinotented.osuutils.osudb.LocalScores
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import kala.collection.mutable.MutableList

interface ScoreProvider {
  /**
   * @return scores, always ordered by [Score.CreateTimeComparator]
   */
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
class LocalOsuScoreProvider(val osu: LocalOsu, scores: LocalScores, val v2Only: Boolean) : ScoreProvider {
  private val byHash = scores.scoredBeatmaps.associateBy { it.md5Hash }

  // TODO: filter by [user], since not all local score belongs to [user] (although it is true in most case)
  override fun beatmapScores(
    user: User,
    beatmapId: BeatmapId,
  ): ImmutableSeq<Score>? {
    val beatmapById = osu.beatmapById

    val beatmap = beatmapById[beatmapId] ?: return null
    val hash = beatmap.md5Hash
    val scores = byHash[hash] ?: return ImmutableSeq.empty()
    return scores.scores.view()
      .filter { it.playerName == user.player.userName && (!v2Only || Mod.has(it.mods, Mod.V2)) }
      .map {
        it.toScore(user.player.id) {
          beatmap.toBeatmap()
        }
      }.sorted(Score.CreateTimeComparator)
      .toSeq()
  }
}

/**
 * Combine results of [ScoreProvider], scores are distinct by creation time.
 * For [LocalOsuScoreProvider], you may use [com.github.hoshinotented.osuutils.api.endpoints.Mod.V2] only to improve
 * performance.
 */
class MergeScoreProvider(val lhs: ScoreProvider, val rhs: ScoreProvider) : ScoreProvider {
  override fun beatmapScores(user: User, beatmapId: BeatmapId): ImmutableSeq<Score>? {
    val lhsResult = lhs.beatmapScores(user, beatmapId)
    val rhsResult = rhs.beatmapScores(user, beatmapId)

    if (lhsResult == null && rhsResult == null) return null
    val result = (lhsResult ?: ImmutableSeq.empty())
      .view()
      .appendedAll(rhsResult ?: ImmutableSeq.empty())
      .distinctBy(ImmutableSeq.factory()) { it.createdAt }
      // we may use improved sort algorithm like merge sort, as the result of [beatmapScores] is ordered
      .sorted(Score.CreateTimeComparator)

    return result
  }
}