/*
 * DiggingRegressionFinder
 * 
 * Version
 * 
 * Created on August 20th, 2012
 * 
 * Created by ND P
 */
package scala.tools.sbs
package pinpoint
package finder

import scala.tools.sbs.pinpoint.instrumentation.CodeInstrumentor

trait DiggingRegressionFinder extends BinaryRegressionFinder {
  self: PinpointBenchmark with Configured =>

  type FinderType <: Finder

  trait Finder extends super.Finder {

    // TODO: issue with jar files:
    // - extracts
    // - backup

    override def run(): FindingResult =
      find(benchmark.className, benchmark.methodName, Nil)

    def find(inspectedClass: String,
             inspectedMethod: String,
             dug: List[String]): FindingResult = {

      // format: OFF
      val instrumentor  = CodeInstrumentor(config, log, benchmark.exclude)
      val currentMethod = instrumentor.getMethod(inspectedMethod,
                                                 inspectedClass,
                                                 config.classpathURLs ++ benchmark.info.classpathURLs)
      // format: ON

      log.info("finding Regression in: " + currentMethod.getLongName)
      log.info("")

      log.debug("binary finding")
      val currentLevelRegression = binaryFinder.find(inspectedClass, inspectedMethod)

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

            // format: OFF
            val lowerLevelRegressionFound = find(position.first.declaringClass,
                                                 position.first.methodName,
                                                 position.first.prototype :: dug)
            // format: ON

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

    def binaryFinder = new DiggingRegressionFinder.super[BinaryRegressionFinder].Finder {
      val benchmark: BenchmarkType = this.benchmark
    }

  }

}
