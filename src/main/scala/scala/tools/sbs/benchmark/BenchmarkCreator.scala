/*
 * BenchmarkCreator
 *
 * Version
 *
 * Created on August 21st, 2012
 *
 * Created by ND P
 */

package scala.tools.sbs
package benchmark

import java.lang.reflect.Method

import scala.tools.sbs.common.Reflection

trait BenchmarkCreator extends BenchmarkBase {
  self: Configured =>

  // format: OFF
  type BenchmarkType    = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory
  // format: ON

  val factory: subFactory = new Factory {

    // format: OFF
    def expand(info: BenchmarkInfo): Option[BenchmarkType] =
      load(info,
           (method: Method, context: ClassLoader) => new Snippet(info, context, method, config),
           (context: ClassLoader) => new Initializable(info,
                                                       context,
                                                       Reflection(config, log).getObject[Template](info.name, config.classpathURLs ++ info.classpathURLs),
                                                       config),
           classOf[Template].getName)
    // format: ON

  }

}
