package com.github.hoshinotented.osuutils.api

import com.github.hoshinotented.osuutils.api.endpoints.EndpointRequest.Companion.initCommon
import kala.collection.Map
import kala.collection.immutable.ImmutableMap
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableMap
import kala.control.Option
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

fun String.toSnakeCase(): String {
  val result = StringBuilder()
  for (i in indices) {
    val c = this[i]
    if (c.isUpperCase()) {
      // in case of consecutive upper case
      if (i > 0 && this[i - 1].isLowerCase()) {
        result.append('_')
      }
      result.append(c.lowercaseChar())
    } else {
      result.append(c)
    }
  }
  return result.toString()
}

sealed interface EndpointComponent {
  companion object {
    fun rebuild(path: ImmutableSeq<EndpointComponent>, provider: (String) -> String): String {
      return path.joinToString("/") {
        when (it) {
          is Raw -> it.str
          is Ref -> provider(it.ref)
        }
      }
    }
  }
  
  data class Ref(val ref: String) : EndpointComponent
  data class Raw(val str: String) : EndpointComponent
}

fun parseEndpointPath(path: String): ImmutableSeq<EndpointComponent> {
  if (path.isEmpty()) return ImmutableSeq.empty()
  val components = ImmutableSeq.from(path.split('/'))
  
  // 不做过多检查
  return components.map {
    val stripped = it.removeSurrounding("{", "}")
    
    // stripped === this if [it] is not surrounded with "{}"
    if (stripped === it) {
      EndpointComponent.Raw(it)
    } else {
      EndpointComponent.Ref(stripped)
    }
  }
}

class RequestObjectResolveResult(
  val endpoint: Endpoint,
  val bodyType: BodyType.Type,
  val components: ImmutableSeq<EndpointComponent>,
  val pathReferenced: Map<String, Member>,
  val data: Map<String, Field>,
) {
  companion object {
    val CACHE: MutableMap<Class<*>, RequestObjectResolveResult> = MutableMap.create()
  }
}

// no multi thread, sory
fun resolveEndpointRequest(clazz: KClass<*>): RequestObjectResolveResult {
  OsuApi.logger.info("Resolving ${clazz.jvmName}")
  var cache = RequestObjectResolveResult.CACHE.getOrNull(clazz.java)
  if (cache != null) {
    OsuApi.logger.info("Cache hit for ${clazz.jvmName}")
    return cache
  }
  
  cache = doResolveEndpointRequest(clazz)
  RequestObjectResolveResult.CACHE.put(clazz.java, cache)
  return cache
}

fun doResolveEndpointRequest(clazz: KClass<*>): RequestObjectResolveResult {
  val clazz = clazz.java
  
  val endpoint: Endpoint = clazz.getAnnotation(Endpoint::class.java)
    ?: throw IllegalArgumentException("${clazz.canonicalName} must be annotated with '@Endpoint'")
  
  var bodyType = clazz.getAnnotation(BodyType::class.java)?.type
  
  if (bodyType == null) {
    OsuApi.logger.warning("No BodyType is given, default to Url")
    bodyType = BodyType.Type.Url
  }
  
  val components = parseEndpointPath(endpoint.path)
  // find the corresponding member in path reference, only field or method without parameters
  val map = MutableMap.create<String, Option<Member>>()
  
  components.forEach {
    if (it is EndpointComponent.Ref) {
      map[it.ref] = Option.none()
    }
  }
  
  val data = MutableMap.create<String, Field>()
  
  for (field in clazz.declaredFields) {
    if (Modifier.isTransient(field.modifiers)) continue
    
    val paramName: ParamName? = field.getAnnotation(ParamName::class.java)
    // 使用 ParamName 或者是原本的名字来做 resolving
    val name = paramName?.name ?: field.name
    val ref = map.getOrNull(name)
    if (ref != null) {
      assert(ref.isEmpty)
      map[name] = Option.some(field)
      
      OsuApi.logger.info("path reference '$name' is resolved to ${clazz.canonicalName}#${field.name}")
      
      // 对于被 path ref 的 field，不会再被作为载荷
      continue
    }
    
    val dataName = paramName?.name
      ?: field.name.toSnakeCase() // 所有参数都必须保证是 camelCase, 否则 toSnakeCase 是未定义行为
    data[dataName] = field
    
    OsuApi.logger.info("parameter '$dataName' is resolved to ${clazz.canonicalName}#${field.name}")
  }
  
  for (key in map.keysView()) {
    if (map.get(key).isEmpty) {
      // 尝试在 method 里面找
      val method = clazz.getMethod(key) ?: throw IllegalArgumentException("Cannot resolve path reference {$key}")
      if (Void::class.java.isAssignableFrom(method.returnType)) {
        throw IllegalArgumentException("Path referenced method must not returns void")
      }
      
      OsuApi.logger.info("path reference '$key' is resolved to ${clazz.canonicalName}#${method.name}()")
      map[key] = Option.some(method)
    }
  }
  
  val pathReferenced = ImmutableMap.from(
    map.view()
      .mapValues { _, v -> v.get() })
  
  return RequestObjectResolveResult(endpoint, bodyType, components, pathReferenced, data)
}

