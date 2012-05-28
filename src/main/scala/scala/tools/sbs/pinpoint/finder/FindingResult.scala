/*
 * RegressionFound
 * 
 * Version
 * 
 * Created on November 1st, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package finder

import scala.collection.mutable.ArrayBuffer
import scala.tools.sbs.benchmark.Benchmark
import scala.tools.sbs.performance.regression.CIRegression

trait FindingResult extends ScrutinyResult {

  def benchmark: PinpointBenchmark

  def benchmarkName = benchmark.name

}

case class Regression(benchmark: PinpointBenchmark,
                      position: InvocationGraph,
                      current: (Double, Double),
                      previous: ArrayBuffer[(Double, Double)],
                      CI: (Double, Double))
  extends FindingResult
  with CIRegression
  with ScrutinySuccess {

  override def toReport = {
    ArrayBuffer(
      "Regression found:",
      "  from method call " +
        position.first.prototype + " at the " + position.startOrdinum + " time of its invocations") ++
      (if (position.length > 1)
        ArrayBuffer("  to method call " +
        position.last.prototype + " at the " + position.endOrdinum, " time of its invocations")
      else Nil) ++
      super[CIRegression].toReport
  }

}

case class NoRegression(benchmark: PinpointBenchmark,
                        confidenceLevel: Int,
                        current: (Double, Double),
                        previous: ArrayBuffer[(Double, Double)],
                        CI: (Double, Double))
  extends FindingResult
  with CIRegression
  with ScrutinyFailure {

  override def toReport = ArrayBuffer("No regression found", "") ++ super[CIRegression].toReport

}
  