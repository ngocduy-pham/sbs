/*
 * MemoryHarness
 * 
 * Version
 * 
 * Created on September 17th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import java.lang.Runtime

import PerfBenchmark.Benchmark

/** Measurer for benchmarking on memory usage. Should be run on a clean new JVM.
  */
object MemoryHarness extends MeasurementHarness[Benchmark] {

  protected val mode = MemoryUsage

  protected val upperBound = manifest[Benchmark]

  def measure(benchmark: Benchmark): MeasurementResult = {
    log.info("[Benchmarking memory consumption]")
    val runtime: Runtime = Runtime.getRuntime
    seriesAchiever achieve (
      benchmark,
      series => series forall (_ == series.head),
      () => {
        benchmark.init()
        val start = runtime.freeMemory
        benchmark.run()
        val measured = start - runtime.freeMemory
        benchmark.reset()
        measured
      })
  }

}
