/*
 * PinpointBenchmarkCreator
 * 
 * Version
 * 
 * Created on August 17th, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import java.lang.reflect.Method

import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path.string2path
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.performance.PerfBenchmark
import scala.tools.sbs.profiling.ProfBenchmark

trait PinpointBenchmarkCreator extends PinpointBenchmark {
  self: Configured =>

  // format: OFF
  type BenchmarkType    = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory
  // format: ON

  val factory: subFactory = new Factory {
    def expand(info: BenchmarkInfo): Option[BenchmarkType] = {
      import PinpointBenchmark._
      import PerfBenchmark._
      import ProfBenchmark._
      val argMap = BenchmarkInfo.readInfo(argFromSrc(info.src), List(
        classNameOpt,
        methodNameOpt,
        fieldNameOpt,
        excludeOpt,
        multiplierOpt,
        measurementOpt,
        sampleOpt,
        previousOpt,
        depthOpt))
      load(
        info,
        (method: Method, context: ClassLoader) => new Snippet(
          info,
          getIntoOrElse(argMap get multiplierOpt, stringToInt, config.multiplier),
          getIntoOrElse(argMap get measurementOpt, stringToInt, config.measurement),
          getIntoOrElse(argMap get sampleOpt, stringToInt, 0),
          argMap getOrElse (classNameOpt, config.className),
          argMap getOrElse (methodNameOpt, config.methodName),
          getIntoOrElse(argMap get excludeOpt, stringToList, config.exclude),
          getIntoOrElse(argMap get previousOpt, s => Directory(s), config.previous),
          getIntoOrElse(argMap get depthOpt, stringToInt, 1),
          method,
          context,
          config),
        (context: ClassLoader) => new Initializable(
          info,
          Reflection(config, log).getObject[PinpointTemplate](info.name, config.classpathURLs ++ info.classpathURLs),
          context,
          config),
        classOf[PinpointTemplate].getName)
    }

  }

}
