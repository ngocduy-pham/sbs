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

import scala.tools.sbs.io.Log

/** Trait for some kinds of profiling.
  */
trait Profiler extends Runner {
  self: ProfBenchmark with Configured =>

  def run(benchmark: BenchmarkType): BenchmarkResult = profile(benchmark)

  def profile(benchmark: BenchmarkType): ProfilingResult

  /** Do nothing method.
    */
  def generate(benchmark: BenchmarkType) = ()

}

object Profiler {

  def apply(config: Config, log: Log): Profiler = new JDIProfiler(config, log)

}
