package scala.tools.sbs.test
package pinpoint
package finder

import org.scalatest.Spec
import scala.tools.sbs.pinpoint.instrumentation.CodeInstrumentor
import scala.tools.sbs.pinpoint.finder.GraphBuilder

class InvocationGraphSpec extends Spec {

  describe("An invocation graph") {

    it("has length == 0 while holding no invocation") {
      val g = new GraphBuilder
      assert(g.result.length == 0)
    }

    it("has start == null while holding no invocation") {
      val g = new GraphBuilder
      assert(g.result.first == null)
    }

    it("has last == null while holding no invocation") {
      val g = new GraphBuilder
      assert(g.result.last == null)
    }

    it("has length == 1 while added 1 invocation") {
      val g = new GraphBuilder
      g add ("c", "m", "s")
      assert(g.result.length == 1)
    }

    it("has length increases along with number of invocations") {
      val g = new GraphBuilder
      val max = 5
      var i = 0
      while (i < max) {
        g add ("c", "m", "s")
        i += 1
        assert(g.result.length == i)
      }
    }

    it("has start is the just added invocation") {
      val g = new GraphBuilder
      g add ("c", "m", "s")
      assert(g.result.first.prototype == CodeInstrumentor.prototype("c", "m", "s"))
    }

    it("has start stays unchanged when added more invocations") {
      val g = new GraphBuilder
      g add ("c", "m", "s")
      g add ("c", "m2", "s")
      assert(g.result.first.prototype == CodeInstrumentor.prototype("c", "m", "s"))
    }

    it("has last is the only added invocation") {
      val g = new GraphBuilder
      g add ("c", "m", "s")
      assert(g.result.last.prototype == CodeInstrumentor.prototype("c", "m", "s"))
    }

    it("has last is the latest invocation") {
      val g = new GraphBuilder
      g add ("c", "m", "s")
      g add ("c", "m2", "s")
      assert(g.result.last.prototype == CodeInstrumentor.prototype("c", "m2", "s"))
      g add ("c", "m", "s")
      assert(g.result.last.prototype == CodeInstrumentor.prototype("c", "m", "s"))
    }

    it("raises error when splitting with less than 2 invocations") {
      val g = new GraphBuilder
      intercept[Error](g.result.split)
      g add ("c", "m", "s")
      intercept[Error](g.result.split)
    }

    it("splits into two 1-length graph when having 2 invocations") {
      val g = new GraphBuilder
      g add ("c", "1", "s")
      g add ("c", "2", "s")
      val (f, s) = g.result.split
      assert(f.length == 1)
      assert(f.first.prototype == CodeInstrumentor.prototype("c", "1", "s"))
      assert(s.length == 1)
      assert(s.last.prototype == CodeInstrumentor.prototype("c", "2", "s"))
    }

    it("splits into two 1-length graph when having 2 invocations of 1 method") {
      val g = new GraphBuilder
      g add ("c", "1", "s")
      g add ("c", "1", "s")
      val (f, s) = g.result.split
      assert(f.length == 1)
      assert(f.first.prototype == CodeInstrumentor.prototype("c", "1", "s"))
      assert(s.length == 1)
      assert(s.last.prototype == CodeInstrumentor.prototype("c", "1", "s"))
    }

    it("splits into two new graphs which have equivalent length in respect of time orders") {
      val g = new GraphBuilder
      for (n <- 1 to 10) g add ("c", n.toString, "s")
      val (f, s) = g.result.split
      assert(f.length == 5)
      val first = for (i <- 1 to 5) yield CodeInstrumentor.prototype("c", i.toString, "s")
      f traverse (i => assert(first contains (i prototype)))
      assert(s.length == 5)
      val second = for (i <- 6 to 10) yield CodeInstrumentor.prototype("c", i.toString, "s")
      s traverse (i => assert(second contains (i prototype)))
    }

    it("should be OK with ad-hoc test on split - 1") {
      val g = new GraphBuilder
      g add ("c", "1", "s")
      g add ("c", "1", "s")
      g add ("c", "1", "s")
      g add ("c", "2", "s")
      val (f, s) = g.result.split
      f traverse (i => assert(i.prototype == CodeInstrumentor.prototype("c", "1", "s")))
      assert(s.first.prototype == CodeInstrumentor.prototype("c", "1", "s"))
      assert(s.last.prototype == CodeInstrumentor.prototype("c", "2", "s"))
    }

    it("should be OK with ad-hoc test on split - 2") {
      val g = new GraphBuilder
      g add ("c", "1", "s")
      g add ("c", "2", "s")
      g add ("c", "3", "s")
      g add ("c", "1", "s")
      g add ("c", "4", "s")
      val (f, s) = g.result.split
      val first = for (i <- 1 to 3) yield CodeInstrumentor.prototype("c", i.toString, "s")
      f traverse (i => assert(first contains (i prototype)))
      val (s1, s2) = s.split
      assert(s1.last.prototype == CodeInstrumentor.prototype("c", "1", "s"))
      assert(s2.first.prototype == CodeInstrumentor.prototype("c", "4", "s"))
    }

  }

}
