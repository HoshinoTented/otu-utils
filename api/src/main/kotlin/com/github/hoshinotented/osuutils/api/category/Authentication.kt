package com.github.hoshinotented.osuutils.api.category

import com.github.hoshinotented.osuutils.api.ApplicationRole
import com.github.hoshinotented.osuutils.api.ClientToken
import com.github.hoshinotented.osuutils.api.OsuApi
import com.github.hoshinotented.osuutils.api.OsuApplication
import com.github.hoshinotented.osuutils.api.UserToken
import com.github.hoshinotented.osuutils.api.category.Users.me
import com.github.hoshinotented.osuutils.api.data.OsuUser
import com.github.hoshinotented.osuutils.api.endpoints.OAuth2Endpoints
import com.github.hoshinotented.osuutils.api.oauth
import com.github.hoshinotented.osuutils.api.successOrThrow
import org.jetbrains.annotations.Contract
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Clock

object Authentication {
  /**
   * @return null if failed to refresh token
   * @throws com.github.hoshinotented.osuutils.api.HttpException when status code is not 200
   */
  @Contract(mutates = "param1")
  fun OsuApplication.refreshToken(token: UserToken): UserToken {
    OsuApi.logger.info("token is expired, refreshing...")
    if (dontRefreshToken) {
      throw IllegalStateException("Don't refresh token")
    }

    val req = OAuth2Endpoints.RefreshToken(clientId, clientSecret, token.refreshToken)
      .toRequest()
      .build()

    val resp = OsuApi.client.send(req, HttpResponse.BodyHandlers.ofString())
      .successOrThrow()

    val createTime = Clock.System.now()
    val respObj = OsuApi.deJson.decodeFromString<OAuth2Endpoints.Response>(resp)

    token.requestTime = createTime
    token.refreshToken = respObj.refreshToken
    token.accessToken = respObj.accessToken
    token.expiresIn = respObj.expiresIn

    return token
  }

  fun OsuApplication.clientGrantToken(): ClientToken {
    val req = OAuth2Endpoints.ClientToken(clientId, clientSecret)
      .toRequest()
      .build()

    val resp = OsuApi.client.send(req, HttpResponse.BodyHandlers.ofString())
      .successOrThrow()

    val createTime = Clock.System.now()
    val respObj = OsuApi.deJson.decodeFromString<OAuth2Endpoints.ClientTokenResponse>(resp)

    return ClientToken(createTime, respObj.expiresIn, respObj.accessToken)
  }

  /**
   * @throws com.github.hoshinotented.osuutils.api.HttpException
   */
  fun OsuApplication.exchangeToken(code: String): UserToken {
    val req = OAuth2Endpoints.AccessToken(clientId, clientSecret, code, redirectUri)
      .toRequest()
      .build()

    val resp = OsuApi.client.send(req, HttpResponse.BodyHandlers.ofString())
      .successOrThrow()

    val createTime = Clock.System.now()
    val respObj = OsuApi.deJson.decodeFromString<OAuth2Endpoints.Response>(resp)

    return UserToken(createTime, respObj.expiresIn, respObj.accessToken, respObj.refreshToken)
  }

  /**
   * Construct a user with given authorization code
   */
  fun OsuApplication.newUser(code: String): Pair<UserToken, OsuUser> {
    val token = exchangeToken(code)
    val role = withToken(token)
    val user = role.me()
    return token to user
  }

  /**
   * 发送一条需要 auth 的请求，如果 token 过期，那么会重新兑换 token 并且再请求一次
   *
   * @throws com.github.hoshinotented.osuutils.api.HttpException
   */
  fun ApplicationRole.sendAuthedRequest(req: HttpRequest.Builder): HttpResponse<String> {
    var resp = OsuApi.client.send(
      req.oauth(token).build(),
      HttpResponse.BodyHandlers.ofString()
    )

    if (resp.statusCode() == 401) {
      // try refresh
      token.refresh(app)
      resp = OsuApi.client.send(
        req.oauth(token).build(),
        HttpResponse.BodyHandlers.ofString()
      )
    }

    return resp
  }
}