package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.api.data.BeatmapCheckSum
import com.github.hoshinotented.osuutils.api.data.BeatmapId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetId
import com.github.hoshinotented.osuutils.api.data.BeatmapSetListed
import org.apache.ibatis.session.SqlSession

class BeatmapSqlite(val session: SqlSession, val readOnly: Boolean = false) : IBeatmapDatabase {
  companion object {
    const val NS = "com.github.hoshinotented.osuutils.database.BeatmapMapper"
  }

  override fun initialize() {
    session.update("$NS.createBeatmapTable")
  }

  override fun loadMaybe(id: BeatmapId): BeatmapCheckSum.Impl? {
    return session.selectOne<BeatmapInDb?>("$NS.loadBeatmap", id)?.toBeatmap()
  }

  override fun loadSetMaybe(id: BeatmapSetId): BeatmapSetListed? {
    TODO("Not yet implemented")
  }

  override fun save(map: BeatmapCheckSum) {
    if (readOnly) return
    session.insert("$NS.saveBeatmap", BeatmapInDb.from(map))
  }

  override fun saveSet(set: BeatmapSetListed) {
    TODO("Not yet implemented")
  }
}