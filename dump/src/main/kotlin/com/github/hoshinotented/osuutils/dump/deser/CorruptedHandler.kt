package com.github.hoshinotented.osuutils.dump.deser

/**
 * TODO: 尝试移除这个, 我们现在不会有 null 的内容了, 因此唯一可能是 osu db 损坏, 这种情况下我们也无法解析
 * This only work on custom types (types that is not in [Deserializers])
 */
interface CorruptedHandler<T> {
  /**
   * Called when the data is corrupted or doesn't meet the nullability requirement
   * @param data partial parsed data, contains null for unparsed data
   */
  fun onCorrupted(data: Array<Any?>): T
}