import com.github.hoshinotented.osuutils.api.data.BeatmapCheckSum
import com.github.hoshinotented.osuutils.database.BeatmapSqlite
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.SqlSessionFactoryBuilder

const val NS = "com.github.hoshinotented.osuutils.database.BeatmapMapper"

fun main() {
  val res = Resources.getResourceAsStream("mybatis-config.xml")
  val sessionFactory = SqlSessionFactoryBuilder().build(res)
  sessionFactory.openSession().use { session ->
    val db = BeatmapSqlite(session)
    db.initialize()
    db.save(BeatmapCheckSum.Impl(114514, 1919810, "1611's Insane", 16.11F, "123456"))
    var result = db.load(114514)
    println(result)
    db.save(BeatmapCheckSum.Impl(114514, 1919810, "727's Extra", 16.11F, "123456"))
    result = db.load(114514)
    println(result)
  }
}