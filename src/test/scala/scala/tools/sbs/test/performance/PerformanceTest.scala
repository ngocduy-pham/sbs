package scala.tools.sbs.test
package performance

import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path.string2path
import scala.tools.sbs.Configured
import scala.tools.sbs.Mode
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.performance.PerfBenchmarkCreator

trait PerformanceTest extends PerfBenchmarkCreator with Configured {
  
  val log = testLog
  val config = testConfig
  
  object DummyBenchmark extends Benchmark {
    val info: BenchmarkInfo = new BenchmarkInfo(
      "dummy",
      testDir,
      Nil,
      testConfig.classpathURLs,
      45000,
      true
    )
    def multiplier = 1
    def measurement = 10
    def sampleNumber = 0
    def timeout = 60000
    def shouldCompile = false
    def createLog(mode: Mode) = testLog
    def init() = ()
    def run() = ()
    def reset() = ()
    def context = null
    def profileClasses = List("DummyBenchmark")
    def profileExclude = List("")
    def profileMethod = "run"
    def profileField = ""
    def pinpointClass = "DummyBenchmark"
    def pinpointMethod = "run"
    def pinpointExclude = List("")
    def pinpointPrevious = Directory("")
  }

}
