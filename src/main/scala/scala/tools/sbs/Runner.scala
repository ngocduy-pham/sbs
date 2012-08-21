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
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.io.Log
import scala.tools.sbs.performance.MeasurementHarnessFactory
import scala.tools.sbs.performance.PerformanceMeasurer
import scala.tools.sbs.pinpoint.Scrutinizer
import scala.tools.sbs.profiling.Profiler

/** Runs the benchmark for some purpose.
  */
trait Runner {
  self: BenchmarkBase =>

  /** Runs the benchmark and produces the benchmarking result.
    */
  def run(info: BenchmarkInfo): Option[BenchmarkResult] = factory expand info map run
  
  def run(benchmark: BenchmarkType): BenchmarkResult

  /** Generates sample results for later regression detections.
    */
  def generate(info: BenchmarkInfo): Unit = factory expand info foreach generate
  
  def generate(benchmark: BenchmarkType): Unit

}

object Runner {

  def apply(config: Config, log: Log, mode: Mode): Runner = mode match {
    case Profiling                                => Profiler(config, log)
    case Pinpointing                              => Scrutinizer(config, log)
    case StartUpState | SteadyState | MemoryUsage => PerformanceMeasurer(config, log, mode, MeasurementHarnessFactory)
    case _                                        => throw new NotSupportedBenchmarkMode(mode)
  }

}

trait RunResult {

  def toXML: scala.xml.Elem

}

trait RunSuccess extends RunResult

trait RunFailure extends RunResult
