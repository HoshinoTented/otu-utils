package com.github.hoshinotented.osuutils.api.category

import com.github.hoshinotented.osuutils.api.ApplicationRole
import com.github.hoshinotented.osuutils.api.OsuApi.deJson
import com.github.hoshinotented.osuutils.api.SeqSerializer
import com.github.hoshinotented.osuutils.api.category.Authentication.sendAuthedRequest
import com.github.hoshinotented.osuutils.api.data.Mode
import com.github.hoshinotented.osuutils.api.data.OsuUser
import com.github.hoshinotented.osuutils.api.data.Score
import com.github.hoshinotented.osuutils.api.data.Type
import com.github.hoshinotented.osuutils.api.endpoints.Users
import com.github.hoshinotented.osuutils.api.successOrThrow
import kala.collection.immutable.ImmutableSeq

object Users {
  /**
   * @return the user, may with new token
   */
  fun ApplicationRole.me(): OsuUser {
    val resp = sendAuthedRequest(Users.Me().toRequest())
    val json = resp.successOrThrow()
    val user = deJson.decodeFromString<OsuUser>(json)

    return user
  }

  // Looks like limit is up to 40
  // ^ no, the server only save 40 recent play score
  fun ApplicationRole.recentScores(user: OsuUser, limit: Int, offset: Int = 0): ImmutableSeq<Score> {
    val reqObj = Users.Scores(
      user.id,
      Type.Recent,
      legacyOnly = true, includeFails = false,
      mode = Mode.Osu,
      limit = limit,
      offset = offset
    )
    val resp = sendAuthedRequest(reqObj.toRequest())
      .successOrThrow()

    return deJson.decodeFromString(SeqSerializer(Score.Companion.serializer()), resp)
  }
}