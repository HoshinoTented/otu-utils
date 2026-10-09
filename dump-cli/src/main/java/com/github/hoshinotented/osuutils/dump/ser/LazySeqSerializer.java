package com.github.hoshinotented.osuutils.dump.ser;

import com.github.hoshinotented.osuutils.dump.deser.LazySeq;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class LazySeqSerializer<T> implements JsonSerializer<LazySeq<T>> {
  @Override
  public JsonElement serialize(LazySeq<T> src, Type typeOfSrc, JsonSerializationContext context) {
    var evaluated = src.getValue();
    return context.serialize(evaluated);
  }
}
