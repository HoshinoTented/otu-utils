import com.github.hoshinotented.osuutils.dump.LocalOsu
import com.github.hoshinotented.osuutils.dump.LocalScores
import com.github.hoshinotented.osuutils.dump.deser.Deserializers
import com.github.hoshinotented.osuutils.dump.deser.parseLocalOsu
import com.github.hoshinotented.osuutils.util.ConsoleOsuParseListener
import com.google.common.io.LittleEndianDataInputStream
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

abstract class OsuBaseTest {
  private val osuHomeEnv = System.getenv("OSU_HOME")!!
  val osuHome = Path(osuHomeEnv)
  val localOsu: LocalOsu
  val localScores: LocalScores

  init {
    if (! osuHome.exists()) {
      throw IllegalArgumentException("$osuHome does not exist")
    }

    val osudb = osuHome.resolve("osu!.db")
    LittleEndianDataInputStream(osudb.inputStream()).use {
      localOsu = parseLocalOsu(it, ConsoleOsuParseListener())
    }

    val scores = osuHome.resolve("scores.db")
    LittleEndianDataInputStream(scores.inputStream()).use {
      localScores = Deserializers.read(LocalScores::class, it)
    }
  }
}