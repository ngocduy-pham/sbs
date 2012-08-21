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
case class StartupHarness(log: Log, config: Config)
  extends PerformanceMeasurer with PerfBenchmarkCreator with Configured {

  type MeasurerType = Measurer

  val mode = StartUpState

  trait Measurer extends super.Measurer {

    def measure(benchmark: BenchmarkType): MeasurementResult = {
      log.info("[Benchmarking startup state]")

      val command = JVMInvoker(log, config).command(benchmark.info, config.classpathURLs ++ benchmark.info.classpathURLs)
      val process = Process(command)
      val exitValue = process !

      if (exitValue == 0) {
        new SeriesAchiever(config, log) achieve (
          benchmark,
          _ => true,
          {
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

  def measurer: MeasurerType = new Measurer {}

}
