package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.data.ScoreHistory
import kala.collection.immutable.ImmutableSeq

interface IScoreHistoryDatabase {
  fun initialize()
  fun listHistories(): ImmutableSeq<ScoreHistory>
  fun loadHistory(id: BeatmapId): ScoreHistory?
  fun saveHistory(history: ScoreHistory)
}