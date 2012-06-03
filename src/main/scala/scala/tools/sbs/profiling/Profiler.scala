/*
 * Profiler
 * 
 * Version
 * 
 * Created October 2nd, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import scala.tools.sbs.benchmark.BenchmarkBase.Benchmark
import scala.tools.sbs.io.Log

/** Trait for some kinds of profiling.
  */
trait Profiler extends Runner {

  protected val upperBound = manifest[ProfBenchmark]

  val benchmarkFactory = ProfBenchmark.factory(log, config)

  protected def doBenchmarking(benchmark: Benchmark): BenchmarkResult = {
    profile(benchmark.asInstanceOf[ProfBenchmark])
  }

  protected def profile(benchmark: ProfBenchmark): ProfilingResult

  /** Do nothing method.
    */
  protected def doGenerating(benchmark: Benchmark) = ()

}

object ProfilerFactory {

  def apply(config: Config, log: Log): Profiler = {
    new JDIProfiler(config, log)
  }

}
