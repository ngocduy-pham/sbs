/*
 * BinarryWrapper
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

import java.net.URL

import scala.tools.nsc.io.Directory
import scala.tools.sbs.io.Log
import scala.tools.sbs.performance.ANOVARegressionFailure
import scala.tools.sbs.performance.CIRegressionFailure
import scala.tools.sbs.performance.CIRegressionSuccess
import scala.tools.sbs.performance.RegressionFailure
import scala.tools.sbs.pinpoint.instrumentation.JavaUtility
import scala.tools.sbs.pinpoint.strategy.InstrumentationRunner
import scala.tools.sbs.pinpoint.strategy.PinpointMeasurerFactory
import scala.tools.sbs.pinpoint.strategy.PreviousVersionExploiter
import scala.tools.sbs.pinpoint.strategy.TwinningDetector

/** @author Sthy
  *
  */
trait BinaryWrapper extends FinderWrapper {

  /** Uses a binary-search-like algorithm to find the regression point
    * in a list of function calls.
    */
  class BinaryFinder(config: Config,
                     log: Log,
                     benchmark: PinpointBenchmark.Benchmark,
                     className: String,
                     methodName: String,
                     instrumentedPath: Directory,
                     storagePath: Directory,
                     graph: InvocationGraph)
    extends BasicFinder(
      config: Config,
      log,
      benchmark,
      className,
      methodName,
      instrumentedPath,
      storagePath)
    with TwinningDetector
    with InstrumentationRunner
    with PreviousVersionExploiter
    with Configured {

    def find() = binaryFind(graph)

    private def binaryFind(graph: InvocationGraph): FindingResult = {
      def narrow(regressionFailure: RegressionFailure): FindingResult = {
        /** Creates only in case necessary.
          */
        def currentRegression = regressionFailure match {
          case CIRegressionFailure(_, current, previous, ci) => {
            RegressionPoint(
              benchmark,
              graph,
              current,
              previous,
              ci)
          }
          case _: ANOVARegressionFailure => throw new ANOVAUnsupportedException
          case _                         => throw new AlgorithmFlowException(this.getClass)
        }
        if (graph.length > 1) {
          val (firstHalf, secondHalf) = graph.split
          try {
            lazy val secondRegression = binaryFind(secondHalf)
            val firstRegression =
              try { binaryFind(firstHalf) }
              catch {
                case _: RegressionUndetectableException =>
                  secondRegression match {
                    case _: NoRegression => currentRegression
                    case _               => secondRegression
                  }
              }
            firstRegression match {
              case noRegression: NoRegression => {
                secondRegression match {
                  case _: NoRegression => currentRegression
                  case _               => secondRegression
                }
              }
              case _ => { firstRegression }
            }
          }
          catch {
            case _: RegressionUndetectableException => currentRegression
          }
        }
        else {
          currentRegression
        }
      }

      if (graph.length == 1) {
        log.info("  Checking whether the " + graph.startOrdinum + " time invocation " +
          "of method call " + graph.first.prototype + "is a Regression")
      }
      else {
        log.info("  Finding Regression between " +
          "the " + graph.startOrdinum + " time invocation of method call " + graph.first.prototype +
          " and the " + graph.endOrdinum + " time invocation of method call " + graph.last.prototype)
      }
      log.info("")

      twinningDetect(
        benchmark,
        measureCurrent(graph),
        measurePrevious(graph),
        regressOK => regressOK match {
          case ciOK: CIRegressionSuccess =>
            NoRegression(benchmark, regressOK.confidenceLevel, ciOK.current, ciOK.previous, ciOK.CI)
          case _ =>
            throw new ANOVAUnsupportedException
        },
        narrow,
        _ => throw new RegressionUndetectableException(className, methodName, graph))
    }

    private def measureCurrent(graph: InvocationGraph) =
      measureCommon(graph, config.classpathURLs ++ benchmark.classpathURLs)

    private def measurePrevious(graph: InvocationGraph) = exploit(
      benchmark.previous,
      benchmark.context,
      config.classpathURLs ++ benchmark.classpathURLs,
      measureCommon(graph, _))

    private def measureCommon(graph: InvocationGraph, classpathURLs: List[URL]) =
      instrumentAndRun(
        (method, instrumentor) => {
          instrumentor.insertBeforeCall(method, graph.first.prototype, JavaUtility.callPinpointHarnessStart)
          instrumentor.insertAfterCall(method, graph.first.prototype, JavaUtility.callPinpointHarnessEnd)
        },
        classpathURLs,
        PinpointMeasurerFactory(config, log).measure(benchmark, _))

  }

}