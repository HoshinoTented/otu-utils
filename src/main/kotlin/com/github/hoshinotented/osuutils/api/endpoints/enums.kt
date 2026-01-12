package com.github.hoshinotented.osuutils.api.endpoints

import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import java.util.function.Predicate

enum class Mode {
  Osu;
  
  override fun toString(): String {
    return when (this) {
      Osu -> "osu"
    }
  }
}

enum class Type {
  Best, Firsts, Recent;
  
  override fun toString(): String {
    return when (this) {
      Best -> "best"
      Firsts -> "firsts"
      Recent -> "recent"
    }
  }
}

// see https://osu.ppy.sh/wiki/en/Client/File_formats/osr_%28file_format%29
// the ordinal is bit offset
enum class Mod {
  NF, EZ, TD,
  HD, HR, SD, DT,
  RX, HT, NC,
  FL, AT, SO, RX2,
  PF,   // perfect
  
  // mania
  K4, K5, K6, K7, K8,
  FI,   // fade in
  RD,   // random
  CN,   // cinema
  TP,   // target practice, osu!cutting edge only
  K9,   // mania
  CP,   // coop
  K1, K3, K2,   // still mania
  V2,   // score v2
  MR;   // mirror
  
  companion object {
    fun asSeq(bits: Int): ImmutableSeq<Mod> {
      return asSet(bits).toSeq()
    }
    
    fun asSet(bits: Int): MutableEnumSet<Mod> {
      val set = MutableEnumSet.create(Mod::class.java)
      
      for (mod in entries) {
        val mask = 1 shl mod.ordinal
        val bit = bits and mask
        if (bit != 0) {
          set.add(mod)
        }
      }
      
      return set
    }
    
    fun toBitMask(mods: ImmutableSeq<Mod>): Int {
      var bits = 0x0
      
      for (mod in mods) {
        bits = bits or (1 shl mod.ordinal)
      }
      
      return bits
    }
    
    val DIFFICULTY_CHANGE_MODS = ImmutableSeq.of(EZ, HT, HR, HD, DT, NC, TD)
  }
}

/**
 * [Predicate.or], [Predicate.and], [ModCombination.of] and [ModCombination.combination] can cover all combination.
 */
sealed interface ModCombination : Predicate<ImmutableSeq<Mod>> {
  /**
   * Check if given mod combination satisfies [this] ModCombination. Only difficulty change mods have effect
   */
  override fun test(mods: ImmutableSeq<Mod>): Boolean
  
  class Exact(val exact: MutableEnumSet<Mod>) : ModCombination {
    /**
     * @return true if [mods] is exact the same as [exact]
     */
    override fun test(mods: ImmutableSeq<Mod>): Boolean {
      return exact == MutableEnumSet.from(Mod::class.java, mods.filter { it in Mod.DIFFICULTY_CHANGE_MODS })
    }
  }
  
  class Combination(val comb: MutableEnumSet<Mod>) : ModCombination {
    /**
     * @return true if [mods] is a subset of [comb]
     */
    override fun test(mods: ImmutableSeq<Mod>): Boolean {
      return mods.containsAll(mods.filter { it in Mod.DIFFICULTY_CHANGE_MODS })
    }
  }
  
  companion object {
    fun of(exact: ImmutableSeq<Mod>): ModCombination =
      Exact(MutableEnumSet.from(Mod::class.java, exact.filter { it in Mod.DIFFICULTY_CHANGE_MODS }))
    
    fun combination(mods: ImmutableSeq<Mod>): ModCombination =
      Combination(MutableEnumSet.from(Mod::class.java, mods.filter { it in Mod.DIFFICULTY_CHANGE_MODS }))
  }
}