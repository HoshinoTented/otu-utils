package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.commonSerde
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class OsuApplicationDatabase(baseDir: Path) {
  val applicationConfigFile: Path = baseDir.resolve("app.json")
  
  private lateinit var applicationCache: OsuApplication
  
  fun load(): OsuApplication? {
    if (::applicationCache.isInitialized) return applicationCache
    if (!applicationConfigFile.exists()) return null
    applicationCache = commonSerde.decodeFromString(applicationConfigFile.readText())
    return applicationCache
  }
}