package com.github.hoshinotented.osuutils

import com.github.hoshinotented.osuutils.api.data.Beatmap
import com.github.hoshinotented.osuutils.api.data.BeatmapSet
import com.github.hoshinotented.osuutils.api.data.Mod
import com.github.hoshinotented.osuutils.data.IBeatmap
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

//fun prettyBeatmap(local: LocalBeatmap): String {
//  return prettyBeatmap(local.titleUnicode ?: local.title, local.difficultyName, local.starRate())
//}

fun prettyBeatmap(title: String, difficultyName: String, starRate: Float): String {
  return "$title / $difficultyName / ${Beatmap.prettyDifficulty(starRate)}"
}

fun prettyBeatmap(beatmap: IBeatmap): String {
  return prettyBeatmap(beatmap.title(), beatmap.difficultyName(), beatmap.starRate())
}


fun prettyTime(time: Instant): String {
  return DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())
    .format(time.toJavaInstant())
}

fun prettyMods(mods: ImmutableSeq<Mod>, prefix: String = "+"): String {
  return prettyMods(MutableEnumSet.from(Mod::class.java, mods), prefix)
}

fun prettyMods(mods: MutableEnumSet<Mod>, prefix: String = "+"): String {
  if (mods.isEmpty) return ""
  
  return buildString {
    append(prefix)
    Mod.entries.forEach {
      if (it in mods) {
        append(it)
      }
    }
  }
}