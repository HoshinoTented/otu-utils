package com.github.hoshinotented.osuutils.dump.deser

/**
 * This only work on custom types (types that is not in [Deserializers])
 */
interface CorruptedHandler<T> {
  /**
   * Called when the data is corrupted or doesn't meet the nullability requirement
   * @param data partial parsed data, contains null for unparsed data
   */
  fun onCorrupted(data: Array<Any?>): T
}