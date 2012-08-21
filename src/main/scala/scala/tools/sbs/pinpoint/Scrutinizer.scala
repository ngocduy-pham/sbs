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

import scala.tools.sbs.io.Log

trait Scrutinizer extends Runner {
  self: PinpointBenchmark =>

  def run(benchmark: BenchmarkType): BenchmarkResult =
    scrutinize(benchmark)
    
  def scrutinize(benchmark: BenchmarkType): ScrutinyResult

  /** Do-nothing method.
    */
  def generate(benchmark: BenchmarkType) = ()

}

object Scrutinizer {

  def apply(config: Config, log: Log): Scrutinizer = {
    new MethodScrutinizer(config, log)
  }

}

trait ScrutinyResult extends BenchmarkResult

trait ScrutinySuccess extends BenchmarkSuccess with ScrutinyResult

trait ScrutinyFailure extends BenchmarkFailure with ScrutinyResult
