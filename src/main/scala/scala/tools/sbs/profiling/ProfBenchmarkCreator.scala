/*
 * ProfBenchmarkCreator
 * 
 * Version
 * 
 * Created on August 17th, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import java.lang.reflect.Method

import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.Reflection

trait ProfBenchmarkCreator extends ProfBenchmark {
  self: Configured =>

  // format: OFF
  type BenchmarkType    = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory
  // format: ON

  val factory = new Factory {

    def expand(info: BenchmarkInfo): Option[BenchmarkType] = {
      import ProfBenchmark._
      val argMap = BenchmarkInfo.readInfo(
        argFromSrc(info.src),
        List(classesOpt, excludeOpt, methodNameOpt, fieldNameOpt) map toOption)
      load(
        info,
        (method: Method, context: ClassLoader) => new Snippet(
          info,
          getIntoOrElse(argMap get classesOpt, stringToList, config.classes),
          getIntoOrElse(argMap get excludeOpt, stringToList, config.exclude),
          argMap getOrElse (methodNameOpt, config.methodName),
          argMap getOrElse (fieldNameOpt, config.fieldName),
          method,
          context,
          config),
        (context: ClassLoader) => new Initializable(
          info,
          Reflection(config, log).getObject[ProfTemplate](info.name, config.classpathURLs ++ info.classpathURLs),
          context,
          config),
        classOf[ProfTemplate].getName)
    }

  }

}
