package com.github.hoshinotented.osuutils.util;

import com.github.hoshinotented.osuutils.api.prettyBeatmap
import com.github.hoshinotented.osuutils.dump.LocalBeatmap
import com.github.hoshinotented.osuutils.dump.deser.LocalOsuParseListener

class ConsoleOsuParseListener : LocalOsuParseListener {
  companion object {
      const val TITLE = "Loading Beatmaps"
  }

  private var firstVisited: Boolean = false

  override fun beforeParseBeatmap(index: Int, max: Int) {
    if (!firstVisited) {
      firstVisited = true
      ProgressIndicator.Console.progress(index + 1, max, TITLE, null)
    }
  }

  override fun afterParseBeatmap(index: Int, max: Int, beatmap: LocalBeatmap) {
    ProgressIndicator.Console.progress(index + 1, max, TITLE, prettyBeatmap(beatmap))
  }
}
