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

enum class Mod {
  NF, DT, HD, HR
}