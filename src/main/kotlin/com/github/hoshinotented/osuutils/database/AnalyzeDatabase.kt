package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.data.AnalyzeMetadata
import com.github.hoshinotented.osuutils.data.AnalyzeRecord
import kala.collection.immutable.ImmutableSeq
import java.nio.file.Path

class AnalyzeDatabase(baseDir: Path) {
  val metadataCache = JsonCache(baseDir.resolve("analyze_metadata.json"), AnalyzeMetadata.serializer())
  
  fun load(): AnalyzeMetadata {
    return metadataCache.get() ?: AnalyzeMetadata(AnalyzeRecord.START - 1, ImmutableSeq.empty())
  }
  
  fun save(metadata: AnalyzeMetadata) {
    metadataCache.set(metadata)
  }
  
  inline fun update(block: (AnalyzeMetadata) -> AnalyzeMetadata) {
    val old = load()
    val ret = block(old)
    if (ret !== old) save(ret)
  }
}