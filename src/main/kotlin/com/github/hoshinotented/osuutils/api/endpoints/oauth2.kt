package com.github.hoshinotented.osuutils.api.endpoints

import com.github.hoshinotented.osuutils.api.Endpoint
import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.data.Token
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object OAuth2Endpoints {
  enum class GrantType {
    Code, Refresh;
    
    override fun toString(): String {
      return when (this) {
        Code -> "authorization_code"
        Refresh -> "refresh_token"
      }
    }
  }
  
  @Endpoint("oauth/token", Endpoint.Method.Post, OsuApi.BASE_URL + "/")
  data class RefreshToken(
    val clientId: Int,
    val clientSecret: String,
    val refreshToken: String,
  ) : EndpointRequest {
    val scope: String = "public identify"
    val grantType: GrantType = GrantType.Refresh
  }
  
  @Endpoint("oauth/token", Endpoint.Method.Post, OsuApi.BASE_URL + "/")
  data class AccessToken(
    val clientId: Int,
    val clientSecret: String,
    val code: String,
    val redirectUri: String,
  ) : EndpointRequest {
    val scope: String = "public identify"
    val grantType: GrantType = GrantType.Code
  }
  
  @Serializable
  data class Response(
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
  ) {
    fun update(token: Token): Token {
      token.expiresIn = expiresIn
      token.accessToken = accessToken
      token.refreshToken = refreshToken
      return token
    }
  }
}