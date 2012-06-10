/*
 * Scrutinizer
 * 
 *  Version
 *  
 *  Created on October 13th, 2011
 *  
 *  Created by ND P
 */

package scala.tools.sbs
package pinpoint

import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.io.Log

trait Scrutinizer extends Runner {
  self: Configured =>

  protected val upperBound = manifest[PinpointBenchmark.Benchmark]

  val benchmarkFactory = PinpointBenchmark.factory(log, config)

  protected def doBenchmarking(benchmark: BenchmarkBase.Benchmark): BenchmarkResult =
    scrutinize(benchmark.asInstanceOf[PinpointBenchmark.Benchmark])

  protected def scrutinize(benchmark: PinpointBenchmark.Benchmark): ScrutinyResult

  /** Do-nothing method.
    */
  protected def doGenerating(benchmark: BenchmarkBase.Benchmark) = ()

}

object ScrutinizerFactory {

  def apply(config: Config, log: Log): Scrutinizer = {
    new MethodScrutinizer(config, log)
  }

}

trait ScrutinyResult extends BenchmarkResult

trait ScrutinySuccess extends BenchmarkSuccess with ScrutinyResult

trait ScrutinyFailure extends BenchmarkFailure with ScrutinyResult
