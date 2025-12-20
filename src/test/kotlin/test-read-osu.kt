import com.github.hoshinotented.osuutils.osudb.LocalOsu
import com.github.hoshinotented.osuutils.osudb.parse
import com.google.common.io.LittleEndianDataInputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.reflect.full.createType
import kotlin.test.Test

class ReadOsuTest {
  // path to osu! directory, must contains `scores.db`, `osu!.db` and `collection.db`
  val osuPath = Path(System.getenv("OSU_HOME"))
  
  @Test
  fun test() {
    val `in` = LittleEndianDataInputStream(osuPath.resolve("osu!.db").inputStream())
    val osu = parse(LocalOsu::class.createType(), `in`)
    return
  }
}