fun processEndpointRequest(request: Any): HttpRequest.Builder {
  val result = resolveEndpointRequest(request::class)
  val endpoint = result.endpoint
  val bodyType = result.bodyType
  val components = result.components
  val map = result.pathReferenced
  val data = result.data
  
  // TODO: remove tailing '/'? this is possible when the last component is path argument and it is null.
  val rebuiltPath = EndpointComponent.rebuild(components) {
    serialize(request, map[it], bodyType) ?: ""   // basically only the last path argument is nullable
  }
  
  OsuApi.logger.info("Request path: $rebuiltPath")
  
  val dataValues = MutableMap.create<String, String>()
  for (pair in data) {
    val v = pair.component2
    val sered = serialize(request, v, bodyType) ?: continue
    dataValues[pair.component1] = sered
  }
  
  val content = serialize(dataValues, bodyType)
  
  val finalUri = if (endpoint.method == Endpoint.Method.Get) {
    "$rebuiltPath?$content"
  } else rebuiltPath
  
  return HttpRequest.newBuilder(URI.create(endpoint.baseUrl).resolve(finalUri))
    .initCommon()
    .apply {
      when (endpoint.method) {
        Endpoint.Method.Get -> GET()
        Endpoint.Method.Post -> POST(HttpRequest.BodyPublishers.ofString(content, Charset.forName("UTF-8")))
      }
      
      setHeader("Content-Type", bodyType.contentType())
    }
}

fun serialize(obj: Any, member: Member, type: BodyType.Type): String? {
  val clazz = obj.javaClass
  
  return when (member) {      // never fail
    is Field -> {
      if (!member.trySetAccessible()) throw IllegalArgumentException("Unable to access field ${clazz.canonicalName}#${member.name}")
      serialize(member.get(obj) ?: return null, type)
    }
    
    is Method -> serialize(member.invoke(obj) ?: return null, type)
    else -> TODO("unreachable")
  }
}

fun serialize(data: Any, type: BodyType.Type): String {
  return when (data) {
    is Number -> data.toString()
    is Boolean -> if (data) "1" else "0"
    is Enum<*> -> data.toString()
    is String -> if (type == BodyType.Type.Url) URLEncoder.encode(data, Charset.forName("UTF-8")) else data
    else -> throw IllegalArgumentException("Unable to serialize ${data.javaClass.canonicalName}")
  }
}

fun serialize(data: Map<String, String>, type: BodyType.Type): String {
  return when (type) {
    BodyType.Type.Url -> serializeToUrl(data)
    BodyType.Type.Json -> TODO()
  }
}

fun serializeToUrl(data: Map<String, String>): String {
  return data.toSeq().joinToString("&") {
    "${it.component1}=${it.component2}"
  }
}