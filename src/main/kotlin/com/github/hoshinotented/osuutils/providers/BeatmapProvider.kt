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
import com.github.hoshinotented.osuutils.osudb.LocalOsu

interface BeatmapProvider {
  /**
   * @return if not null, [Beatmap.beatmapSet] and [Beatmap.checksum] is always set
   */
  fun beatmap(beatmapId: BeatmapId): Beatmap?
  fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet?
  
  fun or(other: BeatmapProvider): BeatmapProvider = ChainedBeatmapProvider(this, other)
}

class OnlineBeatmapProvider(val application: OsuApplication, val user: User) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): Beatmap? {
    return application.beatmap(user, beatmapId)
  }
  
  override fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    return application.beatmapSet(user, beatmapSetId)
  }
}

/**
 * @param delegate must NOT be [LocalBeatmapProvider]
 */
class CacheBeatmapProvider(val delegate: BeatmapProvider, val db: BeatmapDatabase) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): Beatmap? {
    val result = delegate.beatmap(beatmapId)
    if (result != null) {
      val set = delegate.beatmapSet(result.beatmapSetId)
        ?: throw AssertionError("What do you mean a map is not belongs to its beatmap set??")
      
      db.saveSet(set)
    }
    
    return result
  }
  
  override fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    return delegate.beatmapSet(beatmapSetId)?.also {
      db.saveSet(it)
    }
  }
}

class LocalBeatmapProvider(val db: BeatmapDatabase) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): Beatmap? {
    val local = db.loadMaybe(beatmapId) ?: return null
    val set = db.loadSet(local.beatmapSetId)
    
    return local.copy(beatmapSet = set)
  }
  
  override fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    return db.loadSetMaybe(beatmapSetId)
  }
}

class LocalOsuBeatmapProvider(osu: LocalOsu) : BeatmapProvider {
  private val byBeatmapId = osu.beatmaps.associateBy { it.beatmapId }
  private val byBeatmapSetId = osu.beatmaps.groupBy { it.beatmapSetId }
  
  override fun beatmap(beatmapId: BeatmapId): Beatmap? {
    return byBeatmapId.getOrNull(beatmapId.toInt())?.toBeatmap()
  }
  
  override fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    return byBeatmapSetId[beatmapSetId.toInt()]?.let {
      val any = it.first()    // never empty
      BeatmapSet(any.beatmapSetId.toLong(), any.title, any.titleUnicode ?: any.title, null)
    }
  }
}

fun BeatmapProviderImpl(application: OsuApplication, user: User, database: BeatmapDatabase): BeatmapProvider {
  return LocalBeatmapProvider(database)
    .or(CacheBeatmapProvider(OnlineBeatmapProvider(application, user), database))
}

class ChainedBeatmapProvider(val left: BeatmapProvider, val right: BeatmapProvider) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): Beatmap? {
    return left.beatmap(beatmapId) ?: right.beatmap(beatmapId)
  }
  
  override fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    return left.beatmapSet(beatmapSetId) ?: right.beatmapSet(beatmapSetId)
  }
}