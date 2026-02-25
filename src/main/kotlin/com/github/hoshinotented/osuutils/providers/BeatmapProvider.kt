package com.github.hoshinotented.osuutils.providers

import com.github.hoshinotented.osuutils.api.BeatmapSets.beatmapSet
import com.github.hoshinotented.osuutils.api.Beatmaps.beatmap
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetId
import com.github.hoshinotented.osuutils.api.data.MyBeatmapCheckSum
import com.github.hoshinotented.osuutils.api.data.MyBeatmapExtended
import com.github.hoshinotented.osuutils.api.data.MyBeatmapSet
import com.github.hoshinotented.osuutils.api.data.MyBeatmapSetListed
import com.github.hoshinotented.osuutils.data.BeatmapInCollection
import com.github.hoshinotented.osuutils.data.BeatmapInfoCache
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.BeatmapDatabase
import com.github.hoshinotented.osuutils.osudb.LocalOsu
import kala.collection.immutable.ImmutableSeq

interface BeatmapProvider {
  /**
   * @return if not null, [com.github.hoshinotented.osuutils.api.data.Beatmap.beatmapSet] and [com.github.hoshinotented.osuutils.api.data.Beatmap.checksum] is always set
   */
  fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended?

  /**
   * @return if not null, [com.github.hoshinotented.osuutils.api.data.BeatmapSet.beatmaps] is always set
   */
  fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed?
  
  fun or(other: BeatmapProvider): BeatmapProvider = ChainedBeatmapProvider(this, other)
}

class OnlineBeatmapProvider(val application: OsuApplication, val user: User) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended? {
    return application.beatmap(user, beatmapId)
  }

  override fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed? {
    return application.beatmapSet(user, beatmapSetId)
  }
}

/**
 * @param delegate must NOT be [LocalBeatmapProvider]
 */
class CacheBeatmapProvider(val delegate: BeatmapProvider, val db: BeatmapDatabase) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended? {
    val result = delegate.beatmap(beatmapId)
    if (result != null) {
      val set = delegate.beatmapSet(result.setId)
        ?: throw AssertionError("What do you mean a map is not belongs to its beatmap set??")
      
      db.saveSet(set)
    }
    
    return result
  }

  override fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed? {
    return delegate.beatmapSet(beatmapSetId)?.also {
      db.saveSet(it)
    }
  }
}

class LocalBeatmapProvider(val db: BeatmapDatabase) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended? {
    val local = db.loadMaybe(beatmapId) ?: return null
    val set = db.loadSet(local.setId)

    return MyBeatmapExtended.Impl(
      local.id,
      local.setId,
      local.difficulty,
      local.starRate,
      local.checksum,
      MyBeatmapSet.Impl(
        set.id, set.title, set.titleUnicode
      )
    )
  }

  override fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed? {
    return db.loadSetMaybe(beatmapSetId)
  }
}

class LocalOsuBeatmapProvider(osu: LocalOsu) : BeatmapProvider {
  private val byBeatmapId = osu.beatmaps.associateBy { it.beatmapId }
  private val byBeatmapSetId = osu.beatmaps.groupBy { it.beatmapSetId }

  override fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended? {
    return byBeatmapId.getOrNull(beatmapId.toInt())?.toBeatmap()
  }

  override fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed? {
    return byBeatmapSetId[beatmapSetId.toInt()]?.let {
      val any = it.first()    // never empty
      MyBeatmapSetListed.Impl(
        any.beatmapSetId.toLong(),
        any.title,
        any.titleUnicode ?: any.title,
        ImmutableSeq.from(it.map { map ->
          MyBeatmapCheckSum.Impl(
            map.beatmapId.toLong(),
            map.beatmapSetId.toLong(),
            map.difficultyName,
            map.starRate(),
            map.md5Hash
          )
        })
      )
    }
  }
}

/**
 * A tiny beatmap provider that provides beatmaps from [collection],
 * any beatmap query that is not described in [collection] is considered missing.
 * This is used for [com.github.hoshinotented.osuutils.data.BeatmapCollection] actions.
 *
 * @param force if always re-fill [BeatmapInCollection.cache]
 */
class BeatmapCollectionBeatmapProvider(
  collection: ImmutableSeq<BeatmapInCollection>,
  val provider: BeatmapProvider,
  val force: Boolean,
) : BeatmapProvider {
  val collection: ImmutableSeq<BeatmapInCollection> = collection.map {
    val cache = if (force || it.cache == null) {
      provider.beatmap(it.id)?.let { beatmap ->
        BeatmapInfoCache.from(beatmap)
      }
    } else {
      it.cache
    }

    if (it.cache == cache) it else it.copy(cache = cache)
  }

  override fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended? {
    // i don't think this takes time...
    val map = collection.find { it.id == beatmapId }.orNull ?: return null
    if (map.cache == null) return null
    return map.cache.toBeatmap()
  }

  override fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed? {
    val maps = collection.filter { it.cache != null && it.cache.setId == beatmapSetId }
    if (maps.isEmpty) return null
    val first = maps.first
    val cache = first.cache!!   // never null
    return MyBeatmapSetListed.Impl(
      cache.setId, cache.title,
      cache.titleUnicode ?: cache.title,
      maps.map {
        it.cache!!.toBeatmap().downgrade()
      }
    )
  }
}

fun BeatmapProviderImpl(application: OsuApplication, user: User, database: BeatmapDatabase): BeatmapProvider {
  return LocalBeatmapProvider(database)
    .or(CacheBeatmapProvider(OnlineBeatmapProvider(application, user), database))
}

class ChainedBeatmapProvider(val left: BeatmapProvider, val right: BeatmapProvider) : BeatmapProvider {
  override fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended? {
    return left.beatmap(beatmapId) ?: right.beatmap(beatmapId)
  }

  override fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed? {
    return left.beatmapSet(beatmapSetId) ?: right.beatmapSet(beatmapSetId)
  }
}