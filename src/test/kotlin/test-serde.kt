import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KalaSerdeTest {
  val data = ImmutableSeq.of(114, 514, 1919, 810)
  val expected = "[114,514,1919,810]"
  
  @Test
  fun testSer() {
    val what = Json.encodeToString(SeqSerializer(Int.serializer()), data)
    assertEquals(expected, what)
  }
  
  @Test
  fun testDeser() {
    val what = Json.decodeFromString(SeqSerializer(Int.serializer()), expected)
    assertEquals(data, what)
  }
}