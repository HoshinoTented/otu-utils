package com.github.hoshinotented.osuutils.cli

import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.Users
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.cli.action.AnalyzeAction
import com.github.hoshinotented.osuutils.cli.action.BeatmapCollectionActions
import com.github.hoshinotented.osuutils.cli.action.RenderScoresAction
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.data.BeatmapCollection
import com.github.hoshinotented.osuutils.prettyBeatmap
import com.github.hoshinotented.osuutils.util.ProgressIndicator
import kala.collection.immutable.ImmutableSeq
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable
import kotlin.io.writeText

@CommandLine.Command(name = "auth")
class CommandAuth : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  @CommandLine.Parameters(index = "0")
  lateinit var code: String
  
  override fun call(): Int = parent.catching {
    val app = app()
    val existUser = userDB.load()
    if (existUser != null) {
      println("Existing user in ${profile}, overwrite? [Y/N]")
      val line = readlnOrNull()
      if (line == null || (line != "Y" && line != "y")) {
        println("Terminated")
        return@catching 1
      }
    }
    
    with(OsuApi) {
      val user = app.newUser(code)
      userDB.save(user)
      
      println("Hello, ${user.player.userName}!")
    }
    
    0
  }
}

@CommandLine.Command(name = "me", description = ["Print user information"])
class CommandMe : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  override fun call(): Int = parent.catching {
    val app = app()
    var user = user()
    
    user = with(Users) {
      app.me(user.token)
    }
    
    userDB.save(user)
    
    println("Hello, ${user.player.userName}!")
    println("User id: ${user.player.id}")
    println("Play count: ${user.player.playcount}")
    
    return 0
  }
}

@CommandLine.Command(name = "analyze", description = ["Analyze recent scores"])
class CommandAnalyze : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  @CommandLine.Option(
    names = ["--show-recent-unplayed"],
    description = ["Show recent unplayed beatmap in the 'not played' list of the report"],
  )
  var showRecentUnplayed: Boolean = false
  
  @CommandLine.Option(
    names = ["-o", "--output"],
    paramLabel = "FILE",
    description = ["Specify the output file/directory of actions"]
  )
  var output: File? = null
  
  override fun call(): Int = parent.catching {
    parent.cliLogger.info("Run 'analyze' with: showRecentUnplayed=$showRecentUnplayed")
    
    val user = user()
    val app = app()
    val action =
      AnalyzeAction(
        app,
        user,
        analyzeMetadataDB,
        scoreHistoryDB,
        beatmapProvider,
        scoreProvider,
        ProgressIndicator.Console,
        AnalyzeAction.Options(showRecentUnplayed)
      )
    val analResult = action.analyze()
    val output = output
    
    if (output == null) {
      println(analResult)
    } else {
      // do not check parent directory, it's user's fault
      // don't use io, only io for database part
      output.writeText(analResult)
    }
    
    return 0
  }
}

@CommandLine.Command(name = "list-analyze", description = ["List all tracking beatmaps"])
class CommandListAnalyze() : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  override fun call(): Int = parent.catching {
    val tracking = scoreHistoryDB.tracking()
    if (tracking.beatmaps.isEmpty) {
      println("No beatmap are tracked currently.")
    } else {
      var allSuccess = true
      tracking.beatmaps.forEach {
        val map = mapDB.loadMaybe(it.id)
        if (map == null) {
          allSuccess = false
          System.err.println("Unable to load ${it.id} from database, please run 'analyze' to fetch beatmap information.")
          return@forEach
        }
        
        val set = mapDB.loadSet(map.beatmapSetId)
        println("Beatmap[${it.id}]: ${prettyBeatmap(set, map)}")
        println("Comment: ${it.comment ?: "no comment"}")
      }
      
      if (allSuccess) 0 else 1
    }
    
    0
  }
}

@CommandLine.Command(name = "rollback", description = ["Delete scores by last analyze"])
class CommandRollback() : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  override fun call(): Int = parent.catching {
    val user = user()
    val app = app()
    val action = AnalyzeAction(
      app, user, analyzeMetadataDB, scoreHistoryDB, beatmapProvider, scoreProvider,
      ProgressIndicator.Console
    )
    action.removeLastAnalyze(null)
    return 0
  }
}

