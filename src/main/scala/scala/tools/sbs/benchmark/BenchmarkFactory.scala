/*
 * BenchmarkFactory
 * 
 * Version
 * 
 * Created on June 3rd, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package benchmark

object BenchmarkFactory {

  def apply(log: io.Log, config: Config, mode: Mode): BenchmarkBase#Factory = mode match {
    case SteadyState | StartUpState | MemoryUsage => performance.PerfBenchmark.factory(log, config)
    case Profiling                                => profiling.ProfBenchmark.factory(log, config)
    case Pinpointing                              => pinpoint.PinpointBenchmark.factory(log, config)
    case _                                        => BenchmarkBase.factory(log, config)
  }

}
