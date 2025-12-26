package com.github.hoshinotented.osuutils.api

import com.github.hoshinotented.osuutils.api.OsuApi.deJson
import com.github.hoshinotented.osuutils.api.OsuApi.sendAuthedRequest
import com.github.hoshinotented.osuutils.api.Users.me
import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSetId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSets
import com.github.hoshinotented.osuutils.api.endpoints.Beatmaps
import com.github.hoshinotented.osuutils.api.endpoints.Mode
import com.github.hoshinotented.osuutils.api.endpoints.OAuth2Endpoints
import com.github.hoshinotented.osuutils.api.endpoints.OsuUser
import com.github.hoshinotented.osuutils.api.endpoints.Type
import com.github.hoshinotented.osuutils.api.endpoints.Users
import com.github.hoshinotented.osuutils.api.endpoints.Score
import com.github.hoshinotented.osuutils.api.endpoints.ScoreId
import com.github.hoshinotented.osuutils.data.Token
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Contract
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.logging.Logger
import kotlin.time.Clock

@Serializable
data class OsuApplication(
  @SerialName("client_id") val clientId: Int,
  @SerialName("client_secret") val clientSecret: String,
  @SerialName("redirect_uri") val redirectUri: String,
  @SerialName("local_osu_path") val localOsuPath: String?,
) {
  @Transient
  var dontRefreshToken = false
}

object OsuApi {
  const val BASE_URL = "https://osu.ppy.sh"
  const val API_V2 = "$BASE_URL/api/v2/"
  
  val logger = Logger.getLogger("OsuAPI")
  
  val deJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    explicitNulls = false
  }
  
  val client: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .build()
  
  /// region OAuth
  
  /**
   * @return null if failed to refresh token
   * @throws HttpException when status code is not 200
   */
  @Contract(mutates = "param1")
  fun OsuApplication.refreshToken(token: Token): Token {
    logger.info("token is expired, refreshing...")
    if (dontRefreshToken) {
      throw IllegalStateException("Don't refresh token")
    }
    
    val req = OAuth2Endpoints.RefreshToken(clientId, clientSecret, token.refreshToken)
      .toRequest()
      .build()
    
    val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
      .successOrThrow()
    
    val createTime = Clock.System.now()
    val respObj = deJson.decodeFromString<OAuth2Endpoints.Response>(resp)
    
    token.requestTime = createTime
    token.refreshToken = respObj.refreshToken
    token.accessToken = respObj.accessToken
    token.expiresIn = respObj.expiresIn
    
    return token
  }
  
  /**
   * @throws HttpException
   */
  fun OsuApplication.exchangeToken(code: String): Token {
    val req = OAuth2Endpoints.AccessToken(clientId, clientSecret, code, redirectUri)
      .toRequest()
      .build()
    
    val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
      .successOrThrow()
    
    val createTime = Clock.System.now()
    val respObj = deJson.decodeFromString<OAuth2Endpoints.Response>(resp)
    
    return Token(createTime, respObj.expiresIn, respObj.accessToken, respObj.refreshToken)
  }
  
  /// endregion OAuth
  
  /**
   * Construct a user with given authorization code
   */
  fun OsuApplication.newUser(code: String): User {
    val token = exchangeToken(code)
    return me(token)
  }
  
  /**
   * 发送一条需要 auth 的请求，如果 token 过期，那么会重新兑换 token 并且再请求一次
   * @return response and token (may be fresh)
   * @throws HttpException
   */
  fun OsuApplication.sendAuthedRequest(token: Token, req: HttpRequest.Builder): HttpResponse<String> {
    val token = token
    var resp = client.send(
      req.oauth(token).build(),
      HttpResponse.BodyHandlers.ofString()
    )
    
    if (resp.statusCode() == 401) {
      // try refresh
      refreshToken(token)
      resp = client.send(
        req.oauth(token).build(),
        HttpResponse.BodyHandlers.ofString()
      )
    }
    
    return resp
  }
}

object Users {
  /**
   * @return the user, may with new token
   */
  fun OsuApplication.me(token: Token): User {
    val resp = sendAuthedRequest(token, Users.Me().toRequest())
    val json = resp.successOrThrow()
    val user = deJson.decodeFromString<OsuUser>(json)
    
    return User(token, user)
  }
  
  // Looks like limit is up to 40
  // ^ no, the server only save 40 recent play score
  fun OsuApplication.recentScores(user: User, limit: Int, offset: Int = 0): ImmutableSeq<Score> {
    val reqObj = Users.Scores(
      user.player.id,
      Type.Recent,
      legacyOnly = true, includeFails = false,
      mode = Mode.Osu,
      limit = limit,
      offset = offset
    )
    
    val resp = sendAuthedRequest(user.token, reqObj.toRequest())
      .successOrThrow()
    
    return deJson.decodeFromString(SeqSerializer(Score.serializer()), resp)
  }
}

object Beatmaps {
  fun makeBeatmapUrl(beatmapId: BeatmapId): String {
    return "${OsuApi.BASE_URL}/b/$beatmapId"
  }
  
  /**
   * @param scoreId note that this is not [Score.id], but [com.github.hoshinotented.osuutils.api.endpoints.ScoreUserAttribute.Pin.scoreId]
   */
  fun makeScoreUrl(scoreId: ScoreId): String {
    return "${OsuApi.BASE_URL}/scores/$scoreId"
  }
  
  fun OsuApplication.bestScore(user: User, beatmapId: BeatmapId): Score? {
    val reqObj = Beatmaps.UserScore(beatmapId, user.player.id, legacyOnly = true)
    // it is possible that the response is 404, in this case, the user have no score of beatmap with id [beatmapId]
    val json = sendAuthedRequest(user.token, reqObj.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null
    val beatmapUserScore = deJson.decodeFromString<Beatmaps.UserScore.Response>(json)
    return beatmapUserScore.score
  }
  
  fun OsuApplication.beatmapScores(user: User, beatmapId: BeatmapId): ImmutableSeq<Score>? {
    val reqObj = Beatmaps.UserScoreAll(beatmapId, user.player.id, legacyOnly = true)
    val resp = sendAuthedRequest(user.token, reqObj.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null
    val scores = deJson.decodeFromString<Beatmaps.UserScoreAll.Response>(resp)
    // response score are unsorted
    return scores.scores.sorted(Score.CreateTimeComparator)
  }
  
  fun OsuApplication.beatmap(user: User, beatmapId: BeatmapId): Beatmap? {
    val req = Beatmaps.Beatmap(beatmapId)
    val resp = sendAuthedRequest(user.token, req.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null
    
    return deJson.decodeFromString(resp)
  }
}

object BeatmapSets {
  fun OsuApplication.beatmapSet(user: User, beatmapSetId: BeatmapSetId): BeatmapSet? {
    val req = BeatmapSets.BeatmapSet(beatmapSetId)
    val resp = sendAuthedRequest(user.token, req.toRequest())
      .checkNotFound()
      ?.successOrThrow() ?: return null
    
    return deJson.decodeFromString(resp)
  }
}