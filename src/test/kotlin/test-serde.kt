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
}