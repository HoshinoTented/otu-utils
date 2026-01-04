import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import java.sql.DriverManager

fun main() {
  DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
    conn.createStatement().use { stmt ->
      stmt.executeUpdate(
        """
        create table beatmaps (
          beatmap_id integer not null,
          beatmapset_id integer not null,
          difficulty real not null,
          version string not null,
          checksum string not null
        )
      """.trimIndent()
      )
    }
    
    conn.prepareStatement(
      """
      insert into beatmaps values(?, ?, ?, ?, ?)
    """.trimIndent()
    ).use { prepare ->
      prepare.setInt(1, 114514)
      prepare.setInt(2, 1919810)
      prepare.setFloat(3, 5.14F)
      prepare.setString(4, "Yaku Senpai's EX")
      prepare.setString(5, "some md5 sum")
      prepare.executeUpdate()
    }
    
    conn.createStatement().use { stmt ->
      val result = stmt.executeQuery("select * from beatmaps")
      val beatmapId = result.getInt(1)
      val setId = result.getInt(2)
      val difficulty = result.getFloat(3)
      val version = result.getString(4)
      val hash = result.getString(5)
      
      println(Beatmap(setId.toLong(), beatmapId.toLong(), difficulty, version, null, hash))
    }
  }
}