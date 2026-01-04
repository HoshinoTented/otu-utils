package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId

interface IBeatmap {
  fun beatmapId(): BeatmapId
  fun title(): String
  fun difficultyName(): String
  fun starRate(): Float
  fun md5Hash(): String
}