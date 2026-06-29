import com.github.hoshinotented.osuutils.dump.LocalBeatmap
import com.github.hoshinotented.osuutils.dump.LocalCollections
import com.github.hoshinotented.osuutils.dump.LocalScores
import com.github.hoshinotented.osuutils.dump.deser.parse
import com.github.hoshinotented.osuutils.dump.deser.parseLocalOsu
import com.github.hoshinotented.osuutils.util.ConsoleOsuParseListener
import com.google.common.io.LittleEndianDataInputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.test.Test
import kotlin.time.measureTimedValue

class ReadOsuTest {
  // path to osu! directory, must contains `scores.db`, `osu!.db` and `collection.db`
  val osuPath = Path(System.getenv("OSU_HOME"))
  
  @Test
  fun testOsu() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("osu!.db").inputStream())
    val value = measureTimedValue {
      parseLocalOsu(`in`, ConsoleOsuParseListener())
    }

    println("Cost ${value.duration}")
    val osu = value.value

    val what = osu.beatmaps.groupBy { it.stdGrade }
    val how = what.keys
    return
  }

  @Test
  fun find529() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("osu!.db").inputStream())
    val value = parseLocalOsu(`in`, ConsoleOsuParseListener())

    val maps = value.beatmaps.filter { it.beatmapId.toString().contains("529") }
    maps.forEach {
      if (it.totalTimeMilliseconds <= 241000 && it.starRate() >= 4.5) {
        val isRanked = it.rankedStatus
        println("${it.beatmapId}    ${it.title}    Length: ${it.totalTimeMilliseconds / 1000}s    SR: ${it.starRate()}    Ranked: ${isRanked}")
      }
    }
  }
  
  @Test
  fun testScores() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("scores.db").inputStream())
    val scores = parse(LocalScores::class, `in`, null)
    return
  }

  @Test
  fun testCollections() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("collection.db").inputStream())
    val collections = parse(LocalCollections::class, `in`, null)!!
    val osu = parseLocalOsu(
      LittleEndianDataInputStream(osuPath.resolve("osu!.db").inputStream()),
      ConsoleOsuParseListener()
    )
    return
  }
}