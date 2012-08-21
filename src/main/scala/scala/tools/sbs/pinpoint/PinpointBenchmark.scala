/*
 * PinpointingBenchmark
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import java.lang.reflect.Method

import scala.tools.nsc.io.Directory
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.RunOnlyHarness
import scala.tools.sbs.performance.PerfBenchmark
import scala.tools.sbs.profiling.ProfBenchmark

trait PinpointBenchmark extends PerfBenchmark with ProfBenchmark {
  self: Configured =>

  // format: OFF
  type BenchmarkType    <: Benchmark
  type subSnippet       <: BenchmarkType
  type subInitializable <: BenchmarkType
  // format: ON

  trait Benchmark extends super[PerfBenchmark].Benchmark with super[ProfBenchmark].Benchmark {

    def classes: List[String] = Nil
    def className: String

    /** Location of the old class files to be used during pinpointing regression detection.
      * Should not be included in `Config.classpathURLs` and `Benchmark.classpathURLs`.
      */
    def previous: Directory

    /** Maximum recursion depth in the process of finding bottleneck.
      * Value of -1 is stand for unlimited depth.
      */
    def depth: Int

  }

  class Snippet(info: BenchmarkInfo,
                multiplier: Int,
                measurement: Int,
                sampleNumber: Int,
                val className: String,
                val methodName: String,
                val exclude: List[String],
                val previous: Directory,
                val depth: Int,
                method: Method,
                context: ClassLoader,
                config: Config)
    extends super[PerfBenchmark].Snippet(
      info,
      method,
      context,
      config,
      multiplier,
      measurement,
      sampleNumber) with Benchmark {

    val fieldName = ""
    val howToLaunch = Right(this)

  }

  class Initializable(info: BenchmarkInfo,
                      benchmarkObject: PinpointTemplate,
                      context: ClassLoader,
                      config: Config)
    extends super[PerfBenchmark].Initializable(
      info,
      benchmarkObject,
      context,
      config) with Benchmark {

    val className = benchmarkObject.className
    val methodName = benchmarkObject.methodName
    val fieldName = ""
    val exclude = benchmarkObject.exclude
    val previous = benchmarkObject.previous
    val depth = benchmarkObject.depth
    val howToLaunch = Left(RunOnlyHarness)

  }

}

object PinpointBenchmark {

  // format: OFF
  val classNameOpt = "classname"
  val previousOpt  = "previous"
  val depthOpt     = "depth"
  // format: ON

}
