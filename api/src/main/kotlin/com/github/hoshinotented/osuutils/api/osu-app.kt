package com.github.hoshinotented.osuutils.api

import com.github.hoshinotented.osuutils.api.category.Authentication
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.annotations.Contract
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class OsuApplication(
  @SerialName("client_id") val clientId: Int,
  @SerialName("client_secret") val clientSecret: String,
  @SerialName("redirect_uri") val redirectUri: String,
  @SerialName("local_osu_path") val localOsuPath: String?,
) {
  @Transient
  var dontRefreshToken = false

  @Transient
  var token: ClientToken? = null

  fun withToken(t: IToken): ApplicationRole {
    return ApplicationRole(this, t)
  }
}

data class ApplicationRole(val app: OsuApplication, val token: IToken)

/**
 * This is the only one mutable structure in this library, as token is very important, we can't lose it
 */
@ExperimentalTime
@Serializable
data class UserToken(
  var requestTime: Instant,
  var expiresIn: Int,
  override var accessToken: String,
  var refreshToken: String
) : IToken {
  val expiresTime: Instant
    get() {
      return requestTime.plus(expiresIn.seconds)
    }

  override fun refresh(application: OsuApplication) {
    with(Authentication) {
      application.refreshToken(this@UserToken)
    }
  }
}

data class ClientToken(var requestTime: Instant, var expiresIn: Int, override var accessToken: String) : IToken {
  override fun refresh(application: OsuApplication) {
    TODO()
  }
}

sealed interface IToken {
  val accessToken: String

  @Contract(mutates = "this")
  fun refresh(application: OsuApplication)
}