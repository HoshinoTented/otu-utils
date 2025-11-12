@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.api.endpoints

import com.github.hoshinotented.osuutils.api.Endpoint
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

object Users {
  /**
   * @param type 分数列表的类型
   * @param includeFails 是否包含失败的游戏
   */
  @Endpoint("users/{id}/scores/{type}", Endpoint.Method.Get)
  data class Scores(
    val id: UserId,
    val type: Type,
    val legacyOnly: Boolean = false,
    val includeFails: Boolean = false,
    val mode: Mode? = Mode.Osu,
    val limit: Int = 40,
    val offset: Int = 0,
  ) : EndpointRequest
  
  @Endpoint("me/{mode}")
  data class Me(val mode: Mode? = Mode.Osu) : EndpointRequest
}

object Beatmaps {
  @Endpoint("beatmaps/{beatmapId}/scores/users/{userId}")
  data class UserScore(
    val beatmapId: BeatmapId,
    val userId: UserId,
    val legacyOnly: Boolean = false,
    val mode: Mode? = Mode.Osu,
  ) : EndpointRequest {
    @Serializable
    data class Response(val position: Int, val score: Score)
  }
  
  // https://osu.ppy.sh/docs/index.html#get-a-user-beatmap-scores
  @Endpoint("beatmaps/{beatmapId}/scores/users/{userId}/all")
  data class UserScoreAll(
    val beatmapId: BeatmapId,
    val userId: UserId,
    val legacyOnly: Boolean = false,
  ) : EndpointRequest {
    @Serializable
    data class Response(val scores: ImmutableSeq<Score>)
  }
  
  @Endpoint("beatmaps/{id}")
  data class Beatmap(val id: BeatmapId) : EndpointRequest
}

object BeatmapSets {
  @Endpoint("beatmapsets/{id}")
  data class BeatmapSet(val id: BeatmapSetId) : EndpointRequest
}