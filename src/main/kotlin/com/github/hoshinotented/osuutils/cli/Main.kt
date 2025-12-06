package com.github.hoshinotented.osuutils.cli

import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.*
import com.github.hoshinotented.osuutils.io.DefaultFileIO
import com.github.hoshinotented.osuutils.io.DryFileIO
import picocli.CommandLine
import java.util.logging.Level
import java.util.logging.Logger

class Main : MainArgs() {
  internal val io by lazy { if (dryRun) DryFileIO else DefaultFileIO }
  internal val userDB by lazy { UserDatabase(userProfile.toPath()) }
  internal val appDB by lazy { OsuApplicationDatabase(profile.toPath(), io) }
  internal val scoreHistoryDB by lazy { ScoreHistoryDatabase(userProfile.resolve("beatmap_histories").toPath(), io) }
  internal val mapDB by lazy { BeatmapDatabase(profile.resolve("beatmaps").toPath(), io) }
  internal val analyzeMetadataDB by lazy { AnalyzeDatabase(userProfile.toPath(), io) }
  
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
  
  fun initLogger() {
    if (!verbose) {
      OsuApi.logger.level = Level.OFF
      cliLogger.level = Level.OFF
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
    cliLogger.info("Profile directory: " + profile.absolutePath)
    cliLogger.info("User profile directory: " + userProfile.absolutePath)
    
    commandSpec.commandLine().usage(System.out)
    return 0
  }
  
  internal fun user(): User {
    return userDB.load() ?: throw IllegalStateException("No user data is found")
  }
  
  internal fun app(): OsuApplication {
    return appDB.load()?.apply {
      dontRefreshToken = noRefresh
    } ?: throw IllegalStateException("No osu application data is found")
  }
}

fun main(args: Array<String>) {
  val app = Main()
  CommandLine(app)
    .setExecutionStrategy(app::executionStrategy)
    .execute(*args)
}