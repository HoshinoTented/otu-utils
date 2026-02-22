@file:UseSerializers(SeqSerializer::class, ModRestriction.Serde::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSetId
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

  fun toBeatmap(): Beatmap = Beatmap(
    setId, id, starRate, difficultyName, BeatmapSet(setId, title, title(), null), md5Hash
  )
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
)

@Serializable
data class BeatmapCollection(val name: String, val author: String, val beatmaps: ImmutableSeq<BeatmapInCollection>)