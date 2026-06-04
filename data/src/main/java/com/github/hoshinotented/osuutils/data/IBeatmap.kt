package com.github.hoshinotented.osuutils.data

interface IBeatmap {
  fun beatmapId(): BeatmapId
  fun title(): String
  fun difficultyName(): String
  fun starRate(): Float
  fun md5Hash(): String
}