/*
 * PerformanceBenchmark
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import java.lang.reflect.Method
import java.net.URL

import scala.tools.nsc.io.Path
import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.io.Log

trait PerfBenchmark extends BenchmarkBase {
  self: Configured =>

  // format: OFF
  type BenchmarkType    <: Benchmark
  type subSnippet       <: BenchmarkType
  type subInitializable <: BenchmarkType
  // format: ON

  trait Benchmark extends super.Benchmark {

    def multiplier: Int
    def measurement: Int
    def sampleNumber: Int

  }

  class Snippet(info: BenchmarkInfo,
                method: Method,
                context: ClassLoader,
                config: Config,
                val multiplier: Int,
                val measurement: Int,
                val sampleNumber: Int)
    extends super.Snippet(info, context, method, config) with Benchmark

  class Initializable(info: BenchmarkInfo,
                      benchmarkObject: PerfTemplate,
                      context: ClassLoader,
                      config: Config)
    extends super.Initializable(info, context, benchmarkObject, config) with Benchmark {

    // format: OFF
    val multiplier   = 1
    val measurement  = benchmarkObject.measurement
    val sampleNumber = benchmarkObject.sampleNumber
    // format: ON

  }

}

object PerfBenchmark {
  
  // format: OFF
  val multiplierOpt  = "multiplier"
  val measurementOpt = "measurement"
  val sampleOpt      = "sample"
  // format: ON

}
