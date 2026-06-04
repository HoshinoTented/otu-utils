package com.github.hoshinotented.osuutils.api

import kotlinx.serialization.json.Json
import java.net.http.HttpClient
import java.util.logging.Logger

object OsuApi {
  const val BASE_URL = "https://osu.ppy.sh"
  const val API_V2 = "$BASE_URL/api/v2/"

  val logger: Logger = Logger.getLogger("OsuAPI")
  
  val deJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    explicitNulls = false
  }
  
  val client: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    // see BeatmapCollectionActions#download
//    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()
}

