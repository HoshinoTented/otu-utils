package com.github.hoshinotented.osuutils.cli.action

import kala.collection.immutable.ImmutableSeq
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.QRDecomposition
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import kotlin.math.ln
import kotlin.math.sqrt

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
}