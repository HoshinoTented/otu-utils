import com.github.hoshinotented.osuutils.osudb.LocalOsu
import com.github.hoshinotented.osuutils.osudb.LocalOsuParseListener
import com.github.hoshinotented.osuutils.osudb.LocalScores
import com.github.hoshinotented.osuutils.osudb.parse
import com.github.hoshinotented.osuutils.osudb.parseLocalOsu
import com.google.common.io.LittleEndianDataInputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.reflect.full.createType
import kotlin.test.Test

class ReadOsuTest {
  // path to osu! directory, must contains `scores.db`, `osu!.db` and `collection.db`
  val osuPath = Path(System.getenv("OSU_HOME"))
  
  @Test
  fun testOsu() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("osu!.db").inputStream())
    val osu = parseLocalOsu(`in`, LocalOsuParseListener.Console)
    return
  }
  
  @Test
  fun testScores() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("scores.db").inputStream())
    val scores = parse(LocalScores::class, `in`)
    return
  }
}