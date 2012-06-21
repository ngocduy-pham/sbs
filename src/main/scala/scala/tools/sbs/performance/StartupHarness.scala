/*
 * StartupHarness
 * 
 * Version
 * 
 * Created on September 25th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import scala.compat.Platform
import scala.sys.process.Process
import scala.tools.sbs.common.JVMInvoker
import scala.tools.sbs.io.Log

/** Measurer for benchmarking on startup state.
  */
class StartupHarness(val log: Log, val config: Config) extends Measurer with Configured {

  override protected val mode: Mode = StartUpState

  def measure(benchmark: PerfBenchmark.Benchmark): MeasurementResult = {
    log.info("[Benchmarking startup state]")

    val command   = JVMInvoker(log, config).command(benchmark, config.classpathURLs ++ benchmark.info.classpathURLs)
    val process   = Process(command)
    val exitValue = process !

    if (exitValue == 0) {
      new SeriesAchiever(config, log) achieve (
        benchmark,
        _ => true,
        () => {
          val start = Platform.currentTime
          process.!
          Platform.currentTime - start
        },
        false)
    }
    else {
      new ProcessMeasurementFailure(exitValue)
    }
  }

}
