package com.github.hoshinotented.osuutils.providers

import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.data.ScoreHistory
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.ScoreHistoryDatabase
import com.github.hoshinotented.osuutils.initializeScoreHistory
import kala.collection.immutable.ImmutableSeq

class ScoreHistoryProvider(val application: OsuApplication, val user: User, val historyDB: ScoreHistoryDatabase) {
  fun history(beatmapId: BeatmapId): ScoreHistory {
    val history = historyDB.load(beatmapId)
    // in case no record
    if (history.best == null) {
      val history = application.initializeScoreHistory(user, beatmapId)
      historyDB.save(history)
      return history
    } else return history
  }
  
  // similar identical to ScoreHistoryDatabase#loadAll
  fun histories(): ImmutableSeq<ScoreHistory> {
    return historyDB.tracking().beatmaps.map {
      history(it.id)
    }
  }
}