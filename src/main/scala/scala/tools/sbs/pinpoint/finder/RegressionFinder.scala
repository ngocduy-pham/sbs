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

import scala.tools.nsc.io.Directory
import scala.tools.sbs.io.Log

/** Uses instrumentation method to point out the method call
  * that is a performance bottleneck in a given method.
  */
trait FinderWrapper {

  trait RegressionFinder {

    def find(): FindingResult

  }

  abstract class BasicFinder(val config: Config,
                             val log: Log,
                             val benchmark: PinpointBenchmark.Benchmark,
                             val className: String,
                             val methodName: String,
                             val instrumentedPath: Directory,
                             val storagePath: Directory)
    extends RegressionFinder

}
