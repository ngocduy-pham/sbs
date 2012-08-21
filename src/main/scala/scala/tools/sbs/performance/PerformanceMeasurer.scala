/*
 * PerformanceMeasurer
 * 
 * Version
 * 
 * Created on August 19th, 2012
 * 
 * Cretead by ND P
 */

package scala.tools.sbs
package performance

import java.lang.reflect.Method

import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.io.Log

/** A measurer for a benchmarking. Should have a typical type for benchmarking
  * on a typical {@link Mode}.
  */
trait PerformanceMeasurer extends Runner {
  self: PerfBenchmark with Configured =>

  type MeasurerType <: Measurer

  val mode: Mode

  def run(benchmark: BenchmarkType): BenchmarkResult = measurer run benchmark

  def generate(benchmark: BenchmarkType): Unit = measurer generate benchmark

  def measurer: MeasurerType

  trait Measurer {

    def run(benchmark: BenchmarkType): BenchmarkResult =
      measure(benchmark) match {
        case metric: MeasurementSuccess => {

          log.info("[  Run OK  ]")
          log.debug("--metric: " + metric.getClass.getName)

          val regressionResult = regress(benchmark.info, metric)
          regressionResult match {
            case _: BenchmarkSuccess => {
              log.info("[  Benchmark OK  ]")
              store(benchmark.info, metric, true)
            }
            case _ => {
              log.info("[Benchmark FAILED]")
              log.debug("--benchmark failed: " + regressionResult.getClass.getName)
              store(benchmark.info, metric, false)
            }
          }
          regressionResult
        }
        case failed: MeasurementFailure => {
          log.info("[Run FAILED]")
          log.debug("--run failed: " + failed.getClass.getName)
          failure(benchmark.info, failed)
        }
      }

    def generate(benchmark: BenchmarkType) {
      var i = 0
      while (i < benchmark.sampleNumber) {
        measure(benchmark) match {
          case success: MeasurementSuccess =>
            if (store(benchmark.info, success, true)) {
              log.debug("--stored--")
              i += 1
              log.verbose("--got " + i + " sample(s)--")
            }
            else {
              log.debug("--cannot store " + benchmark.info.name)
            }
          case failure: MeasurementFailure => log.debug("--generation error at " + i + ": " + failure.reason + "--")
          case _                           => throw new Error("WTF is just created?")
        }
      }

    }

    /** Measures the desired metric from benchmark.
      * Measurements are achieved through `Benchmark.run()` method.
      * For benchmarks that need no initialization,
      * the `runs` argument can be any positive integer value.
      * On the other hand, benchmarks that need initializations,
      * which had been defined as subclass of
      * {@link scala.tools.sbs.benchmark.BenchmarkTemplate}
      * should have `runs` argument equals to 1.
      * (The `Benchmark.init()` method will be run only once
      * between two measurements, so if `runs` is larger than 1,
      * the benchmark may fail to run.)
      */
    def measure(benchmark: BenchmarkType): MeasurementResult

    /** Loads previous results and uses statistically rigorous method to detect regression.
      *
      * @param result	The metric result just measured.
      */
    def regress(info: BenchmarkInfo, metric: MeasurementSuccess): BenchmarkResult =
      metric match {
        case mesurement: MeasurementSuccess => {
          val history = Persistence(log, config, info, mode).load()
          if (history.length < 1) {
            NoPreviousMeasurement(info.name, mesurement)
          }
          else {
            Statistics(config, log) testDifference (info, mesurement.series, history)
          }
        }
        case _ => {
          throw new WrongRunnerException(self, mode)
        }
      }

    def store(info: BenchmarkInfo, metric: MeasurementSuccess, success: Boolean) =
      Persistence(log, config, info, mode).store(metric, success)

    def failure(info: BenchmarkInfo, failed: MeasurementFailure): BenchmarkResult =
      ImmeasurableFailure(info.name, failed)

  }

}

/** Factory object of {@link Measurer}.
  */
object PerformanceMeasurer {

  def apply(_config: Config, _log: Log, _mode: Mode, harnessFactory: MeasurementHarnessFactory): PerformanceMeasurer =
    _mode match {
	  // format: OFF
      case StartUpState => new StartupHarness(_log, _config)
      case _            => new SubJVMMeasurer with PerfBenchmarkCreator with Configured {
        
        type MeasurerType = Measurer
        
        val log                = _log
        val config             = _config
        val mode               = _mode
        val measurementHarness = harnessFactory(mode)
        val measurer           = new Measurer {}
        // format: ON
      }

    }

}
