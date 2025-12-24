package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.io.FileIO
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.exists

class OsuApplicationDatabase(baseDir: Path, val io: FileIO) {
  val applicationConfigFile: Path = baseDir.resolve("app.json")
  
  private lateinit var applicationCache: OsuApplication
  
  fun load(): OsuApplication? {
    if (::applicationCache.isInitialized) return applicationCache
    if (!io.exists(applicationConfigFile)) return null
    applicationCache = commonSerde.decodeFromString(io.readText(applicationConfigFile))
    
    // validate
    // or maynot, we can't lock file...
//    val localOsuPath = applicationCache.localOsuPath
//    if (localOsuPath != null) {
//      val path = Path.of(localOsuPath)
//      if (! path.exists()) throw IOException("Directory $path doesn't exist")
//    }
    
    return applicationCache
  }
}