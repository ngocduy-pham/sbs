/*
 * MethodRegressionDetector
 * 
 * Version
 * 
 * Created on November 1st, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import java.net.URL

import scala.tools.sbs.performance.CIRegressionFailure
import scala.tools.sbs.performance.CIRegressionSuccess
import scala.tools.sbs.performance.MeasurementSuccess
import scala.tools.sbs.pinpoint.instrumentation.JavaUtility
import scala.tools.sbs.pinpoint.strategy.InstrumentationRunner
import scala.tools.sbs.pinpoint.strategy.SubJVMPinpointMeasurer
import scala.tools.sbs.pinpoint.strategy.PreviousVersionExploiter
import scala.tools.sbs.pinpoint.strategy.TwinningDetector

trait MethodRegressionDetector
  extends TwinningDetector
  with InstrumentationRunner
  with PreviousVersionExploiter
  with SubJVMPinpointMeasurer {
  self: PinpointBenchmark with Configured =>

  type DetectorType <: Detector

  type MeasurerType = Measurer

  def regressionDetect(benchmark: BenchmarkType): ScrutinyRegressionResult =
    regressionDetector(benchmark) run

  trait Detector {

    val benchmark: BenchmarkType

    def run(): ScrutinyRegressionResult = {
      if (benchmark.className == "" || benchmark.methodName == "") {
        throw new NoPinpointingMethodException(benchmark)
      }

      log.info("Detecting performance regression of method " + benchmark.className + "." + benchmark.methodName)
      log.info("")

      twinningDetect(
        benchmark,
        measureCurrent,
        measurePrevious,
        regressOK => regressOK match {
          case ci: CIRegressionSuccess => ScrutinyCIRegressionSuccess(ci)
          case _                       => throw new ANOVAUnsupportedException
        },
        regressFailed => regressFailed match {
          case ci: CIRegressionFailure => (measureCurrent, measurePrevious) match {
            case (current: MeasurementSuccess, previous: MeasurementSuccess) =>
              ScrutinyCIRegressionFailure(ci)
            case _ => throw new AlgorithmFlowException(this.getClass)
          }
          case _ => throw new ANOVAUnsupportedException
        },
        failure => ScrutinyImmeasurableFailure(benchmark.info.name, failure))
    }

    private[this] lazy val measureCurrent = measureCommon(config.classpathURLs ++ benchmark.info.classpathURLs)

    // format: OFF
    private[this] lazy val measurePrevious = exploit(benchmark.previous,
                                                     benchmark.context,
                                                     config.classpathURLs ++ benchmark.info.classpathURLs,
                                                     measureCommon)

    private[this] def measureCommon(classpathURLs: List[URL]) =
      instrumentAndRun(benchmark,
                       (method, instrumentor) => instrumentor.sandwich(method,
                                                                       JavaUtility.callPinpointHarnessStart,
                                                                       JavaUtility.callPinpointHarnessEnd),
                       classpathURLs,
                       measurer.measure(benchmark.info, _))
    // format: ON

  }

  def measurer = new Measurer {}

  def regressionDetector(benchmark: BenchmarkType): DetectorType

}
