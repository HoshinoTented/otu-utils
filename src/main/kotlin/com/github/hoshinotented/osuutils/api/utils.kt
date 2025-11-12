package com.github.hoshinotented.osuutils.api

import com.github.hoshinotented.osuutils.data.Token
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun HttpResponse<String>.successOrThrow(): String {
  if (statusCode() == 200) return body()
  throw HttpException(this)
}

fun HttpResponse<String>.checkNotFound(): HttpResponse<String>? {
  if (statusCode() == 404) return null
  return this
}

fun HttpRequest.Builder.oauth(token: Token): HttpRequest.Builder = apply {
  setHeader("Authorization", "Bearer ${token.accessToken}")
}