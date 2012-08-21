/*
 * SteadyHarness
 * 
 * Version
 * 
 * Created on September 17th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import scala.compat.Platform

/** Measurer for benchmarking on steady state. Should be run on a clean new JVM.
  */
object SteadyHarness extends MeasurementHarness with PerfBenchmarkCreator with Configured {

  val mode = SteadyState

  def measure(benchmark: BenchmarkType): MeasurementResult = {
    val statistic = Statistics(config, log)
    log.info("[Benchmarking steady state]")
    seriesAchiever achieve (
      benchmark,
      series => (statistic CoV series) < config.precisionThreshold,
      {
        benchmark.init()
        val start = Platform.currentTime
        var i = 0
        while (i < benchmark.multiplier) {
          benchmark.run()
          i += 1
        }
        val measured = Platform.currentTime - start
        benchmark.reset()
        measured
      })
  }

}
