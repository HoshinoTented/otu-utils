@file:UseSerializers(SeqSerializer::class, ModRestriction.Serde::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.data.Beatmap
import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetId
import com.github.hoshinotented.osuutils.api.data.MyBeatmapExtended
import com.github.hoshinotented.osuutils.api.data.MyBeatmapSet
import com.github.hoshinotented.osuutils.serde.BeatmapInCollectionSerializer
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import com.github.hoshinotented.osuutils.util.ModRestriction
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Cache for [BeatmapInCollection], this will be added after any
 * [com.github.hoshinotented.osuutils.cli.action.BeatmapCollectionActions]
 * to speed up consequence actions
 */
@Serializable
data class BeatmapInfoCache(
  val id: BeatmapId,
  val setId: BeatmapSetId,
  val title: String,
  val titleUnicode: String?,
  val difficultyName: String,
  val starRate: Float,
  val md5Hash: String,
) : IBeatmap {
  override fun beatmapId(): BeatmapId = id
  override fun title(): String = titleUnicode ?: title
  override fun difficultyName(): String = difficultyName
  override fun starRate(): Float = starRate
  override fun md5Hash(): String = md5Hash

  fun toBeatmap(): MyBeatmapExtended.Impl = MyBeatmapExtended.Impl(
    setId, id, difficultyName, starRate, md5Hash, MyBeatmapSet.Impl(setId, title, title())
  )

  companion object {
    /**
     * @param map [Beatmap.checksum] and [Beatmap.beatmapSet] cannot be null
     */
    fun from(map: MyBeatmapExtended): BeatmapInfoCache {
      val set = map.beatmapSet
      return BeatmapInfoCache(
        map.id, map.setId, set.title, set.titleUnicode, map.difficulty, map.starRate, map.checksum
      )
    }
  }
}

/**
 * @param id the beatmap id
 * @param tag the tag of beatmap in pool, such as `NM1`. This is used for generating score list.
 * @param mods allowed mods combination, see [ModRestriction]
 */
@Serializable(with = BeatmapInCollectionSerializer::class)
@KeepGeneratedSerializer
data class BeatmapInCollection(
  val id: BeatmapId,
  val tag: String?,
  val comment: String?,
  val mods: ModRestriction?,
  val cache: BeatmapInfoCache?,
) {
  companion object {
    fun of(id: BeatmapId): BeatmapInCollection {
      return BeatmapInCollection(id, null, null, null, null)
    }
  }
}

@Serializable
data class BeatmapCollection(val name: String, val author: String, val beatmaps: ImmutableSeq<BeatmapInCollection>)