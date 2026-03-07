package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.Score
import com.github.hoshinotented.osuutils.data.ScoreHistory
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import org.apache.ibatis.session.SqlSession

/**
 * Some QA
 * Q: 在切换数据来源（remote-only / remote+local）时是否会出现一致性问题（如过于远古的分数）
 * A: 并不会，在拉取分数的时候只会拉取 **上次分析时间** 之后的分数。（这是往数据库中添加新分数的唯一方式）
 *    在回滚之后重新分析才有可能拉取旧的本地分数，而这并不会有什么影响，
 *    因为回滚使得状态恢复到上次分析时的状态，这些 _旧_ 的分数看起来就像是新的一样。
 */
class ScoreSqlite(val session: SqlSession, val readOnly: Boolean) : IScoreHistoryDatabase {
  companion object {
    const val NS = "com.github.hoshinotented.osuutils.database.ScoreMapper"
  }

  override fun initialize() {
    session.update("$NS.createScoreTable")
    session.update("$NS.createScoreHistoryTable")
    session.update("$NS.createAnalyzeTable")
    session.commit()
  }

  override fun listHistories(): ImmutableSeq<ScoreHistory> {
    TODO("Not yet implemented")
  }

  fun readAnalyzeList(head: Int): ImmutableSeq<AnalyzeRecordInDb> {
    val list = FreezableMutableList.create<AnalyzeRecordInDb>()
    var ptr = head

    while (ptr != 0) {
      val result = session.selectOne<AnalyzeRecordInDb?>("$NS.loadAnalyze", ptr)
      // fuck you intellij, i don't like `?: if (...)`
      if (result == null) {
        if (ptr == head) {
          throw IllegalArgumentException("Analyze record with id=$head is not found")
        } else {
          throw IllegalStateException("Database is inconsistent, analyze record with id=$ptr is not found")
        }
      }

      ptr = result.prevAnalyzeId
      list.append(result)
    }

    list.reverse()
    return list.toSeq()
  }

  /**
   * @return ordered by [Score.createdAt]
   */
  fun loadScores(id: BeatmapId): ImmutableSeq<Score> {
    val scores = session.selectList<ScoreInDb>("$NS.loadScores", id)
    return ImmutableSeq.from(scores).map { it.toScore() }
  }

  fun loadHistory(data: ScoreHistoryInDb): ScoreHistory {
    val analyzeList = readAnalyzeList(data.activeAnalyzeId)
    val scores = loadScores(data.beatmapId)
    // [scores] is ordered

    // it is possible that [analyzeList] is empty, i.e. [data] has not been analyzed.
    if (analyzeList.isEmpty()) {
      // in this case, the valid part of [scores] is empty

    } else {
      // trying to rebuild [ScoreHistory.groups]
      val groups = IntArray(analyzeList.size())

    }
  }

  override fun loadHistory(id: BeatmapId): ScoreHistory? {
    val record = session.selectOne<ScoreHistoryInDb?>("$NS.loadHistory", id)
      ?: return null

    return loadHistory(record)
  }

  override fun saveHistory(history: ScoreHistory) {
    TODO("Not yet implemented")
  }
}