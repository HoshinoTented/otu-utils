import com.github.hoshinotented.osuutils.api.BeatmapSets.beatmapSet
import com.github.hoshinotented.osuutils.cli.Main
import com.github.hoshinotented.osuutils.util.AccumulateProgressIndicator
import com.github.hoshinotented.osuutils.util.ProgressIndicator
import java.io.File

object DbMigrate {
  // Force update all beatmap data in database, please backup yor database before use
  @JvmStatic
  fun main(args: Array<String>) {
    val main = Main().apply {
      profile = File("./src/test/resources")
      initLogger()
    }
    
    val app = main.app()
    val user = main.user()
    val mapDb = main.mapDB
    val sets = mapDb.listSets()
    val pi = AccumulateProgressIndicator(ProgressIndicator.Console)
    pi.init(sets.size(), "Fetch Beatmap Set Data", null)
    
    sets.forEach {
      val set = app.beatmapSet(user, it)
      if (set == null) {
        pi.progress("Beatmap set $it is not found, skip")
      } else {
        mapDb.saveSet(set)
        pi.progress("$it ${set.titleUnicode}")
      }
    }
  }
}