/*
 * PinpointingBenchmark
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import java.lang.reflect.Method
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Directory
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.io.Log
import scala.tools.sbs.performance.PerfBenchmark
import scala.tools.sbs.util.Constant
import scala.testing.Benchmark
import scala.tools.sbs.profiling.ProfBenchmark
import scala.tools.nsc.io.Path
import java.net.URL

trait PinpointBenchmark extends PerfBenchmark with ProfBenchmark {

  import PinpointBenchamrk.Benchmark

  type subBenchmark <: Benchmark

  type subSnippet <: Snippet with subBenchmark

  type subInitializable <: Initializable with subBenchmark

  class Snippet(name: String,
                arguments: List[String],
                classpathURLs: List[URL],
                src: Path,
                sampleNumber: Int,
                timeout: Int,
                multiplier: Int,
                measurement: Int,
                val className: String,
                val methodName: String,
                val exclude: List[String],
                val privious: Directory,
                val depth: Int,
                method: Method,
                context: ClassLoader,
                config: Config)
    extends super[PerfBenchmark].Snippet(
      name,
      arguments,
      classpathURLs,
      src,
      timeout,
      method,
      context,
      config,
      multiplier,
      measurement,
      sampleNumber)
    with Benchmark

  val classNameOpt = "classname"

  val previousOpt = "previous"

  val depthOpt = "depth"

}

object PinpointBenchmark extends PinpointBenchmark {

  type subBenchmark = Benchmark

  type subSnippet = Snippet

  type subInitializable = Initializable

  type subFactory = Factory

  trait Benchmark extends PerfBenchmark.Benchmark with ProfBenchmark.Benchmark {

    def classes: List[String] = Nil

    def className: String

    /** Location of the old class files to be used during pinpointing regression detection.
      * Should not be included in `Config.classpathURLs` and `Benchmark.classpathURLs`.
      */
    def previous: Directory

    /** Maximum recursion depth in the process of finding bottleneck.
      * Value of -1 is stand for unlimited depth.
      */
    def depth: Int

  }

  def factory(log: Log, config: Config) = new Factory with Configured {

    val log: Log = log

    val config: Config = config

    def createFrom(info: BenchmarkInfo): Benchmark = {
      val argMap = BenchmarkInfo.readInfo(info.src, List(
        classNameOpt,
        methodNameOpt,
        fieldNameOpt,
        excludeOpt,
        multiplierOpt,
        measurementOpt,
        sampleOpt,
        previousOpt,
        depthOpt))
      val className = argMap get classNameOpt match {
        case Some(arg) => arg split Constant.COLON toList
        case _         => config.className
      }
      val exclude = argMap get excludeOpt match {
        case Some(arg) => arg split Constant.COLON toList
        case _         => config.exclude
      }
      val methodName = argMap getOrElse (methodNameOpt, config.methodName)
      val fieldName = argMap getOrElse (fieldNameOpt, config.fieldName)
      val multiplier = getIntoOrElse(argMap get multiplierOpt, stringToInt, config.multiplier)
      val measurement = getIntoOrElse(argMap get measurementOpt, stringToInt, config.measurement)
      val sampleNumber = getIntoOrElse(argMap get sampleOpt, stringToInt, 0)
      load(
        info,
        (method: Method, context: ClassLoader) => new Snippet(
          info.name,
          info.arguments,
          info.classpathURLs,
          info.src,
          info.timeout,
          className,
          exclude,
          methodName,
          fieldName,
          method,
          context,
          config),
        (context: ClassLoader) => new Initializable(
          info.name,
          info.classpathURLs,
          info.src,
          Reflection(config, log).getObject[PinpointBenchmarkTemplate](
            info.name, config.classpathURLs ++ info.classpathURLs),
          context,
          config),
        classOf[PinpointBenchmarkTemplate].getName)
    }

  }

}
