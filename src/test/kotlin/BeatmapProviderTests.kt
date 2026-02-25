import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetId
import com.github.hoshinotented.osuutils.api.data.MyBeatmapCheckSum
import com.github.hoshinotented.osuutils.api.data.MyBeatmapExtended
import com.github.hoshinotented.osuutils.api.data.MyBeatmapSetListed
import com.github.hoshinotented.osuutils.data.BeatmapInCollection
import com.github.hoshinotented.osuutils.data.BeatmapInfoCache
import com.github.hoshinotented.osuutils.providers.BeatmapCollectionBeatmapProvider
import com.github.hoshinotented.osuutils.providers.BeatmapProvider
import kala.collection.immutable.ImmutableMap
import kala.collection.immutable.ImmutableSeq
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class BeatmapProviderTests {
  class TestBeatmapProvider(
    val beatmapSets: ImmutableSeq<MyBeatmapSetListed.Impl>
  ) : BeatmapProvider {
    private val byBeatmapId: ImmutableMap<BeatmapId, MyBeatmapCheckSum.Impl?> = beatmapSets.view()
      .flatMap { it.beatmaps }
      .associateBy { it.id }

    private val bySetId: ImmutableMap<BeatmapSetId, MyBeatmapSetListed.Impl> = beatmapSets.view()
      .associateBy { it.id }

    override fun beatmap(beatmapId: BeatmapId): MyBeatmapExtended? {
      val map = byBeatmapId[beatmapId] ?: return null
      return MyBeatmapExtended.Impl(
        map.id,
        map.setId,
        map.difficulty,
        map.starRate,
        map.checksum,
        bySetId[map.setId]!!.downgrade()
      )
    }

    override fun beatmapSet(beatmapSetId: BeatmapSetId): MyBeatmapSetListed? {
      return beatmapSets.find { it.id == beatmapSetId }.orNull
    }
  }

  companion object {
    fun beatmapSetOf(id: BeatmapId): MyBeatmapSetListed.Impl = MyBeatmapSetListed.Impl(
      id, "Beatmap $id", "Beatmap $id in unicode",
      ImmutableSeq.of(MyBeatmapCheckSum.Impl(id, id, "$id's Insane", 16.11F, "$id"))
    )

    val BEATMAP_SETS: ImmutableSeq<MyBeatmapSetListed.Impl> = ImmutableSeq.of(
      beatmapSetOf(114514),
      beatmapSetOf(1919810),
      beatmapSetOf(1611),
    )
  }

  @Test
  fun testCollectionProvider() {
    val beatmapInCollections = ImmutableSeq.of(
      BeatmapInCollection.of(114514)
        .copy(cache = BeatmapInfoCache.from(BEATMAP_SETS[0].beatmaps[0].upgrade(BEATMAP_SETS[0].downgrade()))),
      BeatmapInCollection.of(1919810)
    )

    val provider = BeatmapCollectionBeatmapProvider(
      beatmapInCollections,
      TestBeatmapProvider(BEATMAP_SETS),
      false
    )

    assertNotNull(provider.beatmap(114514))
    assertNotNull(provider.beatmap(1919810))
    assertNull(provider.beatmap(1611))

    assertSame(provider.collection.first { it.id == 114514L }.cache, beatmapInCollections[0].cache)
    assertNotNull(provider.collection.first { it.id == 1919810L }.cache)
  }
}