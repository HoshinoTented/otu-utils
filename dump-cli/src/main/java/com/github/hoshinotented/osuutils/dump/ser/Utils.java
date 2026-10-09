package com.github.hoshinotented.osuutils.dump.ser;

import com.github.hoshinotented.osuutils.dump.deser.LazySeq;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kala.gson.collection.CollectionTypeAdapter;

public class Utils {
  public static Gson makeGson() {
    return new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .registerTypeAdapter(LazySeq.class, new LazySeqSerializer<>())
        .registerTypeAdapterFactory(CollectionTypeAdapter.factory())
        .create();
  }
}
