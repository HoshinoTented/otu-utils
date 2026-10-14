package com.github.hoshinotented.osuutils.cli.action

import com.github.hoshinotented.osuutils.dump.LocalScoredBeatmap
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableList
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.QRDecomposition
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import java.time.Instant
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/**
 * 对成绩进行分析:
 * * acc 为成绩，保留两位小数
 * * 收集多张谱面的成绩，通过成绩判断谱面难度
 * * 综合多张谱面的成绩计算综合水平
 *
 * 方案由 AI 制定，因为我根本不懂统计学
 *
 * @param scores [Score.beatmapIdx] 和 [Score.playerIdx] 唯一确定一个 [Score],
 *               并且 [0..max(beatmapIdx)] 都有对应的 Score, [0..max(playerIdx)] 也有对应的 Score.
 * @param error 误差
 * @param playerPrior 玩家水平先验, 越大先验越弱
 * @param diffPrior 谱面难度先验, 越大先验越弱
 */
class StatAnalyze(
  val scores: ImmutableSeq<Score>,
  val error: Double = 1.0,
  val playerPrior: Double = 1.0,
  val diffPrior: Double = 1.0
) {
  /**
   * @param accuracy in [0.0, 100.0]
   */
  data class Score(val beatmapIdx: Int, val playerIdx: Int, val scoreIdx: Int, val accuracy: Double)

  /**
   * @param potentials 预测的玩家水平
   * @param difficulties 预测的谱面难度
   */
  class Result(val potentials: DoubleArray, val difficulties: DoubleArray)

  /**
   * @param beatmaps 第 `i` 个元素是谱面的 md5hash, 难度为 [Result.difficulties] 中的第 `i` 个.
   */
  class RichResult(val result: Result, val beatmaps: ImmutableSeq<String>, val dayArrange: ImmutableSeq<Int>)

  fun logit(rawScore: Double): Double {
    val smooth = (rawScore + 0.005) / 100.01
    return ln(smooth / (1.0 - smooth))
  }

  fun analyze(): Result {
    val error2 = error * error
    val playerPrior2 = playerPrior * playerPrior
    val diffPrior2 = diffPrior * diffPrior

    val beatmapCount = scores.maxOf { it.beatmapIdx } + 1
    val playerCount = scores.maxOf { it.playerIdx } + 1
    val scoreMatrix = Array(playerCount) {
      DoubleArray(beatmapCount) {
        Double.NaN
      }
    }

    val sumScore = DescriptiveStatistics()
    scores.forEach {
      val logited = logit(it.accuracy)
      scoreMatrix[it.playerIdx][it.beatmapIdx] = logited
      sumScore.addValue(logited)
    }

    val globalAvg = sumScore.mean

    // 玩家潜力
    val potentials = DoubleArray(playerCount) { globalAvg }
    val difficulties = DoubleArray(beatmapCount) { 0.0 }

    // 正则化
    val lambdaPlayer = error2 / playerPrior2
    val lambdaDiff = error2 / diffPrior2

    // 用矩阵求解线性最小二乘, 我也不懂
    val cols = beatmapCount + playerCount
    val rows = scores.size() + cols
    val A = Array(rows) { DoubleArray(cols) }
    val y = DoubleArray(rows)

    scores.forEachIndexed { idx, score ->
      A[idx][score.playerIdx] = 1.0
      A[idx][playerCount + score.beatmapIdx] = -1.0
      y[idx] = scoreMatrix[score.playerIdx][score.beatmapIdx]
    }

    // now A[..scores.size()] is filled

    // 玩家水平先验
    val sqrtLambdaPlayer = sqrt(lambdaPlayer)
    for (i in 0 until playerCount) {
      val r = scores.size() + i
      A[r][i] = sqrtLambdaPlayer
      y[r] = sqrtLambdaPlayer * globalAvg
    }

    // 谱面难度先验中心化约束
    val sqrtLambdaDiff = sqrt(lambdaDiff)
    for (i in 0 until beatmapCount) {
      val r = scores.size() + playerCount + i
      val j = playerCount + i
      A[r][j] = sqrtLambdaDiff
      y[r] = 0.0
    }

    // 求解最小二乘问题
    val solution = QRDecomposition(Array2DRowRealMatrix(A, false))
      .solver.solve(ArrayRealVector(y, false))

    // solution.size == cols
    for (i in 0 until playerCount) {
      potentials[i] = solution.getEntry(i)
    }

    for (i in 0 until beatmapCount) {
      difficulties[i] = solution.getEntry(playerCount + i)
    }

    // 中心化谱面难度
    val diffMean = difficulties.sum() / beatmapCount
    potentials.indices.forEach { i ->
      potentials[i] -= diffMean
    }

    difficulties.indices.forEach { i ->
      difficulties[i] -= diffMean
    }

    return Result(
      potentials,
      difficulties
    )
  }

  companion object {
    /**
     * @param scores 参与统计的谱面及其分数, 调用前应该先清除非玩家游玩的成绩
     * @param maxDays 统计过去多少天的游玩成绩 (按自然日划分, 00:00 为一天的分界)
     * @param leastPC 一张谱面至少在统计天数中有多少天有游玩记录才视为有效
     * @param meanOrMax 在处理一天内有多个 pc 时采用什么策略, true 则取平均, 否则取最大
     *
     * @return null 如果没有任何成绩参与统计, [RichResult.result] 的 potentials 第 `i` 个元素
     *              对应参与统计的第 `i` 天 (从近到远), beatmaps 第 `i` 个元素对应 difficulties 第 `i` 个
     */
    fun analyze(
      scores: ImmutableSeq<LocalScoredBeatmap>,
      maxDays: Int = 30,
      leastPC: Int = 5,
      meanOrMax: Boolean = false,
    ): RichResult? {
      if (maxDays <= 0) return null

      val now = Instant.now()
      val zone = ZoneId.systemDefault()
      val today = now.atZone(zone).toLocalDate()

      // 过滤最近 maxDays 天内的成绩, 并按天聚合成每日成绩
      val dayScoresByMap = scores.map { map ->
        val playCountOnDay = IntArray(maxDays)
        val sumAccOnDay = DoubleArray(maxDays)
        val maxAccOnDay = DoubleArray(maxDays) { Double.NEGATIVE_INFINITY }

        map.scores.forEach { score ->
          val dayIdx = ChronoUnit.DAYS.between(
            score.createTime.toJavaInstant().atZone(zone).toLocalDate(),
            today
          ).toInt()
          if (dayIdx !in 0 until maxDays) return@forEach

          val acc = score.accuracy.toDouble() * 100
          playCountOnDay[dayIdx]++
          sumAccOnDay[dayIdx] += acc
          if (acc > maxAccOnDay[dayIdx]) maxAccOnDay[dayIdx] = acc
        }

        val dayScores = MutableList.create<Score>()

        for (dayIdx in 0 until maxDays) {
          val pc = playCountOnDay[dayIdx]
          if (pc == 0) continue

          val acc = if (meanOrMax) sumAccOnDay[dayIdx] / pc else maxAccOnDay[dayIdx]
          dayScores.append(Score(0, dayIdx, 0, acc))
        }

        map to dayScores
      }

      // 跳过数据不足的谱面
      val validMaps = dayScoresByMap
        .filter { (_, dayScores) ->
          dayScores.isNotEmpty && dayScores.size() >= leastPC
        }

      if (validMaps.isEmpty()) return null

      // 重新排列谱面和天数: 消除不参与统计的天数
      val usedDays = validMaps
        .flatMap { (_, dayScores) -> dayScores.map { it.playerIdx } }
        .distinct()
        .sorted()

      val arrangedScores = validMaps.flatMapIndexed { beatmapIdx, (_, dayScores) ->
        dayScores.map {
          it.copy(beatmapIdx = beatmapIdx, playerIdx = usedDays.indexOf(it.playerIdx))
        }
      }

      val result = StatAnalyze(ImmutableSeq.from(arrangedScores)).analyze()

      return RichResult(result, validMaps.map { it.first.md5Hash }, usedDays)
    }
  }
}