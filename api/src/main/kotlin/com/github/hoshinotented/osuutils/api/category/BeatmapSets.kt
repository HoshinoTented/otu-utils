package com.github.hoshinotented.osuutils.api.category

import com.github.hoshinotented.osuutils.api.ApplicationRole
import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.category.Authentication.sendAuthedRequest
import com.github.hoshinotented.osuutils.api.checkNotFound
import com.github.hoshinotented.osuutils.api.data.BeatmapSetListed
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSets
import com.github.hoshinotented.osuutils.api.successOrThrow
import com.github.hoshinotented.osuutils.data.BeatmapSetId

object BeatmapSets {
  fun ApplicationRole.beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSetListed? {
    val req = BeatmapSets.BeatmapSet(beatmapSetId)
    val resp = sendAuthedRequest(req.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null

    return OsuApi.deJson.decodeFromString<BeatmapSetListed.Impl>(resp)
  }
}