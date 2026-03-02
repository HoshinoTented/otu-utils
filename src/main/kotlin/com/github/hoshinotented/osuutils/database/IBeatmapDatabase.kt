package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.data.BeatmapCheckSum
import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetListed

interface IBeatmapDatabase {
  fun initialize()

  fun loadMaybe(id: BeatmapId): BeatmapCheckSum.Impl?
  fun load(id: BeatmapId): BeatmapCheckSum.Impl {
    return loadMaybe(id) ?: throw IllegalStateException("Unable to load beatmap $id from database")
  }

  fun loadSetMaybe(id: BeatmapSetId): BeatmapSetListed?
  fun loadSet(id: BeatmapSetId): BeatmapSetListed {
    return loadSetMaybe(id) ?: throw IllegalStateException("Unable to load beatmap set $id from database")
  }

  /**
   * Save [map] to database
   */
  fun save(map: BeatmapCheckSum)

  /**
   * Save [set] to database, including [BeatmapSetListed.beatmaps]
   */
  fun saveSet(set: BeatmapSetListed)
}