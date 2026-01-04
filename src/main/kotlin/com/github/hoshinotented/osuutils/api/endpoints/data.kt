@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.api.endpoints

import com.github.hoshinotented.osuutils.data.IBeatmap
import com.github.hoshinotented.osuutils.osudb.LocalBeatmap
import com.github.hoshinotented.osuutils.prettyBeatmap
import com.github.hoshinotented.osuutils.prettyMods
import com.github.hoshinotented.osuutils.serde.SeqSerializer
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
 * @param id the id of score
 * @param currentUserAttribute set when requested from [com.github.hoshinotented.osuutils.api.Beatmaps.beatmapScores]
 */
@Serializable
data class Score(
  val accuracy: Float,
  @SerialName("created_at") val createdAt: Instant,
  val id: ScoreId,
  val mods: ImmutableSeq<Mod>,
  @SerialName("user_id") val userId: UserId,
  val beatmap: Beatmap?,
  @SerialName("beatmapset") val beatmapSet: BeatmapSet?,
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
    
    /**
     * @param beatmap TODO abstraction...
     */
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
}

@Serializable
data class BeatmapSet(
  val id: BeatmapSetId, val title: String,
  @SerialName("title_unicode") val titleUnicode: String,
  val beatmaps: ImmutableSeq<Beatmap>? = null,
) {
  companion object {
    val DUMMY = BeatmapSet(0, "", "")
  }
}

// TODO: some api returns Beatmap with "beatmapset", this can save time
/**
 * For [IBeatmap], the caller must ensure that [beatmapSet] and [checksum] is set...
 */
@Serializable
data class Beatmap(
  @SerialName("beatmapset_id") val beatmapSetId: Long,
  val id: Long,
  @SerialName("difficulty_rating") val difficulty: Float,
  val version: String,
  @SerialName("beatmapset") val beatmapSet: BeatmapSet?,
  val checksum: String?,
) : IBeatmap {
  override fun beatmapId(): BeatmapId {
    return id
  }
  
  override fun title(): String {
    return beatmapSet!!.titleUnicode
  }
  
  override fun difficultyName(): String {
    return version
  }
  
  override fun starRate(): Float {
    return difficulty
  }
  
  override fun md5Hash(): String {
    return checksum!!
  }
  
  companion object {
    fun prettyDifficulty(difficulty: Float): String {
      return String.format("%.2f*", difficulty)
    }
  }
}