@file:OptIn(ExperimentalTime::class)

import com.github.hoshinotented.osuutils.api.*
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.Type
import com.github.hoshinotented.osuutils.api.endpoints.Users
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kala.collection.immutable.ImmutableSeq
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.io.File
import kotlin.test.Test
import kotlin.time.ExperimentalTime

open class MyTest {
  @Serializable
  data class TrackBeatmap(val id: BeatmapId, val comment: String?)
  
  companion object {
    const val RES_DIR = "src/test/resources"
    const val USER_DATA = "$RES_DIR/user_data.json"
    const val APP_DATA = "$RES_DIR/app.json"
    const val TRACKING_BEATMAPS_DATA = "$RES_DIR/tracking_beatmaps.json"
    const val BEATMAP_HISTORY_DIR = "$RES_DIR/beatmap_histories"
    
    lateinit var application: OsuApplication
    lateinit var Json: Json
    lateinit var user: User
    lateinit var trackingBeatmaps: ImmutableSeq<TrackBeatmap>
    
    @JvmStatic
    @BeforeAll
    fun initialize() {
      Json = Json {
        prettyPrint = true
        explicitNulls = false
      }
      
      application = Json.decodeFromString<OsuApplication>(File(APP_DATA).readText())
      
      // initialize user
      val content = File(USER_DATA).readText()
      val obj = Json.decodeFromString<JsonObject>(content)
      
      val authCode = obj["authCode"]?.jsonPrimitive?.content
      val user: User
      if (authCode != null) {
        println("authorization code is found, requesting token")
        with(OsuApi) {
          user = application.newUser(authCode)
        }
      } else {
        user = Json.decodeFromJsonElement(obj)
      }
      
      println(user.player)
      
      this.user = user

//      trackingBeatmaps = Json.decodeFromString(
//        SeqSerializer(TrackBeatmap.serializer()),
//        File(TRACKING_BEATMAPS_DATA).readText()
//      )
    }
    
    @JvmStatic
    @AfterAll
    fun post() {
      if (::user.isInitialized) {
        println("Saving user data")
        File(USER_DATA).writeText(Json.encodeToString(user))
      }
    }
  }
  
  @Test
  fun what() {
    val req = processEndpointRequest(Users.Scores(114514, Type.Recent))
    val post = processEndpointRequest(OAuth2Endpoints.RefreshToken(114514, "114", "514"))
    return;
  }
  
  @Test
  fun testDeser() {
    val resp = """
      {
          "access_token": "verylongstring",
          "expires_in": 86400,
          "refresh_token": "anotherlongstring",
          "token_type": "Bearer"
      }
    """.trimIndent()
    
    val obj = Json.decodeFromString<OAuth2Endpoints.Response>(resp)
    println(obj)
  }
  
  @Test
  fun recentScores() {
    with(com.github.hoshinotented.osuutils.api.Users) {
      val scores = application.recentScores(user, 10, 0)
      scores.forEach(::println)
    }
  }
  
  @Test
  fun bestScore() {
    with(Beatmaps) {
      // Fuccho's Another 谱师：melonboy
      // 時計の部屋と精神世界
      val score = application.bestScore(user, 1090074)
      println(score)
    }
  }
  
  @Test
  fun getBeatmapSet() {
    with(BeatmapSets) {
      val set = application.beatmapSet(user, 508947)
      println(set)
    }
  }
}
