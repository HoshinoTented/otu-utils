package com.github.hoshinotented.osuutils.api.endpoints

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

// in the same order as mod select panel, top to bottom and left to right
enum class Mod {
  EZ, NF, HT, HR, DT, HD, FL
}