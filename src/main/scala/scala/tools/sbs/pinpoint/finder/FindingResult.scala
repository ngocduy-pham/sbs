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

import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.performance.CIRegression

trait FindingResult extends ScrutinyResult {

  def info: BenchmarkInfo

  def benchmarkName = info.name

}

case class RegressionPoint(info: BenchmarkInfo,
                           position: InvocationGraph,
                           current: (Double, Double),
                           previous: List[(Double, Double)],
                           CI: (Double, Double))
  extends FindingResult
  with CIRegression
  with ScrutinySuccess {

  override def toReport = {
    List(
      "Regression found:",
      "  from method call " +
        position.first.prototype + " at the " + position.startOrdinum + " time of its invocations") ++
      (if (position.length > 1)
        List("  to method call " +
        position.last.prototype + " at the " + position.endOrdinum, " time of its invocations")
      else Nil) ++
      super[CIRegression].toReport
  }

}

case class NoRegression(info: BenchmarkInfo,
                        confidenceLevel: Int,
                        current: (Double, Double),
                        previous: List[(Double, Double)],
                        CI: (Double, Double))
  extends FindingResult
  with CIRegression
  with ScrutinyFailure {

  override def toReport = List("No regression found", "") ++ super[CIRegression].toReport

}
  