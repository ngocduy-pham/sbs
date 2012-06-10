/*
 * Measurer
 * 
 * Version
 * 
 * Created on September 17th, 2011
 * 
 * Cretead by ND P
 */

package scala.tools.sbs
package performance

import scala.tools.sbs.io.Log
import scala.tools.sbs.benchmark.BenchmarkBase

/** A measurer for a benchmarking. Should have a typical type for benchmarking
  * on a typical {@link Mode}.
  */
trait Measurer extends Runner {
  self: Configured =>

  protected val mode: Mode

  protected val upperBound = manifest[PerfBenchmark.Benchmark]

  val benchmarkFactory = PerfBenchmark.factory(log, config)

  protected def doBenchmarking(benchmark: BenchmarkBase.Benchmark): BenchmarkResult =
    measure(benchmark.asInstanceOf[PerfBenchmark.Benchmark]) match {
      case metric: MeasurementSuccess => {

        log.info("[  Run OK  ]")
        log.debug("--metric: " + metric.getClass.getName)

        val regressionResult = regress(benchmark, metric)
        regressionResult match {
          case _: BenchmarkSuccess => {
            log.info("[  Benchmark OK  ]")
            store(benchmark, metric, true)
          }
          case _ => {
            log.info("[Benchmark FAILED]")
            log.debug("--benchmark failed: " + regressionResult.getClass.getName)
            store(benchmark, metric, false)
          }
        }
        regressionResult
      }
      case failed: MeasurementFailure => {
        log.info("[Run FAILED]")
        log.debug("--run failed: " + failed.getClass.getName)
        failure(benchmark, failed)
      }
    }

  protected def doGenerating(benchmark: BenchmarkBase.Benchmark) {
    val perfBenchmark = benchmark.asInstanceOf[PerfBenchmark.Benchmark]
    var i = 0
    while (i < perfBenchmark.sampleNumber) {
      measure(perfBenchmark) match {
        case success: MeasurementSuccess =>
          if (store(perfBenchmark, success, true)) {
            log.debug("--stored--")
            i += 1
            log.verbose("--got " + i + " sample(s)--")
          }
          else {
            log.debug("--cannot store " + benchmark.name)
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
  protected def measure(benchmark: PerfBenchmark.Benchmark): MeasurementResult

  /** Loads previous results and uses statistically rigorous method to detect regression.
    *
    * @param result	The metric result just measured.
    */
  protected def regress(benchmark: BenchmarkBase.Benchmark, metric: MeasurementSuccess): BenchmarkResult =
    metric match {
      case mesurement: MeasurementSuccess => {
        val history = Persistence(log, config, benchmark, mode).load()
        if (history.length < 1) {
          NoPreviousMeasurement(benchmark, mesurement)
        }
        else {
          Statistics(config, log) testDifference (benchmark, mesurement.series, history)
        }
      }
      case _ => {
        throw new WrongRunnerException(this, mode)
      }
    }

  protected def store(benchmark: BenchmarkBase.Benchmark, metric: MeasurementSuccess, success: Boolean) =
    Persistence(log, config, benchmark, mode).store(metric, success)

  protected def failure(benchmark: BenchmarkBase.Benchmark, failed: MeasurementFailure): BenchmarkResult =
    ImmeasurableFailure(benchmark, failed)

}

/** Factory object of {@link Measurer}.
  */
object MeasurerFactory {

  def apply(config: Config, log: Log, mode: Mode, harnessFactory: MeasurementHarnessFactory): Measurer = mode match {
    case StartUpState => new StartupHarness(log, config)
    case _            => new SubJVMMeasurer(log, config, mode, harnessFactory(mode))
  }

}
