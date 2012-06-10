/*
 * DiggingWrapper
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

import scala.tools.nsc.io.Directory
import scala.tools.sbs.io.Log
import scala.tools.sbs.pinpoint.instrumentation.CodeInstrumentor
import scala.tools.sbs.pinpoint.strategy.RequiredInfo

/** @author Sthy
  *
  */
trait DiggingWrapper extends FinderWrapper {
  self: FinderFactory =>

  class DiggingFinder(config: Config,
                      log: Log,
                      benchmark: PinpointBenchmark.Benchmark,
                      className: String,
                      methodName: String,
                      instrumentedPath: Directory,
                      storagePath: Directory)
    extends BasicFinder(
      config: Config,
      log,
      benchmark,
      className,
      methodName,
      instrumentedPath,
      storagePath)
    with RequiredInfo
    with Configured {

    def find() = find(className, methodName, List(className + methodName))

    // TODO: issue with jar files:
    // - extracts
    // - backup

    def find(declaringClass: String, diggingMethod: String, dug: List[String]): FindingResult = {

      val instrumentor = CodeInstrumentor(config, log, benchmark.exclude)

      val invocationCollector = new InvocationCollector(
        config,
        log,
        benchmark,
        declaringClass,
        diggingMethod,
        instrumentedPath,
        storagePath)

      val currentMethod = instrumentor.getMethod(
        diggingMethod,
        declaringClass,
        config.classpathURLs ++ benchmark.classpathURLs)

      log.info("Finding Regression in: " + currentMethod.getLongName)
      log.info("")

      if (invocationCollector.graph.length == 0) {
        log.info("  No detectable method call found")
        log.info("")
        throw new RegressionUndetectableException(declaringClass, diggingMethod, invocationCollector.graph)
      }

      log.debug("Not empty calling list from: " + currentMethod.getLongName)

      if (!invocationCollector.isMatchOK) {
        log.error("Mismatch expression lists, skip further detection")
        throw new MismatchExpressionList(declaringClass, diggingMethod, invocationCollector)
      }

      log.debug("Binary finding")
      val currentLevelRegression = FinderFactory(
        config,
        log,
        benchmark,
        declaringClass,
        diggingMethod,
        instrumentedPath,
        storagePath,
        invocationCollector.graph) find ()

      currentLevelRegression.toReport foreach (line => {
        log.info(line)
      })
      log.info("")

      currentLevelRegression match {
        case RegressionPoint(_, position, _, _, _) if ((position.length == 1) &&
          (shouldProceed(position.first.prototype, dug)) &&
          !(benchmark.exclude exists (position.first.declaringClass matches _))) =>
          try {
            log.verbose("  Digging into: " + position.first.prototype)

            val lowerLevelRegressionFound =
              find(
                position.first.declaringClass,
                position.first.methodName,
                dug :+ position.first.prototype)

            lowerLevelRegressionFound match {
              case _: NoRegression => currentLevelRegression
              case _               => lowerLevelRegressionFound
            }
          }
          catch {
            case e => {
              log.debug("Digging failed: " + e)
              currentLevelRegression
            }
          }

        case _ => currentLevelRegression
      }

    }

    def shouldProceed(prototype: String, dug: List[String]) =
      (benchmark.depth == -1) || (dug.length < benchmark.depth && !(dug contains prototype))

  }

}
