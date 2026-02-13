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
    CommandBeatmapCollectionInfo::class,
    CommandBeatmapCollectionExport::class,
    CommandBeatmapCollectionDownload::class,
    CommandLine.HelpCommand::class,
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

//  @CommandLine.Option(
//    names = ["--user-profile"],
//    paramLabel = "DIRECTORY",
//    description = ["Path to user profile directory"],
//    defaultValue = "."
//  )
//  lateinit var userProfile: File
  
  @CommandLine.Option(names = ["-v", "--verbose"])
  var verbose: Boolean = false
  
  @CommandLine.Spec
  lateinit var commandSpec: CommandLine.Model.CommandSpec
  
  @CommandLine.Option(
    names = ["--dry-run"],
    description = [
      "Don't update most thing to disk (except user token cause this is very important),",
      "this doesn't include user specified outputs, such as '-o'"
    ]
  )
  var dryRun: Boolean = false
  
  @CommandLine.Option(
    names = ["--no-refresh"],
    description = ["Don't refresh user token and fail when user token is expired, usually used with --dry-run"]
  )
  var noRefresh: Boolean = false

  class Prefer {
    @CommandLine.Option(
      names = ["--prefer-local"],
      description = ["Fetch data from local osu database, 'local_osu_path' in application.json must be set."],
    )
    var local: Boolean = false

    @CommandLine.Option(
      names = ["--prefer-mixed"],
      description = ["Fetch data from both osu.ppy.sh and local osu database, 'local_osu_path' in application.json must be set."]
    )
    var mixed: Boolean = false

    @CommandLine.Option(
      names = ["--prefer-remote"],
      description = ["Fetch data from osu.ppy.sh, this is the default option"]
    )
    var remote: Boolean = true

    override fun toString(): String {
      return when {
        local -> "local"
        mixed -> "mixed"
        remote -> "remote"
        else -> throw RuntimeException("unreachable")
      }
    }
  }

  @CommandLine.ArgGroup(
    exclusive = true,
    heading = "Specify the source where the data fetch from."
  )
  var prefer: Prefer = Prefer()
  
  override fun toString(): String {
    return """
      MainArgs(
        profile=$profile,
        verbose=$verbose,
        dryRun=$dryRun,
        noRefresh=$noRefresh,
        prefer=$prefer
      )
    """.trimIndent()
  }
}