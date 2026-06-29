package com.github.hoshinotented.osuutils.dump.deser

import com.github.hoshinotented.osuutils.data.Mod
import kala.collection.SeqView
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Sized(val bytes: Int)

@Target(AnnotationTarget.CLASS)
annotation class BackingType(val value: KClass<*>)

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