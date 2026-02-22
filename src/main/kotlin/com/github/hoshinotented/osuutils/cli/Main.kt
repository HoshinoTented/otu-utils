package com.github.hoshinotented.osuutils.cli

import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.*
import com.github.hoshinotented.osuutils.io.DefaultFileIO
import com.github.hoshinotented.osuutils.io.DryFileIO
import com.github.hoshinotented.osuutils.osudb.*
import com.github.hoshinotented.osuutils.providers.BeatmapProviderImpl
import com.github.hoshinotented.osuutils.providers.LocalOsuScoreProvider
import com.github.hoshinotented.osuutils.providers.MergeScoreProvider
import com.github.hoshinotented.osuutils.providers.OnlineScoreProvider
import com.google.common.io.LittleEndianDataInputStream
import picocli.CommandLine
import java.io.IOException
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.*

class Main : MainArgs() {
  internal val io by lazy { if (dryRun) DryFileIO else DefaultFileIO }
  internal val userDB by lazy { UserDatabase(profile.toPath()) }
  internal val appDB by lazy { OsuApplicationDatabase(profile.toPath(), io) }
  internal val scoreHistoryDB by lazy { ScoreHistoryDatabase(profile.resolve("beatmap_histories").toPath(), io) }
  internal val mapDB by lazy { BeatmapDatabase(profile.resolve("beatmaps").toPath(), io) }
  internal val analyzeMetadataDB by lazy { AnalyzeDatabase(profile.toPath(), io) }
  internal val localOsu by lazy {
    useLocalOsu("osu!.db") {
      parseLocalOsu(it, LocalOsuParseListener.Console())
    }
  }
  
  internal val localScores by lazy {
    useLocalOsu("scores.db") {
      parse(LocalScores::class, it)
        ?: throw OsuParseException("Unable to find any scores in scores.db, this could be either this application is out-of-date or the database is corrupted.")
    }
  }
  
  internal val scoreProvider by lazy {
    when {
      prefer.local -> LocalOsuScoreProvider(localOsu, localScores, true)
      prefer.mixed -> {
        val local = LocalOsuScoreProvider(localOsu, localScores, true)
        val online = OnlineScoreProvider(app())

        // online before local, as online score provide more information such as score link
        MergeScoreProvider(online, local)
      }

      else -> OnlineScoreProvider(app())
    }

  }
  
  internal val beatmapProvider by lazy {
    // never local
    BeatmapProviderImpl(app(), user(), mapDB)
  }
  
  val cliLogger = Logger.getLogger("CLI")
  
  inline fun catching(block: Main.() -> Int): Int {
    try {
      return block()
    } catch (e: Exception) {
      System.err.println(e.message)
      cliLogger.throwing("Main", "catching", e)
      return 1
    }
  }
  
  internal fun localOsuPath(): Path {
    val path = app().localOsuPath ?: throw IllegalArgumentException("No local_osu_path is specified.")
    return Path(path)
  }
  
  // remember to close
  private inline fun <R> useLocalOsu(db: String, block: (LittleEndianDataInputStream) -> R): R {
    // TODO: maybe also use FileIO... i don't know
    
    val thePath = localOsuPath()
    if (!thePath.exists() || !thePath.isDirectory()) throw IOException("Directory not found: $thePath")
    val dbFile = thePath.resolve(db)
    if (!dbFile.exists() || !dbFile.isRegularFile()) throw IOException("Database file not found: $dbFile")

    return LittleEndianDataInputStream(dbFile.inputStream()).use(block)
  }
  
  fun initLogger() {
    if (!verbose) {
      OsuApi.logger.level = Level.OFF
      cliLogger.level = Level.OFF
    } else {
      OsuApi.logger.level = Level.ALL
      cliLogger.level = Level.ALL
    }
  }
  
  fun executionStrategy(parseResult: CommandLine.ParseResult): Int {
    try {
      initLogger()
      return CommandLine.RunLast().execute(parseResult)
    } finally {
      userDB.save()
    }
  }
  
  override fun call(): Int {
    cliLogger.info("Options: $this")
    
    commandSpec.commandLine().usage(System.out)
    return 0
  }
  
  internal fun user(): User {
    return userDB.load() ?: throw IllegalStateException("No user data is found, use 'auth' command to initialize.")
  }
  
  internal fun app(): OsuApplication {
    return appDB.load()?.apply {
      dontRefreshToken = noRefresh
    } ?: throw IllegalStateException("No osu application data is found, use 'init' command to initialize.")
  }
}

fun main(args: Array<String>) {
  val app = Main()
  CommandLine(app)
    .setExecutionStrategy(app::executionStrategy)
    .execute(*args)
}