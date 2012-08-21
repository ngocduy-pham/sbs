/*
 * PerfBenchmarkCreator
 * 
 * Version
 * 
 * Created on August 17th, 2012
 * 
 * Created by ND P
 * 
 */

package scala.tools.sbs
package performance

import java.lang.reflect.Method

import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.Reflection

trait PerfBenchmarkCreator extends PerfBenchmark {
  self: Configured =>

  // format: OFF
  type BenchmarkType    = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory
  // format: ON

  val factory: subFactory = new Factory {

    def expand(info: BenchmarkInfo): Option[BenchmarkType] = {
      import PerfBenchmark._
      val argMap = BenchmarkInfo.readInfo(
        argFromSrc(info.src),
        List(multiplierOpt, measurementOpt, sampleOpt) map toOption)
      load(
        info,
        (method: Method, context: ClassLoader) => new Snippet(
          info,
          method,
          context,
          config,
          getIntoOrElse(argMap get multiplierOpt, stringToInt, config.multiplier),
          getIntoOrElse(argMap get measurementOpt, stringToInt, config.measurement),
          getIntoOrElse(argMap get sampleOpt, stringToInt, 0)),
        (context: ClassLoader) => new Initializable(
          info,
          Reflection(config, log).getObject[PerfTemplate](info.name, config.classpathURLs ++ info.classpathURLs),
          context,
          config),
        classOf[PerfTemplate].getName)
    }

  }

}
