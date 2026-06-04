// https://github.com/ppy/osu/wiki/Legacy-database-file-structure
// we assume the database comes from newest version, thus version related format problem is gone

package com.github.hoshinotented.osuutils.dump

import com.github.hoshinotented.osuutils.data.Mod
import kala.collection.SeqView
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet

annotation class Sized(val bytes: Int)

data class IntFloatPair(val int: Int, val float: Float)

@Sized(bytes = 10)    // 4 + 4 + 2 where 2 is for two indicators
data class ModStarCache(val mods: MutableEnumSet<Mod>, val difficulty: Float)

@Sized(bytes = 17)
data class TimePoint(val bpm: Double, val offset: Double, val inherit: Boolean)

/**
 * @param T must be [Sized]
 */
class LazySeq<T>(val initializer: Lazy<ImmutableSeq<T>>) : SeqView<T> {
  /**
   * May fail
   */
  val value: ImmutableSeq<T> get() = initializer.value
  
  override fun iterator(): MutableIterator<T> {
    return value.iterator()
  }
}

data class LocalScoredBeatmap(val md5Hash: String, val scores: ImmutableSeq<LocalScore>)

data class LocalScores(
  val version: Int,
  val scoredBeatmaps: ImmutableSeq<LocalScoredBeatmap>,
)

data class LocalCollection(val name: String, val beatmaps: ImmutableSeq<String>)

data class LocalCollections(
  val version: Int,
  val collections: ImmutableSeq<LocalCollection>
)