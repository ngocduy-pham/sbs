/*
 * RegressionResult
 * 
 * Version
 * 
 * Created on September 17th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import scala.tools.sbs.util.Constant

/** Represents the result of a benchmarking (of one benchmark on one {@link BenchmarkMode}).
  */
trait RegressionResult extends BenchmarkResult

abstract class RegressionSuccess(val benchmarkName: String) extends RegressionResult with BenchmarkSuccess {

  def confidenceLevel: Int

}

trait RegressionDetected {

  def current: (Double, Double)
  def previous: List[(Double, Double)]

  def toReport = List(
    Constant.INDENT + "New approach sample mean: " + current._1.formatted("%.2f") +
      " +- " + current._2.formatted("%.2f"),
    Constant.INDENT + "History sample mean:      ") ++
    (List[String]() /: previous)((lines, m) => lines :+
      Constant.INDENT + "                          " + m._1.formatted("%.2f") +
      " +- " + m._2.formatted("%.2f"))

}

trait CIRegression extends RegressionDetected {

  def CI: (Double, Double)

  override def toReport = super.toReport :+
    (Constant.INDENT + "Confidence interval:      [" + CI._1.formatted("%.2f") + "; " + CI._2.formatted("%.2f") + "]")

}

trait ANOVARegression extends RegressionDetected {

  def SSA: Double
  def SSE: Double
  def FValue: Double
  def F: Double

  override def toReport =
    super.toReport :+
      ("         F-test:") :+
      (" Sum-of-squared due to alternatives: " + SSA) :+
      ("       Sum-of-squared due to errors: " + SSE) :+
      ("                       Alternatives: " + "K") :+
      ("     Each alternatives measurements: " + "N") :+
      ("SSA * (N - 1) * K / (SSE * (K - 1)): " + FValue) :+
      ("                     F distribution: " + F)

}

case class CIRegressionSuccess(name: String,
                               confidenceLevel: Int,
                               current: (Double, Double),
                               previous: List[(Double, Double)],
                               CI: (Double, Double)) extends RegressionSuccess(name) with CIRegression

case class ANOVARegressionSuccess(name: String,
                                  confidenceLevel: Int,
                                  current: (Double, Double),
                                  previous: List[(Double, Double)],
                                  SSA: Double,
                                  SSE: Double,
                                  FValue: Double,
                                  F: Double) extends RegressionSuccess(name) with ANOVARegression

case class NoPreviousMeasurement(benchmarkName: String, measurementSuccess: MeasurementSuccess)
  extends RegressionResult with BenchmarkSuccess {

  def toReport = List(Constant.INDENT + "No previous measurement result to detect regression")

}

abstract class RegressionFailure(val benchmarkName: String) extends RegressionResult with BenchmarkFailure

case class CIRegressionFailure(name: String,
                               current: (Double, Double),
                               previous: List[(Double, Double)],
                               CI: (Double, Double)) extends RegressionFailure(name) with CIRegression

case class ANOVARegressionFailure(name: String,
                                  current: (Double, Double),
                                  previous: List[(Double, Double)],
                                  SSA: Double,
                                  SSE: Double,
                                  FValue: Double,
                                  F: Double) extends RegressionFailure(name) with ANOVARegression

case class ImmeasurableFailure(name: String, failure: MeasurementFailure)
  extends RegressionFailure(name) {

  def toReport = List(Constant.INDENT + failure.reason)

}
