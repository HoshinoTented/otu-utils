@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class BeatmapCollection(val name: String, val author: String, val beatmaps: ImmutableSeq<BeatmapId>)