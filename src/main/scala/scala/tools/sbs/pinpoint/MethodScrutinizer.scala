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
import scala.tools.sbs.pinpoint.finder.DiggingRegressionFinder

case class MethodScrutinizer(config: Config, log: Log)
  extends Scrutinizer
  with MethodRegressionDetector
  with DiggingRegressionFinder
  with PinpointBenchmarkCreator
  with Configured {

  // format: OFF
  type FinderType   = Finder
  type DetectorType = Detector

  val instrumentedPath = (config.bin / ".instrumented").createDirectory()
  val storagePath      = (config.bin / ".backup").createDirectory()

  override def run(benchmark: BenchmarkType)      = super[Scrutinizer].run(benchmark)
  override def generate(benchmark: BenchmarkType) = super[Scrutinizer].generate(benchmark)
  // format: ON

  def scrutinize(benchmark: BenchmarkType): ScrutinyResult =
    regressionDetect(benchmark) match {
      case regressionSuccess: ScrutinyCIRegressionSuccess => regressionSuccess
      case regressionFailure: ScrutinyCIRegressionFailure if (config.pinpointBottleneckDectect) =>
        try { regressionFind(benchmark) }
        catch { case _: MismatchExpressionList => regressionFailure }
      case anythingelse => anythingelse
    }

  def regressionFinder(pinpointBenchmark: BenchmarkType): FinderType =
    new Finder { val benchmark = pinpointBenchmark }

  def regressionDetector(pinpointBenchmark: BenchmarkType): DetectorType =
    new Detector { val benchmark = pinpointBenchmark }

}
