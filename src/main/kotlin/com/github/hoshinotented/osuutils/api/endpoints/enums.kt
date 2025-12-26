package com.github.hoshinotented.osuutils.api.endpoints

import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList

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
    fun from(bits: Int): ImmutableSeq<Mod> {
      val result = FreezableMutableList.create<Mod>()
      
      for (mod in entries) {
        val mask = 1 shl mod.ordinal
        val bit = bits and mask
        if (bit != 0) {
          result.append(mod)
        }
      }
      
      return result.toSeq()
    }
    
    fun toBitMask(mods: ImmutableSeq<Mod>): Int {
      var bits = 0x0
      
      for (mod in mods) {
        bits = bits or (1 shl mod.ordinal)
      }
      
      return bits
    }
  }
}