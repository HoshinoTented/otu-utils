@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.api.data

import com.github.hoshinotented.osuutils.api.Beatmaps.beatmapScores
import com.github.hoshinotented.osuutils.data.IBeatmap
import com.github.hoshinotented.osuutils.prettyBeatmap
import com.github.hoshinotented.osuutils.prettyMods
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.Seq
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.time.Instant

@Serializable
data class OsuUser(
  val id: UserId,
  @SerialName("username") val userName: String,
  @SerialName("beatmap_playcounts_count") val playcount: Int?,
)

@Serializable
data class ScoreUserAttribute(val pin: Pin) {
  @Serializable
  data class Pin(@SerialName("score_id") val scoreId: ScoreId)
}

/**
 * @param id the id of score, this value may invalid, such as local scores
 * @param currentUserAttribute set when requested from [beatmapScores]
 */
@Serializable
data class Score(
  val accuracy: Float,
  @SerialName("created_at") val createdAt: Instant,
  val id: ScoreId,
  val mods: ImmutableSeq<Mod>,
  @SerialName("user_id") val userId: UserId,
  // not null in recent scores or best score of beatmap, null in all score of beatmap
//  val beatmap: MyBeatmapCheckSum.Impl?,
  // not null in recent scores
//  @SerialName("beatmapset") val beatmapSet: MyBeatmapSet.Impl?,
  @SerialName("current_user_attributes") val currentUserAttribute: ScoreUserAttribute?,
) {
  object AccComparator : Comparator<Score> {
    override fun compare(l: Score, r: Score): Int {
      return l.accuracy.compareTo(r.accuracy)
    }
  }

  object CreateTimeComparator : Comparator<Score> {
    override fun compare(l: Score, r: Score): Int {
      return l.createdAt.compareTo(r.createdAt)
    }
  }

  companion object {
    fun rawAcc(accuracy: Float): String {
      return String.format("%2.2f", accuracy * 100)
    }

    fun prettyAcc(accuracy: Float): String {
      return String.format("%2.2f%%", accuracy * 100)
    }

    fun prettyDiff(bias: Float): String {
      return String.format("%+2.2f%%", bias * 100)
    }

    fun prettyScore(score: Int, accuracy: Float, mods: ImmutableSeq<Mod>, beatmap: IBeatmap): String {
      return buildString {
        append("$score ${prettyAcc(accuracy)} ")
        if (mods.isNotEmpty) {
          append("${prettyMods(mods)} ")
        }

        append("| ")

        append(prettyBeatmap(beatmap))
      }
    }
  }

  val isLocal: Boolean get() = id > 0
}

sealed interface BeatmapSet {
  val id: BeatmapSetId
  val title: String
  @SerialName("title_unicode")
  val titleUnicode: String

  @Serializable
  data class Impl(
    override val id: BeatmapSetId,
    override val title: String,
    @SerialName("title_unicode") override val titleUnicode: String
  ) : BeatmapSet
}

sealed interface BeatmapSetListed : BeatmapSet {
  val beatmaps: ImmutableSeq<out BeatmapCheckSum>

  fun downgrade(): BeatmapSet.Impl = BeatmapSet.Impl(
    id, title, titleUnicode
  )

  @Serializable
  data class Impl(
    override val id: BeatmapSetId,
    override val title: String,
    @SerialName("title_unicode") override val titleUnicode: String,
    override val beatmaps: ImmutableSeq<BeatmapCheckSum.Impl>
  ) : BeatmapSetListed
}

// TODO: some api returns Beatmap with "beatmapset", this can save time

sealed interface Beatmap {
  companion object {
    fun prettyDifficulty(difficulty: Float): String {
      return String.format("%.2f*", difficulty)
    }
  }

  // serial name on interface property doesn't work,
  // but we still place them here

  /**
   * The id of beatmap, note that the id may not unique, i.e. a local beatmap that has not been submitted.
   */
  val id: BeatmapId
  @SerialName("beatmapset_id")
  val setId: BeatmapSetId
  @SerialName("version")
  val difficulty: String
  @SerialName("difficulty_rating")
  val starRate: Float

  @Serializable
  data class Impl(
    override val id: BeatmapId,
    @SerialName("beatmapset_id") override val setId: BeatmapSetId,
    @SerialName("version") override val difficulty: String,
    @SerialName("difficulty_rating") override val starRate: Float,
  ) : Beatmap
}

sealed interface BeatmapCheckSum : Beatmap {
  val checksum: String

  fun upgrade(set: BeatmapSet.Impl): BeatmapExtended.Impl = BeatmapExtended.Impl(
    id, setId, difficulty, starRate, checksum, set
  )

  @Serializable
  data class Impl(
    override val id: BeatmapId,
    @SerialName("beatmapset_id") override val setId: BeatmapSetId,
    @SerialName("version") override val difficulty: String,
    @SerialName("difficulty_rating") override val starRate: Float,
    override val checksum: String,
  ) : BeatmapCheckSum {
    companion object {
      fun from(map: BeatmapCheckSum): Impl {
        if (map is Impl) return map
        return Impl(
          map.id, map.setId,
          map.difficulty,
          map.starRate, map.checksum
        )
      }
    }
  }
}

sealed interface BeatmapExtended : BeatmapCheckSum, IBeatmap {
  @SerialName("beatmapset")
  val beatmapSet: BeatmapSet

  override fun starRate(): Float = starRate
  override fun beatmapId(): BeatmapId = id
  override fun title(): String = beatmapSet.title
  override fun difficultyName(): String = difficulty
  override fun md5Hash(): String = checksum

  fun downgrade(): BeatmapCheckSum.Impl = BeatmapCheckSum.Impl.from(this)

  @Serializable
  data class Impl(
    override val id: BeatmapId,
    @SerialName("beatmapset_id") override val setId: BeatmapSetId,
    @SerialName("version") override val difficulty: String,
    @SerialName("difficulty_rating") override val starRate: Float,
    override val checksum: String,
    @SerialName("beatmapset") override val beatmapSet: BeatmapSet.Impl,
  ) : BeatmapExtended
}