@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.time.Instant

/**
 * @param id the id of analyze, starts from 100
 */
@Serializable
data class AnalyzeRecord(val id: Int, val date: Instant) {
  companion object {
    const val START: Int = 100
  }
}

@Serializable
data class AnalyzeMetadata(val lastId: Int, val records: ImmutableSeq<AnalyzeRecord>) {
  fun addRecord(record: AnalyzeRecord): AnalyzeMetadata {
    if (lastId + 1 != record.id)
      throw IllegalArgumentException("id doesn't match, expected ${lastId + 1}, but got ${record.id}")
    
    return copy(lastId = record.id + 1, records = records.appended(record))
  }
}