import MyTest.Companion.RES_DIR
import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSetId
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

// TODO: when we use a really database?
class BeatmapDatabase {
  companion object {
    val DIR = Path("$RES_DIR/beatmaps")
    val METADATA: Path = DIR.resolve("metadata.json")
    
    val json = Json {
      prettyPrint = true
      explicitNulls = false
    }
    
    fun init() {
      if (!DIR.exists()) {
        if (!DIR.toFile().mkdirs()) throw IOException("Unable to create directory $DIR")
      }
    }
  }
  
  @Serializable
  data class ThinBeatmapSet(
    val id: BeatmapSetId, val title: String,
    @SerialName("title_unicode") val titleUnicode: String,
    @Serializable(with = SeqSerializer::class) val beatmaps: ImmutableSeq<BeatmapId>,
  )
  
  class Metadata(val map: MutableMap<BeatmapId, BeatmapSetId>) : AutoCloseable {
    override fun close() {
      METADATA.writeText(json.encodeToString(map))
    }
  }
  
  private lateinit var metadataCache: Metadata
  
  private fun metadata(): Metadata {
    if (::metadataCache.isInitialized) return metadataCache
    if (METADATA.exists()) {
      val map = json.decodeFromString<MutableMap<BeatmapId, Long>>(METADATA.readText())
      return Metadata(map)
    } else {
      return Metadata(mutableMapOf())
    }
  }
  
  fun load(beatmapId: BeatmapId): Beatmap? {
    val setId = metadata().use {
      if (it.map.containsKey(beatmapId)) {
        it.map.getValue(beatmapId)
      } else return null
    }
    
    val path = DIR.resolve("$setId").resolve("$beatmapId.json")
    return if (path.exists()) {
      json.decodeFromString(path.readText())
    } else null
  }
  
  fun save(map: Beatmap) {
    val setPath = DIR.resolve("${map.beatmapSetId}")
    if (!setPath.exists()) {
      if (!setPath.toFile().mkdirs()) throw IOException("Unable to create directories: $setPath")
    }
    
    setPath.resolve("${map.id}.json")
      .writeText(json.encodeToString(map))
  }
  
  fun loadSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    val path = DIR.resolve("$beatmapSetId.json")
    if (!path.exists()) return null
    
    val thin = json.decodeFromString<ThinBeatmapSet>(path.readText())
    val beatmaps = thin.beatmaps.map {
      load(it) ?: throw IllegalStateException("Beatmap $it not found, database may be corrupted.")
    }
    
    return BeatmapSet(thin.id, thin.title, thin.titleUnicode, beatmaps)
  }
  
  fun saveSet(set: BeatmapSet) {
    val beatmaps = set.beatmaps ?: throw IllegalArgumentException("Cannot save beatmap set with no beatmaps")
    val thin = ThinBeatmapSet(set.id, set.title, set.titleUnicode, set.beatmaps.map { it.id })
    
    DIR.resolve("${set.id}.json").writeText(json.encodeToString(thin))
    
    // save beatmaps
    beatmaps.forEach { save(it) }
    
    // update metadata
    metadata().use { metadata ->
      beatmaps.forEach { map ->
        metadata.map[map.id] = set.id
      }
    }
  }
}