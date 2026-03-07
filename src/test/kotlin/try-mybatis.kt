import com.github.hoshinotented.osuutils.api.data.BeatmapCheckSum
import com.github.hoshinotented.osuutils.api.data.BeatmapSetListed
import com.github.hoshinotented.osuutils.database.BeatmapSqlite
import kala.collection.immutable.ImmutableSeq
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.SqlSessionFactoryBuilder

const val NS = "com.github.hoshinotented.osuutils.database.BeatmapMapper"

val set = BeatmapSetListed.Impl(
  1919810, "Test Set", "Test Set 1919810",
  ImmutableSeq.of(
    BeatmapCheckSum.Impl(114514, 1919810, "1611's Insane", 16.11F, "123456")
  )
)

fun main() {
  val res = Resources.getResourceAsStream("mybatis-config.xml")
  val sessionFactory = SqlSessionFactoryBuilder().build(res)
  sessionFactory.openSession().use { session ->
    val db = BeatmapSqlite(session)
    db.initialize()
    db.saveSet(set)
    db.save(BeatmapCheckSum.Impl(114515, 1919811, "1612's Insane", 16.12F, "123457"))
    var result = db.load(114514)
    println(result)
    db.save(result.copy(difficulty = "727's Extra"))
    result = db.load(114514)
    println(result)
    // prove 114515 exists
    result = db.load(114515)
    println(result)

    val setResult = db.loadSet(1919810)
    println(setResult)
  }
}