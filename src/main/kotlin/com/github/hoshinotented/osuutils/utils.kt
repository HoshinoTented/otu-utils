package com.github.hoshinotented.osuutils

import kotlinx.serialization.json.Json

internal val commonSerde = Json {
  prettyPrint = true
  explicitNulls = false
}

