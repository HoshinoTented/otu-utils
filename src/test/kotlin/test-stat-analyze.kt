import com.github.hoshinotented.osuutils.api.prettyBeatmap
import com.github.hoshinotented.osuutils.api.prettyMods
import com.github.hoshinotented.osuutils.cli.action.StatAnalyze
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableList
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.measureTimedValue

class StatAnalyzeTest : OsuBaseTest() {
  @Test
  fun test() {
    val playerName = ImmutableSeq.of("Hoshino Tented")
    // 加入统计的谱面 id
    val consideredBeatmapIds = ImmutableSeq.of(
      1607327,
      923985,
      1714884,
      4344475,
      5024443,
      2321761,
      2395915,
      4750677,
    )

    val now = Clock.System.now()
    val beatmapByHash = localOsu.beatmaps.associateBy { it.md5Hash }

    val consideredBeatmaps = localScores.scoredBeatmaps
      .filter {
        val map = beatmapByHash.getOrNull(it.md5Hash) ?: return@filter false
        map.beatmapId in consideredBeatmapIds
      }

    val result = measureTimedValue {
      StatAnalyze.analyze(consideredBeatmaps)
    }

    val res = result.value
    if (res == null) {
      println("没有数据")
      return
    }

    println("一共有 ${res.beatmaps.size()} 张谱面参与统计")
    println("一共有 ${res.result.potentials.size} 天的游玩数据参与统计")
    println("统计耗时: ${result.duration}")

    println("天数水平统计(越大越强):")
    res.result.potentials
      // 因为是用 第 n + 1 天 前归组的, 所以这里反序遍历
      .reversedArray()
      .forEachIndexed { idx, pot ->
        val realIdx = res.dayArrange[idx]
        println("前 ${realIdx + 1} 天: $pot")
      }

    println("谱面难度修正(越大越难):")
    res.result.difficulties.forEachIndexed { idx, diff ->
      val modded = res.beatmaps[idx]
      val hash = modded.map.md5Hash
      val map = beatmapByHash.getOrNull(hash)

      val scoreCount = modded.map.scores.size()
      val mod = prettyMods(modded.mod)
      val mapDisplay = if (map != null) {
        val display = prettyBeatmap(map)
        "${map.beatmapId} - $display"
      } else {
        hash
      }

      println("$mapDisplay $mod($scoreCount): 难度修正为 $diff")
    }
  }
}