@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.endpoints.OsuUser
import com.github.hoshinotented.osuutils.serde.SeqSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * This is the only one mutable structure in this library, as token is very important, we can't lose it
 */
@ExperimentalTime
@Serializable
data class Token(var requestTime: Instant, var expiresIn: Int, var accessToken: String, var refreshToken: String) {
  val expiresTime: Instant
    get() {
      return requestTime.plus(expiresIn.seconds)
    }
}

/**
 * User 指代的是用户
 */
@Serializable
@ExperimentalTime
data class User(val token: Token, val player: OsuUser)
