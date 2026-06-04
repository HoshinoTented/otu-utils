import com.github.hoshinotented.osuutils.dump.LocalBeatmap
import com.github.hoshinotented.osuutils.dump.LocalCollections
import com.github.hoshinotented.osuutils.dump.LocalOsuParseListener
import com.github.hoshinotented.osuutils.dump.LocalScores
import com.github.hoshinotented.osuutils.dump.parse
import com.github.hoshinotented.osuutils.dump.parseLocalOsu
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
      parseLocalOsu(`in`, LocalOsuParseListener.Console())
    }

    println("Cost ${value.duration}")
    val osu = value.value
    return
  }

  @Test
  fun find529() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("osu!.db").inputStream())
    val value = parseLocalOsu(`in`, LocalOsuParseListener.Console())

    val maps = value.beatmaps.filter { it.beatmapId.toString().contains("529") }
    maps.forEach {
      if (it.totalTimeMilliseconds <= 241000 && it.starRate() >= 4.5) {
        val isRanked = LocalBeatmap.Companion.RankedStatus.values()[it.rankedStatus.toInt()]
        println("${it.beatmapId}    ${it.title}    Length: ${it.totalTimeMilliseconds / 1000}s    SR: ${it.starRate()}    Ranked: ${isRanked}")
      }
    }
  }
  
  @Test
  fun testScores() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("scores.db").inputStream())
    val scores = parse(LocalScores::class, `in`)
    return
  }

  @Test
  fun testCollections() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("collection.db").inputStream())
    val collections = parse(LocalCollections::class, `in`)!!
    val osu = parseLocalOsu(
      LittleEndianDataInputStream(osuPath.resolve("osu!.db").inputStream()),
      LocalOsuParseListener.Console()
    )
    return
  }
}