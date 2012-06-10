/*
 * ProfilingBenchmarkTemplate
 * 
 * Version
 * 
 * Created on October 31th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import scala.tools.sbs.benchmark.Template

trait ProfTemplate extends Template {

  val classes: List[String] = Nil

  val exclude: List[String] = Nil

  val methodName = ""

  val fieldName = ""

}
