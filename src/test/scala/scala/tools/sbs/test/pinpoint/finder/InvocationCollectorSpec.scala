package scala.tools.sbs.test
package pinpoint
package finder

import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.io.Path.string2path
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.BenchmarkCompiler
import scala.tools.sbs.pinpoint.finder.InvocationCollector
import scala.tools.sbs.pinpoint.PinpointBenchmark
import scala.tools.sbs.util.FileUtil

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Spec

class InvocationCollectorSpec extends Spec with BeforeAndAfterAll {

  val benchmarkName = "InvocationCollectorTestBenchmark"

  val invoCollectorDir = testDir / "InvocationCollectorSpec" createDirectory ()

  val invoCollectorFile = invoCollectorDir / (benchmarkName + ".scala") toFile

  val instrumentedOut = invoCollectorDir / "instrumented" createDirectory ()

  val backupPlace = invoCollectorDir / "backup" createDirectory ()

  val benchmarkInfo = new BenchmarkInfo(benchmarkName, invoCollectorFile, Nil, Nil, 0, true)

  def benchmarkSource(runBody: String, otherDefs: String) =
    "class " + benchmarkName + """ extends scala.tools.sbs.pinpoint.PinpointTemplate {
	|
	|  def init() = ()
	|
	|  def run() = {
    |
    |""" +
      runBody +
      """
    |  }
    |  """ +
      otherDefs +
      """
    |  def reset() = ()
    |
	|}
    """

  def define(content: String) = {
    invoCollectorFile.deleteIfExists
    FileUtil createFile invoCollectorFile.path
    FileUtil.write(invoCollectorFile.path, content)
  }

  def createBenchmark(content: String) = ({
    define(content)
    val compiler = BenchmarkCompiler(testLog, testConfig)
    benchmarkInfo.isCompiledOK(compiler, testConfig)
    benchmarkInfo.expand(PinpointBenchmark.factory(testLog, testConfig), testConfig)
  }).asInstanceOf[PinpointBenchmark.Benchmark]

  def createCollector(runBody: String, otherDefs: String, className: String, methodName: String) =
    new InvocationCollector(
      testConfig,
      testLog,
      createBenchmark(benchmarkSource(runBody, otherDefs)),
      className,
      methodName,
      instrumentedOut,
      backupPlace)

  override def afterAll = FileUtil clean testDir

  describe("InvocationCollector") {

    it("creates an empty graph if the given method doesn't invoke any method during run") {
      val collector = createCollector("", "", benchmarkName, "run")
      collector.graph traverse (m => testLog.verbose(m.prototype))
      assert(collector.graph.length == 0)
    }

    it("collects all method invocations in a benchmark run") {
      val runBody = """
      |  foo
      |  bar
      |  baz
      """

      val otherDefs = """
      |  def foo = ()
      |  def bar = ()
      |  def baz = ()
	  """

      val collector = createCollector(runBody, otherDefs, benchmarkName, "run")
      collector.graph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("foo", "bar", "baz"))(collector.graph traverse (_ methodName))
    }

    it("collects all method invocations - in while loop") {
      val runBody = """
      |  var i = 0
      |  while(i < 2) {
      |    reset()
      |    i += 1
      |  }
        """

      val collector = createCollector(runBody, "", benchmarkName, "run")
      collector.graph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("reset", "reset"))(collector.graph traverse (_ methodName))
    }

    it("collects all method invocations - in if statement") {
      val runBody = "if (true) init else reset"

      val collector = createCollector(runBody, "", benchmarkName, "run")
      collector.graph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("init"))(collector.graph traverse (_ methodName))
    }

    it("collects all method invocations - in pattern matching") {
      val runBody = """
      |  true match {
      |    case true => reset
      |    case _    => init
      |  }
        """

      val collector = createCollector(runBody, "", benchmarkName, "run")
      collector.graph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("reset"))(collector.graph traverse (_ methodName))
    }

    it("collects all method invocations in the first run of a recursive method") {
      val runBody = "rec(false)"

      val otherDefs = """
      |  def rec(arg: Boolean): Unit =
      |  if (arg) rec(false)
      |  else {
      |    foo
      |    bar
      |    baz
      |  }
      |
      |  def foo = ()
      |
      |  def bar = ()
    
      |  def baz = ()
	  """

      val collector = createCollector(runBody, otherDefs, benchmarkName, "rec")
      collector.graph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("foo", "bar", "baz"))(collector.graph traverse (_ methodName))
    }

    it("collects all method invocations in the first run of a method") {
      val runBody = """
      |  rec(false)
      |  rec(true)
      """

      val otherDefs = "  def rec(arg: Boolean): Unit = if (arg) init else reset"

      val collector = createCollector(runBody, otherDefs, benchmarkName, "rec")
      collector.graph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("reset"))(collector.graph traverse (_ methodName))
    }

  }

}
