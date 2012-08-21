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

import scala.tools.sbs.performance.ANOVARegressionFailure
import scala.tools.sbs.performance.CIRegressionFailure
import scala.tools.sbs.performance.CIRegressionSuccess
import scala.tools.sbs.performance.RegressionFailure
import scala.tools.sbs.pinpoint.instrumentation.JavaUtility
import scala.tools.sbs.pinpoint.strategy.InstrumentationRunner
import scala.tools.sbs.pinpoint.strategy.SubJVMPinpointMeasurer
import scala.tools.sbs.pinpoint.strategy.PreviousVersionExploiter
import scala.tools.sbs.pinpoint.strategy.TwinningDetector

trait BinaryRegressionFinder
  extends RegressionFinder
  with SubJVMPinpointMeasurer
  with TwinningDetector
  with InstrumentationRunner
  with PreviousVersionExploiter {
  self: PinpointBenchmark with Configured =>

  type FinderType <: Finder

  /** Uses a binary-search-like algorithm to find the regression point
    * in a list of function call expressions.
    */
  trait Finder extends super.Finder {

    def run(): FindingResult =
      find(benchmark.className, benchmark.methodName)

    def find(inspectedClass: String, inspectedMethod: String): FindingResult = {
      val invocationRecorder = new InvocationRecorder(config, log, benchmark, instrumentedPath, storagePath)

      if (invocationRecorder.currentGraph.length == 0) {
        log.info("no detectable method call found")
        log.info("")
        throw new RegressionUndetectableException(inspectedClass, inspectedMethod, invocationRecorder.currentGraph)
      }

      log.debug("not empty calling list from: " + inspectedClass + "." + inspectedMethod)

      if (!invocationRecorder.isMatchOK) {
        log.error("Mismatch expression lists, skip further detection")
        throw new MismatchExpressionList(inspectedClass, inspectedMethod, invocationRecorder)
      }
      find(invocationRecorder.currentGraph, inspectedClass, inspectedMethod)
    }

    private def find(graph: InvocationGraph,
                     inspectedClass: String,
                     inspectedMethod: String): FindingResult = {

      //    private def binaryFind(graph: InvocationGraph): FindingResult = {
      def narrow(regressionFailure: RegressionFailure): FindingResult = {
        /** Creates only in case necessary.
          */
        def currentRegression = regressionFailure match {
          case CIRegressionFailure(_, current, previous, ci) => {
            RegressionPoint(benchmark.info, graph, current, previous, ci)
          }
          case _: ANOVARegressionFailure => throw new ANOVAUnsupportedException
          case _                         => throw new AlgorithmFlowException(this.getClass)
        }
        if (graph.length > 1) {
          val (firstHalf, lastHalf) = graph.split
          try {
            lazy val secondRegression = find(lastHalf, inspectedClass, inspectedMethod)
            val firstRegression =
              try { find(firstHalf, inspectedClass, inspectedMethod) }
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
          catch { case _: RegressionUndetectableException => currentRegression }
        }
        else { currentRegression }
      }

      if (graph.length == 1) {
        log.info("  Checking whether the " + graph.startOrdinum + "(st, nd, rd, th) time invocation " +
          "of method call " + graph.first.prototype + "is a Regression")
      }
      else {
        log.info("  Finding Regression between " +
          "the " + graph.startOrdinum + "(st, nd, rd, th) time invocation of method call " + graph.first.prototype +
          " and the " + graph.endOrdinum + "(st, nd, rd, th) time invocation of method call " + graph.last.prototype)
      }
      log.info("")

      // format: OFF
      twinningDetect(benchmark,
                     measureCurrent(benchmark, graph),
                     measurePrevious(benchmark, graph),
			         regressOK => regressOK match {
			           case ciOK: CIRegressionSuccess =>
			             NoRegression(benchmark.info, regressOK.confidenceLevel, ciOK.current, ciOK.previous, ciOK.CI)
			           case _ =>
			             throw new ANOVAUnsupportedException
			         },
                     narrow,
                     _ => throw new RegressionUndetectableException(inspectedClass, inspectedMethod, graph))
      // format: ON
    }

    private def measureCurrent(benchmark: BenchmarkType, graph: InvocationGraph) =
      measureCommon(benchmark, graph, config.classpathURLs ++ benchmark.info.classpathURLs)

    // format: OFF
    private def measurePrevious(benchmark: BenchmarkType, graph: InvocationGraph) =
      exploit(benchmark.previous,
              benchmark.context,
              config.classpathURLs ++ benchmark.info.classpathURLs,
              measureCommon(benchmark, graph, _))

    private def measureCommon(benchmark: BenchmarkType, graph: InvocationGraph, classpathURLs: List[URL]) =
      instrumentAndRun(benchmark,
                       (method, instrumentor) => {
                         instrumentor.insertBeforeCall(method, graph.first.prototype, JavaUtility.callPinpointHarnessStart)
                         instrumentor.insertAfterCall(method, graph.first.prototype, JavaUtility.callPinpointHarnessEnd)
                       },
                       classpathURLs,
                       measurer.measure(benchmark.info, _))
    // format: ON

  }

}
