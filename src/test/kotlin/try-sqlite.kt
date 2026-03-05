import com.github.hoshinotented.osuutils.api.data.BeatmapCheckSum
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import java.sql.Connection
import java.sql.DriverManager

fun Connection.queryAll(): ImmutableSeq<BeatmapCheckSum.Impl> {
  createStatement().use { query ->
    val result = query.executeQuery("SELECT * FROM BEATMAP")
    val buffer = FreezableMutableList.create<BeatmapCheckSum.Impl>()

    while (result.next()) {
      val id = result.getInt("id")
      val setId = result.getInt("set_id")
      val difficulty = result.getString("difficulty")
      val starRate = result.getFloat("star_rate")
      val checksum = result.getString("checksum")

      buffer.append(BeatmapCheckSum.Impl(id.toLong(), setId.toLong(), difficulty, starRate, checksum))
    }

    return buffer.toSeq()
  }
}

fun main() {
  DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
    connection.isReadOnly
    connection.autoCommit = false
    connection.createStatement().use { statement ->
      statement.executeUpdate("CREATE TABLE BEATMAP (id integer, set_id integer, difficulty string, star_rate float, checksum string)")
      connection.commit()
      statement.executeUpdate("INSERT INTO BEATMAP VALUES(114514, 1919810, '1611', 16.11, 'abcdefg')")
      println(connection.queryAll())
      connection.rollback()
      println(connection.queryAll())
      connection.commit()
      println(connection.queryAll())
    }
  }
}