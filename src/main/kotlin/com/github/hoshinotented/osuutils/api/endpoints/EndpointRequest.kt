package com.github.hoshinotented.osuutils.api.endpoints

import com.github.hoshinotented.osuutils.api.processEndpointRequest
import com.github.hoshinotented.osuutils.data.Token
import java.net.http.HttpRequest

interface EndpointRequest {
  companion object {
    fun HttpRequest.Builder.initCommon(): HttpRequest.Builder = apply {
      setHeader("Accept", "application/json")
    }
  }
  
  fun toRequest(): HttpRequest.Builder {
    return processEndpointRequest(this)
  }
}