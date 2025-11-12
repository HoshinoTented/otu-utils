import com.github.hoshinotented.osuutils.ScoreAnalyzer
import com.github.hoshinotented.osuutils.api.Beatmaps
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.data.ScoreHistory
import com.github.hoshinotented.osuutils.initializeScoreHistory
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.BeforeAll
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class AnalyzeTest : TestBase() {
  companion object {
    @Serializable
    data class TrackBeatmap(val id: BeatmapId, val comment: String?)
    
    const val TRACKING_BEATMAPS_DATA = "$RES_DIR/tracking_beatmaps.json"
    const val BEATMAP_HISTORY_DIR = "$RES_DIR/beatmap_histories"
    
    lateinit var trackingBeatmaps: ImmutableSeq<TrackBeatmap>
    
    @BeforeAll
    @JvmStatic
    fun beforeAll() {
      initialize()
      
      trackingBeatmaps = json.decodeFromString(
        SeqSerializer(TrackBeatmap.serializer()),
        File(TRACKING_BEATMAPS_DATA).readText()
      )
    }
  }
  
  fun readScoreHistory(beatmapId: BeatmapId): ScoreHistory? {
    val path = Path(BEATMAP_HISTORY_DIR).resolve("$beatmapId.json")
    if (!path.exists()) return null
    
    return json.decodeFromString(path.readText())
  }
  
  fun ScoreHistory.save() {
    val dir = Path(BEATMAP_HISTORY_DIR)
    if (!dir.exists()) {
      if (!dir.toFile().mkdirs()) throw IOException("Unable to create directories: $dir")
    }
    
    val file = dir.resolve("$beatmapId.json")
    file.writeText(json.encodeToString(this))
  }
  
  fun histories(): ImmutableSeq<ScoreHistory> {
    return trackingBeatmaps.map {
      val id = it.id
      val history = readScoreHistory(id) ?: application.initializeScoreHistory(user, id)
      history
    }
  }
  
  @Test
  fun analyze() {
    val histories = histories()
    
    // the limit can be calculated by the difference of play count
    val analyzer = ScoreAnalyzer(application, user, histories, 1000)
    val now = Clock.System.now()
    val reports = analyzer.analyze(now, now - 30.days)
    reports.forEachIndexed { idx, report ->
      report.history.save()
      
      val track = trackingBeatmaps.get(idx)
      val oldHistory = histories.get(idx)
      val comment = track.comment
      
      val map = beatmap(report.history.beatmapId)!!
      val mapSet = beatmapSet(map.beatmapSetId)!!
      
      println(
        "Report of beatmap: [${mapSet.titleUnicode} / ${map.version} / ${map.difficulty}*](${
          Beatmaps.makeBeatmapUrl(
            map.id
          )
        })"
      )
      if (comment != null) {
        println("> $comment")
        println()
      }
      
      println("Recent play count: ${report.playCount}")
      // We can use new history, as the best score may be updated
      println(report.report?.pretty(oldHistory.best))
    }
  }
  
  @Test
  fun removeLastAnalyze() {
    val histories = histories()
    histories.forEach {
      val beatmap = beatmap(it.beatmapId)!!
      val set = beatmapSet(beatmap.beatmapSetId)!!
      println("Removing the last group of ${set.titleUnicode} / ${beatmap.version}")
      if (it.groups.isNotEmpty()) {
        val index = it.groups.last()
        val newHistory = it.copy(scores = it.scores.slice(0, index), groups = it.groups.copyOf(it.groups.size - 1))
        newHistory.save()
        val firstScoreInGroup = it.scores.get(index)
        println("Removed all scores since " + ScoreAnalyzer.Report.prettyTime(firstScoreInGroup.createdAt))
      }
    }
  }
}