import com.github.hoshinotented.osuutils.api.prettyBeatmap
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

    // 之前碰到过谱面没有 md5hash 的情况，不确定有没有问题
    // 最近 30 天有游玩记录的谱面
    val nearbyScoredMap = localScores.scoredBeatmaps
      .filter {
        val map = beatmapByHash.getOrNull(it.md5Hash) ?: return@filter false

        map.beatmapId in consideredBeatmapIds
      }
      .map { map ->
        val nearbyScores = map.scores.filter {
          val diff = now - it.createTime
          diff.inWholeDays < 30 && it.playerName in playerName
        }

        map.copy(scores = nearbyScores)
      }
      .filter { it.scores.isNotEmpty }

    // 最近 30 天有游玩记录的，数据充足(至少有 5 天的游玩记录)的谱面
    val scores = nearbyScoredMap.flatMapIndexed { mapIdx, map ->
      val playCountOnDay = IntArray(30)
      val accOnDay = DoubleArray(30)

      map.scores.map { score ->
        val playerIdx = (now - score.createTime).inWholeDays.toInt()
        playCountOnDay[playerIdx]++
        accOnDay[playerIdx] += (score.accuracy.toDouble() * 100)
      }

      val scores = MutableList.create<StatAnalyze.Score>()

      for (i in 0 until 30) {
        val pc = playCountOnDay[i]
        if (pc == 0) continue

        val avgAcc = accOnDay[i] / pc
        val score = StatAnalyze.Score(mapIdx, i, 0, avgAcc)
        scores.append(score)
      }

      // 跳过数据不足的谱面
      if (scores.size() < 5) ImmutableSeq.empty() else scores
    }

    val scoredBeatmaps = scores.map { it.beatmapIdx }.distinct()
    val scoredDay = scores.map { it.playerIdx }.distinct()

    // 重新排列谱面和天数: 消除不参与统计的谱面和不参与统计的天数
    val beatmapArrange = scoredBeatmaps.sorted()
    val playerArrange = scoredDay.sorted()

    println("一共有 ${scoredBeatmaps.size} 张谱面参与统计")
    println("一共有 ${scoredDay.size} 天的游玩数据参与统计")

    val rearrangedScores = scores.map {
      val beatmapIdx = beatmapArrange.indexOf(it.beatmapIdx)
      val playerIdx = playerArrange.indexOf(it.playerIdx)

      assert(beatmapIdx >= 0)
      assert(playerIdx >= 0)

      it.copy(beatmapIdx = beatmapIdx, playerIdx = playerIdx)
    }

    if (rearrangedScores.isEmpty()) {
      println("没有数据!")
      return
    }

    println("开始统计")

    val result = measureTimedValue {
      StatAnalyze(ImmutableSeq.from(rearrangedScores))
        .analyze()
    }

    println("统计完成, 耗时: ${result.duration}")

    val analyzeResult = result.value

    println("天数水平统计(越大越强):")
    analyzeResult.potentials
      // 因为是用 第 n + 1 天 前归组的, 所以这里反序遍历
      .reversedArray()
      .forEachIndexed { idx, pot ->
        val realIdx = playerArrange[idx]
        println("前 ${realIdx + 1} 天: $pot")
      }

    println("谱面难度修正(越大越难):")
    analyzeResult.difficulties.forEachIndexed { idx, diff ->
      val beatmapIdx = beatmapArrange[idx]
      val hash = nearbyScoredMap[beatmapIdx].md5Hash
      val map = beatmapByHash.getOrNull(hash)

      val mapDisplay = if (map != null) {
        val display = prettyBeatmap(map)
        "${map.beatmapId} - $display"
      } else {
        hash
      }

      println("$mapDisplay: 难度修正为 $diff")
    }
  }
}