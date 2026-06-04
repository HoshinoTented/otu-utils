@file:UseSerializers(SeqSerializer::class)

package com.github.hoshinotented.osuutils.data

import com.github.hoshinotented.osuutils.api.SeqSerializer
import com.github.hoshinotented.osuutils.api.UserToken
import com.github.hoshinotented.osuutils.api.data.OsuUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.time.ExperimentalTime

/**
 * User 指代的是 otu-utils 中的用户, 而非 osu 账号
 */
@Serializable
@ExperimentalTime
data class User(val token: UserToken, val player: OsuUser)
