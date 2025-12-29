@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.osudb.LocalBeatmap
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * @param id the beatmap id
 * @param tag the tag of beatmap in pool, such as `NM1`. This is used for generating score list.
 */
@Serializable
data class BeatmapInCollection(
  val id: BeatmapId,
  val tag: String?,
)

@Serializable
data class BeatmapCollection(val name: String, val author: String, val beatmaps: ImmutableSeq<BeatmapInCollection>)