package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.io.FileIO
import java.nio.file.Path

class OsuApplicationDatabase(baseDir: Path, val io: FileIO) {
  val applicationConfigFile: Path = baseDir.resolve("app.json")
  
  private lateinit var applicationCache: OsuApplication
  
  fun load(): OsuApplication? {
    if (::applicationCache.isInitialized) return applicationCache
    if (!io.exists(applicationConfigFile)) return null
    applicationCache = commonSerde.decodeFromString(io.readText(applicationConfigFile))
    return applicationCache
  }
}