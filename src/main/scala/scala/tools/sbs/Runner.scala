/*
 * Runner
 * 
 * Version
 * 
 * Created on October 5th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs

import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.common.RuntimeTypeChecker
import scala.tools.sbs.io.Log
import scala.tools.sbs.performance.MeasurementHarnessFactory
import scala.tools.sbs.performance.MeasurerFactory
import scala.tools.sbs.pinpoint.ScrutinizerFactory
import scala.tools.sbs.profiling.Profiler

/** Runs the benchmark for some purpose.
  */
trait Runner extends RuntimeTypeChecker {
  self: Configured =>

  import BenchmarkBase.Benchmark

  def benchmarkFactory: BenchmarkBase#Factory

  /** Runs the benchmark and produces the benchmarking result.
    */
  def run(benchmark: Benchmark): BenchmarkResult =
    if (check(benchmark.getClass)) {
      val result = doBenchmarking(benchmark)
      result.toReport foreach log.info
      result
    }
    else {
      throw new MismatchBenchmarkImplementationException(benchmark, this)
    }

  protected def doBenchmarking(benchmark: Benchmark): BenchmarkResult

  /** Generates sample results for later regression detections.
    */
  def generate(benchmark: Benchmark) =
    if (check(benchmark.getClass)) {
      doGenerating(benchmark)
    }
    else {
      throw new MismatchBenchmarkImplementationException(benchmark, this)
    }

  protected def doGenerating(benchmark: Benchmark)

}

object RunnerFactory {

  def apply(config: Config, log: Log, mode: Mode): Runner = mode match {
    case Profiling                                => Profiler(config, log)
    case Pinpointing                              => ScrutinizerFactory(config, log)
    case StartUpState | SteadyState | MemoryUsage => MeasurerFactory(config, log, mode, MeasurementHarnessFactory)
    case _                                        => throw new NotSupportedBenchmarkMode(mode)
  }

}

trait RunResult {

  def toXML: scala.xml.Elem

}

trait RunSuccess extends RunResult

trait RunFailure extends RunResult