@CommandLine.Command(name = "render-scores", description = ["Render scores of the beatmap to a chart"])
class CommandRenderScores() : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  @CommandLine.Parameters(index = "0", paramLabel = "BEATMAP ID")
  var beatmapId: BeatmapId = 0
  
  @CommandLine.Parameters(index = "1", paramLabel = "FILE", description = ["Render output, in .png format"])
  lateinit var outFile: File
  
  override fun call(): Int = parent.catching {
    val user = user()
    val db = scoreHistoryDB
    val scores = db.load(beatmapId)
    val map = mapDB.loadMaybe(beatmapId)
      ?: throw Exception("No local beatmap data for beatmap: $beatmapId.")
    val set = mapDB.loadSetMaybe(map.beatmapSetId)
      ?: throw Exception("No local beatmap set data for beatmap: $beatmapId, database might be corrupted.")
    val title = prettyBeatmap(set, map)
    
    RenderScoresAction(outFile, title, user.player.userName, scores.scores)
      .run()
    
    return@catching 0
  }
}

abstract class CommandBeatmapCollectionArgs() {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = ["Path to beatmap collection"])
  lateinit var pathToCollection: File
  
  @CommandLine.Option(
    names = ["-o"],
    paramLabel = "FILE",
    description = ["Path to new beatmap collection file, some cache will be wrote in"]
  )
  var pathToNewCollection: File? = null
  
  @CommandLine.Option(
    names = ["-f", "--force"],
    description = ["Ignore beatmap information cache and force to re-fetch"]
  )
  var force: Boolean = false
  
  @CommandLine.Option(
    names = ["--fill-mods"],
    description = ["Fill 'modSets' field in beatmap collection by the tag of beatmaps, only available if '-o' is specified"]
  )
  var fillMods = false
  
  fun beatmapCollection(): BeatmapCollection {
    return commonSerde.decodeFromString<BeatmapCollection>(pathToCollection.readText())
  }
  
  fun action(): BeatmapCollectionActions {
    return BeatmapCollectionActions(
      pathToNewCollection?.toPath(), force, fillMods, beatmapCollection(),
      ProgressIndicator.Console
    )
  }
}

@CommandLine.Command(name = "info-collection", description = ["Print information of a beatmap collection"])
class CommandBeatmapCollectionInfo() : CommandBeatmapCollectionArgs(), Callable<Int> {
  override fun call(): Int = parent.catching {
    val action = action()
    val success = action.info(beatmapProvider)
    
    if (success) 0 else 1
  }
}

@CommandLine.Command(
  name = "collection-scores",
  description = ["List highest score in collection from local osu!, V2 must on"]
)
class CommandBeatmapCollectionExport() : CommandBeatmapCollectionArgs(), Callable<Int> {
  @CommandLine.Option(
    names = ["-e", "--export"],
    paramLabel = "DIRECTORY",
    description = ["Export replays to given directory."]
  )
  var export: File? = null
  
  override fun call(): Int = parent.catching {
    val action = action()
    val success =
      action.scores(localOsuPath(), beatmapProvider, localScores, export?.toPath(), ImmutableSeq.of(Mod.V2))
    
    if (success) 0 else 1
  }
}

@CommandLine.Command(
  name = "download-collection",
  description = ["Download beatmaps in the collection, must not with '--prefer-local'"]
)
class CommandBeatmapCollectionDownload : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = ["Path to beatmap collection"])
  lateinit var pathToCollection: File
  
  @CommandLine.Parameters(index = "1", paramLabel = "DIRECTORY", description = ["Path to export output directory"])
  lateinit var outDir: File
  
  fun beatmapCollection(): BeatmapCollection {
    return commonSerde.decodeFromString<BeatmapCollection>(pathToCollection.readText())
  }
  
  override fun call(): Int = parent.catching {
    val action = BeatmapCollectionActions(null, false, false, beatmapCollection(), ProgressIndicator.Console)
    action.download(outDir.toPath(), beatmapProvider)
    0
  }
}