package com.github.hoshinotented.osuutils.api

/**
 * @param path 格式 "foo/{reference}/bar" 其中 reference 可以是任何 field 和无参 method。
 *             并且 `{reference}` 必须独占一个 component，不允许 `foo{reference}`。
 *             当引用一个 field/method 的时候，需要用原本的名字或者是 ParamName 的名字（如果有 ParamName 就只能用 ParamName 的名字），
 *             不应该使用 toSnakeCase 之后的名字。
 *             如果 String? 类型的字段为 null，那么在 path 中对应的引用不会被替换成 `null`，而是空字符串。
 *             因此仅能在最后一个 component 处使用可空的 reference。
 * @param baseUrl must ends with '/'
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Endpoint(val path: String, val method: Method = Method.Get, val baseUrl: String = OsuApi.API_V2) {
  enum class Method {
    Get,
    Post
  }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ParamName(val name: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class BodyType(val type: Type) {
  enum class Type {
    Url, Json;
    
    fun contentType(): String {
      return when (this) {
        Url -> "application/x-www-form-urlencoded"
        Json -> "application/json"
      }
    }
  }
}