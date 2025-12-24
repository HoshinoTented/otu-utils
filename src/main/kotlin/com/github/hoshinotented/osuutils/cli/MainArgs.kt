package com.github.hoshinotented.osuutils.cli

import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
  name = "otu-utils",
  version = ["otu-utils v${GenerateVersion.VERSION}"],
  subcommands = [
    CommandAuth::class,
    CommandMe::class,
    CommandAnalyze::class,
    CommandListAnalyze::class,
    CommandRollback::class,
    CommandRenderScores::class,
    CommandLine.HelpCommand::class
  ]
)
abstract class MainArgs : Callable<Int> {
  @CommandLine.Option(
    names = ["-p", "--profile"],
    paramLabel = "DIRECTORY",
    description = ["Path to profile directory"],
    defaultValue = "."
  )
  lateinit var profile: File
  
  @CommandLine.Option(
    names = ["--user-profile"],
    paramLabel = "DIRECTORY",
    description = ["Path to user profile directory"],
    defaultValue = "."
  )
  lateinit var userProfile: File
  
  @CommandLine.Option(names = ["-v", "--verbose"])
  var verbose: Boolean = false
  
  @CommandLine.Spec
  lateinit var commandSpec: CommandLine.Model.CommandSpec
  
  @CommandLine.Option(
    names = ["--dry-run"],
    description = ["Don't update almost any data to disk (except user token cause this is very important, '-o' still work)"]
  )
  var dryRun: Boolean = false
  
  @CommandLine.Option(
    names = ["--no-refresh"],
    description = ["Don't refresh user token and fail when user token is expired, usually used with --dry-run"]
  )
  var noRefresh: Boolean = false
  
  @CommandLine.Option(
    names = ["--prefer-local"],
    description = ["Fetch data from local osu database if possible, 'local_osu_path' in application.json must be set."]
  )
  var preferLocal: Boolean = false
}