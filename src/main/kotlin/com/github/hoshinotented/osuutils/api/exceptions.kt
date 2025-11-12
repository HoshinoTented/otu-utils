package com.github.hoshinotented.osuutils.api

import java.net.http.HttpResponse

data class HttpException(val resp: HttpResponse<String>) : Exception(resp.body())