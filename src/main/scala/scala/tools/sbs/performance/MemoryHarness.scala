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

/** Measurer for benchmarking on memory usage. Should be run on a clean new JVM.
  */
object MemoryHarness extends MeasurementHarness with PerfBenchmarkCreator with Configured {

  val mode = MemoryUsage

  def measure(benchmark: Benchmark): MeasurementResult = {
    log.info("[Benchmarking memory consumption]")
    val runtime: Runtime = Runtime.getRuntime
    seriesAchiever achieve (
      benchmark,
      series => series forall (_ == series.head),
      {
        benchmark.init()
        val start = runtime.freeMemory
        benchmark.run()
        val measured = start - runtime.freeMemory
        benchmark.reset()
        measured
      })
  }

}
