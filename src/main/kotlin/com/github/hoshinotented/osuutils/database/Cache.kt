package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.commonSerde
import kala.collection.mutable.MutableMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

abstract class Cache<T : Any> {
  private lateinit var cached: T
  
  abstract fun load(): T?
  abstract fun save(value: T)
  
  fun get(): T? {
    if (::cached.isInitialized) return cached
    cached = load() ?: return null
    return cached
  }
  
  fun set(value: T) {
    this.cached = value
    save(value)
  }
}

/**
 * @param T must be [Serializable]
 */
class JsonCache<T : Any>(val path: Path, val serializer: KSerializer<T>) : Cache<T>() {
  override fun load(): T? {
    if (!path.exists()) return null
    return commonSerde.decodeFromString(serializer, path.readText())
  }
  
  override fun save(value: T) {
    path.createParentDirectories()
    path.writeText(commonSerde.encodeToString(serializer, value))
  }
}

abstract class KVCache<K, V : Any> {
  private val cache = MutableMap.create<K, V>()
  
  abstract fun load(key: K): V?
  abstract fun save(key: K, value: V)
  
  fun get(key: K): V? {
    if (cache.containsKey(key)) return cache.get(key)
    val loaded = load(key) ?: return null
    cache.put(key, loaded)
    return loaded
  }
  
  fun set(key: K, value: V) {
    cache.set(key, value)
    save(key, value)
  }
}

class DirJsonKVCache<K, V : Any>(val serializer: KSerializer<V>, val pathProvider: (K) -> Path) : KVCache<K, V>() {
  override fun load(key: K): V? {
    val path = pathProvider(key)
    if (!path.exists()) return null
    return commonSerde.decodeFromString(serializer, path.readText())
  }
  
  override fun save(key: K, value: V) {
    val path = pathProvider(key)
    path.createParentDirectories()
    path.writeText(commonSerde.encodeToString(serializer, value))
  }
}