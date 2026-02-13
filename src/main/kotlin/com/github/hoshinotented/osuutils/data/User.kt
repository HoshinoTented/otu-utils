@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.OsuApi.refreshToken
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.endpoints.OsuUser
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.annotations.Contract
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * This is the only one mutable structure in this library, as token is very important, we can't lose it
 */
@ExperimentalTime
@Serializable
data class Token(
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
    application.refreshToken(this)
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

/**
 * User 指代的是用户
 */
@Serializable
@ExperimentalTime
data class User(val token: Token, val player: OsuUser)
