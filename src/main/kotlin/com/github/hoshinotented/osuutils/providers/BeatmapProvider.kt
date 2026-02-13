package com.github.hoshinotented.osuutils.providers

import com.github.hoshinotented.osuutils.api.BeatmapSets.beatmapSet
import com.github.hoshinotented.osuutils.api.Beatmaps.beatmap
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSetId
import com.github.hoshinotented.osuutils.data.BeatmapInCollection
import com.github.hoshinotented.osuutils.data.BeatmapInfoCache
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.BeatmapDatabase
import com.github.hoshinotented.osuutils.osudb.LocalOsu
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableList

interface BeatmapProvider {
  /**
   * @return if not null, [Beatmap.beatmapSet] and [Beatmap.checksum] is always set
   */
  fun beatmap(beatmapId: BeatmapId): Beatmap?

  /**
   * @return if not null, [BeatmapSet.beatmaps] is always set
   */
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

class BeatmapCollectionBeatmapProvider(
  collection: ImmutableSeq<BeatmapInCollection>,
  val provider: BeatmapProvider,
  val force: Boolean,
) : BeatmapProvider {
  val collection: ImmutableSeq<BeatmapInCollection> = collection.map {
    val cache = if (force || it.cache == null) {
      val beatmap = provider.beatmap(it.id)
      if (beatmap == null) null else {
        val set = beatmap.beatmapSet!!
        BeatmapInfoCache(
          beatmap.id,
          beatmap.beatmapSetId,
          set.title,
          set.titleUnicode,
          beatmap.version,
          beatmap.difficulty,
          beatmap.checksum!!
        )
      }
    } else {
      it.cache
    }

    if (it.cache == cache) it else it.copy(cache = cache)
  }

  override fun beatmap(beatmapId: BeatmapId): Beatmap? {
    // i don't think this takes time...
    val map = collection.find { it.id == beatmapId }.orNull ?: return null
    if (map.cache == null) return null
    return map.cache.toBeatmap()
  }

  override fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    val maps = collection.filter { it.cache != null && it.cache.setId == beatmapSetId }
    if (maps.isEmpty) return null
    val first = maps.first
    val cache = first.cache!!   // never null
    return BeatmapSet(
      cache.setId, cache.title, cache.titleUnicode ?: cache.title, maps.map { it.cache!!.toBeatmap() }
    )
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