/*
 * FinderFactory
 * 
 * Version
 * 
 * Created on October 28th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package finder

import scala.tools.nsc.io.Directory
import scala.tools.sbs.io.Log

trait FinderFactory extends DiggingWrapper with BinaryWrapper {

  def apply(config: Config,
            log: Log,
            benchmark: PinpointBenchmark.Benchmark,
            className: String,
            methodName: String,
            instrumentedPath: Directory,
            storagePath: Directory): RegressionFinder =
    new DiggingFinder(
      config: Config,
      log,
      benchmark,
      className,
      methodName,
      instrumentedPath,
      storagePath)

  def apply(config: Config,
            log: Log,
            benchmark: PinpointBenchmark.Benchmark,
            className: String,
            methodName: String,
            instrumentedPath: Directory,
            storagePath: Directory,
            graph: InvocationGraph): RegressionFinder =
    new BinaryFinder(
      config: Config,
      log,
      benchmark,
      className,
      methodName,
      instrumentedPath,
      storagePath,
      graph)

}

object FinderFactory extends FinderFactory
