package scala.tools.sbs.test
package pinpoint
package finder

import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.io.Path.string2path
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.BenchmarkCompiler
import scala.tools.sbs.pinpoint.finder.InvocationRecorder
import scala.tools.sbs.pinpoint.PinpointBenchmark
import scala.tools.sbs.util.FileUtil

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Spec

class InvocationRecorderSpec extends Spec with BeforeAndAfterAll {

  val benchmarkName    = "InvocationRecorderTestBenchmark"
  val invoRecorderDir  = testDir         / "InvocationRecorderSpec" createDirectory ()
  val invoRecorderFile = invoRecorderDir / (benchmarkName + ".scala") toFile
  val instrumentedOut  = invoRecorderDir / "instrumented" createDirectory ()
  val backupPlace      = invoRecorderDir / "backup" createDirectory ()
  val benchmarkInfo    = new BenchmarkInfo(benchmarkName, invoRecorderFile, Nil, Nil, 0, false)

  def benchmarkSource(runBody: String, otherDefs: String, pinpointClass: String, pinpointMethod: String) =
    "class " + benchmarkName + """ extends scala.tools.sbs.pinpoint.PinpointTemplate {

  override val className = """+ "\"" + pinpointClass + "\"" + """

  override val methodName = """+ "\"" + pinpointMethod + "\"" + """

  def init() = ()

  def run() = {""" +
    runBody + """
  }

""" +
    otherDefs +
"""

  def reset() = ()

}
"""

  def define(content: String) = {
    invoRecorderFile.deleteIfExists
    FileUtil createFile invoRecorderFile.path
    FileUtil.write(invoRecorderFile.path, content)
  }

  def createBenchmark(content: String) = ({
    define(content)
    val compiler = BenchmarkCompiler(testLog, testConfig)
    benchmarkInfo.isCompiledOK(compiler, testConfig)
    benchmarkInfo.expand(PinpointBenchmark.factory(testLog, testConfig), testConfig)
  }).asInstanceOf[PinpointBenchmark.Benchmark]

  def createRecorder(runBody: String, otherDefs: String, className: String, methodName: String = "run") =
    new InvocationRecorder(
      testConfig,
      testLog,
      createBenchmark(benchmarkSource(runBody, otherDefs, className, methodName)),
      instrumentedOut,
      backupPlace)

  override def afterAll = FileUtil clean testDir

  describe("InvocationRecorder") {

    it("creates an empty graph if the given method doesn't invoke any method during run") {
      val recorder = createRecorder("", "", benchmarkName)
      recorder.currentGraph traverse (m => testLog.verbose(m.prototype))
      assert(recorder.currentGraph.length == 0)
    }

    it("collects all method invocations in a benchmark run") {
      val runBody = """
    foo
    bar
    baz"""

      val otherDefs = """
    def foo = ()
    def bar = ()
    def baz = ()
"""

      val recorder = createRecorder(runBody, otherDefs, benchmarkName)
      recorder.currentGraph traverse (m => testLog.verbose(m.prototype))
      expect(ArrayBuffer("foo", "bar", "baz"))(recorder.currentGraph traverse (_ methodName))
    }

    it("collects all method invocations - in while loop") {
      val runBody = """
    var i = 0
    while(i < 2) {
      reset()
      i += 1
    }"""

      val recorder = createRecorder(runBody, "", benchmarkName)
      recorder.currentGraph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("reset", "reset"))(recorder.currentGraph traverse (_ methodName))
    }

    it("collects all method invocations - in if statement") {
      val runBody = "if (true) init else reset"

      val recorder = createRecorder(runBody, "", benchmarkName)
      recorder.currentGraph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("init"))(recorder.currentGraph traverse (_ methodName))
    }

    it("collects all method invocations - in pattern matching") {
      val runBody = """
    true match {
      case true => reset
      case _    => init
    }"""

      val recorder = createRecorder(runBody, "", benchmarkName)
      recorder.currentGraph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("reset"))(recorder.currentGraph traverse (_ methodName))
    }

    it("collects all method invocations in the first run of a recursive method") {
      val runBody = "rec(false)"

      val otherDefs = """
    def rec(arg: Boolean): Unit =
      if (arg) rec(false)
      else {
        foo
        bar
        baz
    }

    def foo = ()

    def bar = ()

    def baz = ()
"""

      val recorder = createRecorder(runBody, otherDefs, benchmarkName, "rec")
      recorder.currentGraph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("foo", "bar", "baz"))(recorder.currentGraph traverse (_ methodName))
    }

    it("collects all method invocations in the first run of a method") {
      val runBody = """
    rec(false)
    rec(true)"""

      val otherDefs = "  def rec(arg: Boolean): Unit = if (arg) init else reset"

      val recorder = createRecorder(runBody, otherDefs, benchmarkName, "rec")
      recorder.currentGraph traverse (m => testLog.verbose(m.prototype))

      expect(ArrayBuffer("reset"))(recorder.currentGraph traverse (_ methodName))
    }

  }

}

