/*
 * PerformanceBenchmark
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import java.lang.reflect.Method
import java.net.URL

import scala.tools.nsc.io.Path
import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.io.Log

trait PerfBenchmark extends BenchmarkBase {

  import PerfBenchmark.Benchmark

  type subBenchmark     <: Benchmark
  type subSnippet       <: subBenchmark
  type subInitializable <: subBenchmark

  class Snippet(name: String,
                arguments: List[String],
                classpathURLs: List[URL],
                src: Path,
                timeout: Int,
                method: Method,
                context: ClassLoader,
                config: Config,
                val multiplier: Int,
                val measurement: Int,
                val sampleNumber: Int)
    extends super.Snippet(name, arguments, classpathURLs, src, timeout, context, method, config)
    with Benchmark

  class Initializable(name: String,
                      classpathURLs: List[URL],
                      src: Path,
                      benchmarkObject: PerfTemplate,
                      context: ClassLoader,
                      config: Config)
    extends super.Initializable(name, classpathURLs, src, context, benchmarkObject, config)
    with Benchmark {

    val multiplier   = 1
    val measurement  = benchmarkObject.measurement
    val sampleNumber = benchmarkObject.sampleNumber

  }

  val multiplierOpt  = "multiplier"
  val measurementOpt = "measurement"
  val sampleOpt      = "sample"

}

object PerfBenchmark extends PerfBenchmark {

  type subBenchmark     = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory

  trait Benchmark extends BenchmarkBase.Benchmark {

    def multiplier: Int
    def measurement: Int
    def sampleNumber: Int

  }

  def factory(log: Log, config: Config) = new Factory with Configured {

    val log: Log       = log
    val config: Config = config

    def createFrom(info: BenchmarkInfo): Benchmark = {
      val argMap = BenchmarkInfo.readInfo(info.src, List(multiplierOpt, measurementOpt, sampleOpt) map toOption)
      load(
        info,
        (method: Method, context: ClassLoader) => new Snippet(
          info.name,
          info.arguments,
          info.classpathURLs,
          info.src,
          info.timeout,
          method,
          context,
          config,
          getIntoOrElse(argMap get multiplierOpt, stringToInt, config.multiplier),
          getIntoOrElse(argMap get measurementOpt, stringToInt, config.measurement),
          getIntoOrElse(argMap get sampleOpt, stringToInt, 0)),
        (context: ClassLoader) => new Initializable(
          info.name,
          info.classpathURLs,
          info.src,
          Reflection(config, log).getObject[PerfTemplate](info.name, config.classpathURLs ++ info.classpathURLs),
          context,
          config),
        classOf[PerfTemplate].getName)
    }

  }

}
