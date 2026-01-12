package com.github.hoshinotented.osuutils.util

import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.parser.ModCombinationLexer
import com.github.hoshinotented.osuutils.parser.ModCombinationParser
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableEnumSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

@Serializable
data class ModRestriction(val code: String) {
  object Serde : KSerializer<ModRestriction> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
      "com.github.hoshinotented.osuutils.data.ModRestriction",
      PrimitiveKind.STRING
    )
    
    override fun serialize(
      encoder: Encoder,
      value: ModRestriction,
    ) {
      encoder.encodeString(value.code)
    }
    
    override fun deserialize(decoder: Decoder): ModRestriction {
      return ModRestriction(decoder.decodeString())
    }
  }
  
  val compiled: MCExpr by lazy {
    produceModRestriction(code)
  }
}

sealed interface MCExpr {
  enum class TestResult {
    Success,
    TooMany,
    TooFew
  }
  
  companion object {
    fun MCExpr.test(mods: ImmutableSeq<Mod>): TestResult {
      val set = MutableEnumSet.from(Mod::class.java, mods.filter { it in Mod.DIFFICULTY_CHANGE_MODS })
      val ctx = Context(MutableEnumSet.create(Mod::class.java))
      val match = this.accept(set, ctx)
      val exactMatch = ctx.matched == set
      return when {
        match && exactMatch -> TestResult.Success
        match -> TestResult.TooMany
        else -> TestResult.TooFew
      }
    }
  }
  
  data class Context(var matched: MutableEnumSet<Mod>)
  
  /**
   * @return if success
   */
  fun accept(mods: MutableEnumSet<Mod>, context: Context): Boolean
  
  data class Exact(val mod: Mod?) : MCExpr {
    override fun accept(mods: MutableEnumSet<Mod>, context: Context): Boolean {
      if (mod == null) return mods.isEmpty
      if (mod in mods) {
        context.matched.add(mod)
        return true
      } else {
        return false
      }
    }
  }
  
  data class Seq(val mods: ImmutableSeq<MCExpr>) : MCExpr {
    override fun accept(mods: MutableEnumSet<Mod>, context: Context): Boolean {
      return this.mods.foldLeft(true) { acc, it ->
        // we can't place acc before .accept
        it.accept(mods, context) && acc
      }
    }
  }
  
  data class Or(val lhs: MCExpr, val rhs: MCExpr) : MCExpr {
    override fun accept(mods: MutableEnumSet<Mod>, context: Context): Boolean {
      val savePoint = context.matched.clone()
      if (lhs.accept(mods, context)) return true
      context.matched = MutableEnumSet.from(Mod::class.java, savePoint)
      if (rhs.accept(mods, context)) return true
      context.matched = MutableEnumSet.from(Mod::class.java, savePoint)
      return false
    }
  }
  
  data class Combination(val sets: ImmutableSeq<MCExpr>, val excludeNoMod: Boolean) : MCExpr {
    override fun accept(mods: MutableEnumSet<Mod>, context: Context): Boolean {
      // excludeNoMod here is interpreted as matched at least one
      
      val anyMatch = sets.foldLeft(false) { acc, it ->
        it.accept(mods, context) || acc
      }
      
      return !excludeNoMod || anyMatch
    }
  }
}

fun produceModRestriction(expr: String): MCExpr {
  val stream = ANTLRInputStream(expr)
  val lexer = ModCombinationLexer(stream)
  val tokens = CommonTokenStream(lexer)
  val parser = ModCombinationParser(tokens)
  return produceModRestriction(parser.expr())
}

fun produceModRestriction(ctx: ModCombinationParser.ExprContext): MCExpr {
  val mod = ctx.MOD()
  // Exact
  if (mod != null) {
    val text = mod.text
    if (text == "NM") return MCExpr.Exact(null)
    return MCExpr.Exact(Mod.valueOf(text))
  }
  
  // Or
  if (ctx.OR() != null) {
    val lhs = produceModRestriction(ctx.expr(0))
    val rhs = produceModRestriction(ctx.expr(1))
    return MCExpr.Or(lhs, rhs)
  }
  
  // grouped
  if (ctx.LP() != null) {
    return produceModRestriction(ctx.expr(0))
  }
  
  // combination
  if (ctx.LB() != null) {
    val excludeNM = ctx.NEG() != null
    val mods = ImmutableSeq.from(ctx.expr()).map(::produceModRestriction)
    
    return MCExpr.Combination(mods, excludeNM)
  }
  
  // otherwise, it is a seq
  val lhs = produceModRestriction(ctx.expr(0))
  val rhs = produceModRestriction(ctx.expr(1))
  // even HDHREZ is considered (HDHR)EZ, which means seq is left-associate,
  // but it is still possible that rhs is a Seq, consider HD(EZHR)
  
  val lhsSeq = if (lhs is MCExpr.Seq) lhs.mods else ImmutableSeq.of(lhs)
  val rhsSeq = if (rhs is MCExpr.Seq) rhs.mods else ImmutableSeq.of(rhs)
  
  return MCExpr.Seq(lhsSeq.appendedAll(rhsSeq))
}
