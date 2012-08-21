/*
 * RegressionFinder
 * 
 * Version
 * 
 * Created on May 28th, 2012
 * 
 * Created by ND P
 */
package scala.tools.sbs
package pinpoint
package finder

/** Uses instrumentation method to point out the method call
  * that is a performance regression point in a given method.
  */
trait RegressionFinder {
  self: PinpointBenchmark =>

  type FinderType <: Finder

  def regressionFind(benchmark: BenchmarkType): FindingResult =
    regressionFinder(benchmark) run

  trait Finder {

    val benchmark: BenchmarkType

    def run(): FindingResult

  }

  def regressionFinder(benchmark: BenchmarkType): FinderType

}
