package com.github.hoshinotented.osuutils

import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.osudb.LocalBeatmap
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.toJavaInstant

fun prettyBeatmap(set: BeatmapSet, map: Beatmap): String {
  return "${set.titleUnicode} / ${map.version} / ${Beatmap.prettyDifficulty(map.difficulty)}"
}

fun prettyBeatmap(local: LocalBeatmap): String {
  return "${local.titleUnicode ?: local.title} / ${local.difficultyName} / ${Beatmap.prettyDifficulty(local.od)}"
}

fun prettyTime(time: Instant): String {
  return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
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