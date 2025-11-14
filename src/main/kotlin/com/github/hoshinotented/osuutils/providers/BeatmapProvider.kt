package com.github.hoshinotented.osuutils.providers

import com.github.hoshinotented.osuutils.api.BeatmapSets.beatmapSet
import com.github.hoshinotented.osuutils.api.Beatmaps.beatmap
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSetId
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.BeatmapDatabase

class BeatmapProvider(val application: OsuApplication, val user: User, val database: BeatmapDatabase) {
  /**
   * @return null if no such beatmap
   */
  fun beatmap(beatmapId: BeatmapId): Beatmap? {
    var beatmap = database.load(beatmapId)
    if (beatmap != null) return beatmap
    
    beatmap = application.beatmap(user, beatmapId)
    if (beatmap == null) return null
    
    // we don't save this beatmap, but instead the beatmap set
    val set = application.beatmapSet(user, beatmap.beatmapSetId)
      ?: throw AssertionError("What do you mean a map is not belongs to its beatmap set??")
    
    database.saveSet(set)
    return beatmap
  }
  
  fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    var set = database.loadSet(beatmapSetId)
    if (set != null) return set
    
    set = application.beatmapSet(user, beatmapSetId)
    if (set == null) return null
    
    database.saveSet(set)
    return set
  }
}