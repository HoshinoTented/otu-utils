package com.github.hoshinotented.osuutils.serde

import kala.collection.immutable.ImmutableMap
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.FreezableMutableList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

class SeqSerializer<T>(val elementSerializer: KSerializer<T>) : KSerializer<ImmutableSeq<T>> {
  override val descriptor: SerialDescriptor = SerialDescriptor(
    "kala.collection.immutable.ImmutableSeq",
    listSerialDescriptor(elementSerializer.descriptor)
  )
  
  override fun serialize(
    encoder: Encoder,
    value: ImmutableSeq<T>,
  ) {
    val handle = encoder.beginCollection(descriptor, value.size())
    value.forEachIndexed { i, it ->
      handle.encodeSerializableElement(descriptor, i, elementSerializer, it)
    }
    handle.endStructure(descriptor)
  }
  
  override fun deserialize(decoder: Decoder): ImmutableSeq<T> {
    val seq = FreezableMutableList.create<T>()
    
    decoder.decodeStructure(descriptor) {
      val knownSize = decodeCollectionSize(descriptor)
      if (knownSize != -1 && decodeSequentially()) {
        for (i in 0 until knownSize) {
          seq.append(decodeSerializableElement(descriptor, i, elementSerializer))
        }
      } else {
        while (true) {
          val index = decodeElementIndex(descriptor)
          if (index == CompositeDecoder.DECODE_DONE) break
          seq.append(decodeSerializableElement(descriptor, index, elementSerializer))
        }
      }
    }
    
    return seq.freeze()
  }
}