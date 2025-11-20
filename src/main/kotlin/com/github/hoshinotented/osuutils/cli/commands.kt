package com.github.hoshinotented.osuutils.cli

import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.Users
import com.github.hoshinotented.osuutils.cli.action.AnalyzeAction
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.io.writeText

@CommandLine.Command(name = "auth")
class CommandAuth() : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  @CommandLine.Parameters(index = "0")
  lateinit var code: String
  
  override fun call(): Int = parent.catching {
    val app = app()
    val existUser = userDB.load()
    if (existUser != null) {
      println("Existing user in ${userProfile}, overwrite? [Y/N]")
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
class CommandMe() : Callable<Int> {
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
class CommandAnalyze() : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  @CommandLine.Option(
    names = ["--show-recent-unplayed"],
    description = ["Show recent unplayed beatmap in the 'not played' list of the report"],
  )
  var showRecentUnplayed: Boolean = false
  
  override fun call(): Int = parent.catching {
    parent.cliLogger.info("Run 'analyze' with: showRecentUnplayed=$showRecentUnplayed")
    
    val user = user()
    val app = app()
    val action = AnalyzeAction(app, user, scoreHistoryDB, mapDB, AnalyzeAction.Options(showRecentUnplayed))
    val analResult = action.analyze()
    val output = output
    
    if (output == null) {
      println(analResult)
    } else {
      // do not check parent directory, it's user's fault
      output.writeText(analResult)
    }
    
    return 0
  }
  
}

@CommandLine.Command(name = "rollback", description = ["Delete scores by last analyze"])
class CommandRollback() : Callable<Int> {
  @CommandLine.ParentCommand
  lateinit var parent: Main
  
  override fun call(): Int = parent.catching {
    val user = user()
    val app = app()
    val action = AnalyzeAction(app, user, scoreHistoryDB, mapDB)
    action.removeLastAnalyze()
    return 0
  }
}