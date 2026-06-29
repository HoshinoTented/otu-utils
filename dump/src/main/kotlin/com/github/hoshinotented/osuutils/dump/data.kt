// https://github.com/ppy/osu/wiki/Legacy-database-file-structure
// we assume the database comes from newest version, thus version related format problem is gone

package com.github.hoshinotented.osuutils.dump

import kala.collection.immutable.ImmutableSeq

data class LocalScoredBeatmap(val md5Hash: String, val scores: ImmutableSeq<LocalScore>)

data class LocalScores(
  val version: Int,
  val scoredBeatmaps: ImmutableSeq<LocalScoredBeatmap>,
)

data class LocalCollection(val name: String, val beatmaps: ImmutableSeq<String>)

data class LocalCollections(
  val version: Int,
  val collections: ImmutableSeq<LocalCollection>
)