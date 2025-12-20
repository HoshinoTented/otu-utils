@file:OptIn(ExperimentalTime::class)

import com.github.hoshinotented.osuutils.api.BeatmapSets.beatmapSet
import com.github.hoshinotented.osuutils.api.Beatmaps.beatmap
import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.Beatmap
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapId
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSet
import com.github.hoshinotented.osuutils.api.endpoints.BeatmapSetId
import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.database.BeatmapDatabase
import com.github.hoshinotented.osuutils.io.DefaultFileIO
import com.github.hoshinotented.osuutils.io.FileIO
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.io.path.Path
import kotlin.time.ExperimentalTime

abstract class TestBase {
  companion object {
    const val RES_DIR = "src/test/resources"
    const val USER_DATA = "$RES_DIR/user_data.json"
    const val APP_DATA = "$RES_DIR/app.json"
    
    val beatmapDatabase = BeatmapDatabase(Path("$RES_DIR/beatmaps"), DefaultFileIO)
    lateinit var application: OsuApplication
    lateinit var json: Json
    lateinit var user: User
    var initialized: Boolean = false
    
    fun initialize() {
      if (initialized) return
      initialized = true
      
      json = Json {
        prettyPrint = true
        explicitNulls = false
      }
      
      application = json.decodeFromString<OsuApplication>(File(APP_DATA).readText())
      
      // initialize user
      val content = File(USER_DATA).readText()
      val obj = json.decodeFromString<JsonObject>(content)
      
      val authCode = obj["authCode"]?.jsonPrimitive?.content
      val user: User
      if (authCode != null) {
        println("authorization code is found, requesting token")
        with(OsuApi) {
          user = application.newUser(authCode)
        }
      } else {
        user = json.decodeFromJsonElement(obj)
      }
      
      println(user.player)
      
      this.user = user
    }
    
    fun post() {
      if (::user.isInitialized) {
        println("Saving user data")
        File(USER_DATA).writeText(json.encodeToString(user))
      }
    }
  }
  
  /**
   * @return null if [beatmapId] is not a valid beatmap id
   */
  fun beatmap(beatmapId: BeatmapId): Beatmap? {
    var beatmap = beatmapDatabase.loadMaybe(beatmapId)
    if (beatmap != null) return beatmap
    
    beatmap = application.beatmap(user, beatmapId)
    if (beatmap == null) return null
    
    // we don't save this beatmap, but instead the beatmap set
    val set = application.beatmapSet(user, beatmap.beatmapSetId)
      ?: throw AssertionError("What do you mean a map is not belongs to its beatmap set??")
    
    beatmapDatabase.saveSet(set)
    return beatmap
  }
  
  fun beatmapSet(beatmapSetId: BeatmapSetId): BeatmapSet? {
    var set = beatmapDatabase.loadSetMaybe(beatmapSetId)
    if (set != null) return set
    
    set = application.beatmapSet(user, beatmapSetId)
    if (set == null) return null
    
    beatmapDatabase.saveSet(set)
    return set
  }
}