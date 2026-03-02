package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.data.BeatmapCheckSum
import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetId

open class BeatmapInDb(
  var id: BeatmapId = 0,
  var setId: BeatmapSetId = 0,
  var difficulty: String? = null,
  var starRate: Float = 0.0F,
  var checksum: String? = null,
) {
  companion object {
    fun from(map: BeatmapCheckSum): BeatmapInDb = BeatmapInDb(
      map.id,
      map.setId,
      map.difficulty,
      map.starRate,
      map.checksum,
    )
  }

  fun toBeatmap(): BeatmapCheckSum.Impl = BeatmapCheckSum.Impl(
    id, setId, difficulty!!, starRate, checksum!!
  )

  override fun toString(): String {
    return "BeatmapInDb(id=$id, setId=$setId, difficulty=$difficulty, starRate=$starRate, checksum=$checksum)"
  }
}

open class BeatmapSetInDb(
  var id: BeatmapSetId = 0,
  var title: String? = null,
  var titleUnicode: String? = null,
  var beatmaps: List<BeatmapInDb>? = null,
)