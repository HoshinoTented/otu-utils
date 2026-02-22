package com.github.hoshinotented.osuutils.serde

import com.github.hoshinotented.osuutils.data.BeatmapInCollection
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject

class BeatmapInCollectionSerializer : JsonTransformingSerializer<BeatmapInCollection>(
  BeatmapInCollection.generatedSerializer()
) {
  override fun transformDeserialize(element: JsonElement): JsonElement {
    if (element !is JsonPrimitive) return element
    return buildJsonObject {
      put("id", element)
    }
  }
}