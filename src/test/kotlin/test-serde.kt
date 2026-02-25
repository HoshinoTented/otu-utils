import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.endpoints.MyBeatmap
import com.github.hoshinotented.osuutils.api.endpoints.MyBeatmapCheckSum
import com.github.hoshinotented.osuutils.api.endpoints.MyBeatmapExtended
import com.github.hoshinotented.osuutils.commonSerde
import com.github.hoshinotented.osuutils.data.BeatmapCollection
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class SerdeTest {
  @Test
  fun testTransform() {
    @Language("JSON") val json = """
      {
        "name": "Test",
        "author": "IDK",
        "beatmaps": [
          114514,
          {
            "id": 1919810, "tag": "NM1"
          }
        ]
      }
    """.trimIndent()

    val collection = commonSerde.decodeFromString<BeatmapCollection>(json)
    assertEquals(114514, collection.beatmaps[0].id)
    assertEquals(1919810, collection.beatmaps[1].id)
  }

  @Test
  fun testPoly() {
    @Language("JSON") val json = """
      {
        "id": 114514,
        "beatmapset_id": 1919810,
        "version": "1611's Insane",
        "difficulty_rating": 16.11,
        "checksum": "1145141919810",
        "beatmapset": {
          "id": 1919810,
          "title": "Back to Marie",
          "title_unicode": "Back to Marie"
        }
      }
    """.trimIndent()

    OsuApi.deJson.decodeFromString<MyBeatmap.Impl>(json)
    OsuApi.deJson.decodeFromString<MyBeatmapCheckSum.Impl>(json)
    OsuApi.deJson.decodeFromString<MyBeatmapExtended.Impl>(json)
  }
}