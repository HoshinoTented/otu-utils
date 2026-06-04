package com.github.hoshinotented.osuutils.api.category

import com.github.hoshinotented.osuutils.api.ApplicationRole
import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.OsuApi.deJson
import com.github.hoshinotented.osuutils.api.category.Authentication.sendAuthedRequest
import com.github.hoshinotented.osuutils.api.checkNotFound
import com.github.hoshinotented.osuutils.api.data.BeatmapExtended
import com.github.hoshinotented.osuutils.api.data.OsuUser
import com.github.hoshinotented.osuutils.api.data.Score
import com.github.hoshinotented.osuutils.api.endpoints.Beatmaps
import com.github.hoshinotented.osuutils.api.successOrThrow
import com.github.hoshinotented.osuutils.data.BeatmapId
import com.github.hoshinotented.osuutils.data.BeatmapSetId
import com.github.hoshinotented.osuutils.data.ScoreId
import kala.collection.immutable.ImmutableSeq

object Beatmaps {
  fun makeBeatmapUrl(beatmapId: BeatmapId): String {
    return "${OsuApi.BASE_URL}/b/$beatmapId"
  }

  fun makeBeatmapDownloadUrlSayobot(setId: BeatmapSetId): String {
    return "https://txy1.sayobot.cn/beatmaps/download/full/$setId"
  }

  /**
   * @param scoreId note that this is not [com.github.hoshinotented.osuutils.api.data.Score.id], but [com.github.hoshinotented.osuutils.api.data.ScoreUserAttribute.Pin.scoreId]
   */
  fun makeScoreUrl(scoreId: ScoreId): String {
    return "${OsuApi.BASE_URL}/scores/$scoreId"
  }

  fun ApplicationRole.bestScore(user: OsuUser, beatmapId: BeatmapId): Score? {
    val reqObj = Beatmaps.UserScore(beatmapId, user.id, legacyOnly = true)
    // it is possible that the response is 404, in this case, the user have no score of beatmap with id [beatmapId]
    val json = sendAuthedRequest(reqObj.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null
    val beatmapUserScore = deJson.decodeFromString<Beatmaps.UserScore.Response>(json)
    return beatmapUserScore.score
  }

  fun ApplicationRole.beatmapScores(user: OsuUser, beatmapId: BeatmapId): ImmutableSeq<Score>? {
    val reqObj = Beatmaps.UserScoreAll(beatmapId, user.id, legacyOnly = true)
    val resp = sendAuthedRequest(reqObj.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null
    val scores = deJson.decodeFromString<Beatmaps.UserScoreAll.Response>(resp)
    // response score are unsorted
    return scores.scores.sorted(Score.CreateTimeComparator)
  }

  fun ApplicationRole.beatmap(beatmapId: BeatmapId): BeatmapExtended? {
    val req = Beatmaps.Beatmap(beatmapId)
    val resp = sendAuthedRequest(req.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null

    return deJson.decodeFromString<BeatmapExtended.Impl>(resp)
  }
}