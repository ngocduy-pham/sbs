/*
 * ProfBenchmark
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import java.lang.reflect.Method

import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.ObjectHarness
import scala.tools.sbs.common.RunOnlyHarness

trait ProfBenchmark extends BenchmarkBase {
  self: Configured =>

  // format: OFF
  type BenchmarkType    <: Benchmark
  type subSnippet       <: BenchmarkType
  type subInitializable <: BenchmarkType
  // format: ON

  trait Benchmark extends super.Benchmark {

    /** Names of the classes to be profiled the loading.
      */
    def classes: List[String]

    /** Names of the classes to be ignored from profiling.
      */
    def exclude: List[String]

    /** Name of the method to be profiled the invocations.
      */
    def methodName: String

    /** Name of the field to be profiled the accessing and modifying.
      */
    def fieldName: String

    /** How java runs this benchmark independently to sbs.
      * Snippet benchmarks can run on themselves with the method main.
      * Initializable benchmarks are loaded and run by an ObjectHarness.
      */
    def howToLaunch: Either[ObjectHarness, Benchmark]

  }

  class Snippet(info: BenchmarkInfo,
                val classes: List[String],
                val exclude: List[String],
                val methodName: String,
                val fieldName: String,
                method: Method,
                context: ClassLoader,
                config: Config)
    extends super.Snippet(info, context, method, config) with Benchmark {

    val howToLaunch = Right(this)

  }

  class Initializable(info: BenchmarkInfo,
                      benchmarkObject: ProfTemplate,
                      context: ClassLoader,
                      config: Config)
    extends super.Initializable(info, context, benchmarkObject, config) with Benchmark {

    // format: OFF
    val classes     = benchmarkObject.classes
    val exclude     = benchmarkObject.exclude
    val methodName  = benchmarkObject.methodName
    val fieldName   = benchmarkObject.fieldName
    val howToLaunch = Left(RunOnlyHarness)
    // format: ON

  }

}

object ProfBenchmark {

  // format: OFF
  val classesOpt    = "profile-class"
  val excludeOpt    = "exclude"
  val methodNameOpt = "methodname"
  val fieldNameOpt  = "fieldname"
  // format: ON

}
