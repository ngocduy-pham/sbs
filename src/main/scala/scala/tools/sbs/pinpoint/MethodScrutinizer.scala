/*
 * MethodScrutinizer
 * 
 * Version
 * 
 * Created on October 13th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import scala.tools.nsc.io.Path.string2path
import scala.tools.sbs.io.Log
import scala.tools.sbs.pinpoint.finder.BinaryWrapper
import scala.tools.sbs.pinpoint.finder.DiggingWrapper
import scala.tools.sbs.pinpoint.finder.FinderFactory

class MethodScrutinizer(val config: Config, val log: Log) extends Scrutinizer with Configured {

  def scrutinize(benchmark: PinpointBenchmark.Benchmark): ScrutinyResult = {

    val instrumentedPath = config.bin / ".instrumented" createDirectory ()
    val storage = config.bin / ".backup" createDirectory ()

    val detector = ScrutinyRegressionDetectorFactory(config, log, benchmark, instrumentedPath, storage)

    detector detect benchmark match {
      case regressionSuccess: ScrutinyCIRegressionSuccess => regressionSuccess
      case regressionFailure: ScrutinyCIRegressionFailure if (config.pinpointBottleneckDectect) =>
        try {
          FinderFactory(
            config,
            log,
            benchmark,
            benchmark.className,
            benchmark.methodName,
            instrumentedPath,
            storage) find ()
        }
        catch {
          case _: MismatchExpressionList => regressionFailure
        }
      case anythingelse => anythingelse
    }
  }

}
