import com.github.hoshinotented.osuutils.api.endpoints.Mod
import com.github.hoshinotented.osuutils.prettyMods
import kala.collection.immutable.ImmutableSeq
import kotlin.test.asserter
import com.github.hoshinotented.osuutils.api.endpoints.Mod.*
import com.github.hoshinotented.osuutils.util.MCExpr
import com.github.hoshinotented.osuutils.util.produceModRestriction
import kotlin.test.Test

class ModCombinationTest {
  fun test(code: String, vararg mods: Mod, expect: MCExpr.TestResult = MCExpr.TestResult.Success) {
    val mods = ImmutableSeq.from(mods)
    
    println("Test code: $code")
    println("Against to mods: ${prettyMods(mods, prefix = "")}")
    println("Expecting: $expect")
    
    val expr = produceModRestriction(code)
    
    with(MCExpr) {
      val result = expr.test(mods)
      if (result != expect) {
        asserter.fail("Test failed due to: $result")
      }
    }
  }
  
  @Test
  fun positive() {
    test("NM")
    test("HD", HD)
    test("HDHR", HD, HR)
    test("HR HD", HD, HR)
    test("HD(HREZ)", EZ, HD, HR)
    test("(HDHR)EZ", EZ, HD, HR)
    test("EZ(HD|HR)", EZ, HD)
    test("EZ(HD|HR)", EZ, HR)
    test("(HD|HR)(HT|DT)", HD, HT)
    test("(HD|HR)(HT|DT)", HD, DT)
    test("(HD|HR)(HT|DT)", HR, HT)
    test("(HD|HR)(HT|DT)", HR, DT)
    test("{ HD, HR }")
    test("{ HD, HR }", HD)
    test("{ HD, HR }", HR)
    test("{ HD, HR }", HD, HR)
    test("DT{ HD, HR }", DT)
    test("DT{ HD, HR }", DT, HD)
    test("DT{ HD, HR }", DT, HR)
    test("DT{ HD, HR }", DT, HD, HR)
    test("{ { HD }, { HR } }")
    test("{ { HD }, { HR } }", HD)
    test("{ { HD }, { HR } }", HR)
    test("{ { HD }, { HR } }", HD, HR)
  }
  
  @Test
  fun negative() {
    test("NM", HD, expect = MCExpr.TestResult.TooFew)   // the only special case
    test("HD", HR, expect = MCExpr.TestResult.TooFew)
    test("HR", HD, HR, expect = MCExpr.TestResult.TooMany)
    test("!{ HD }", expect = MCExpr.TestResult.TooFew)
    test("!{ HD }HR", HR, expect = MCExpr.TestResult.TooFew)
    test("{ HD, HR }", HD, DT, expect = MCExpr.TestResult.TooMany)
  }
}