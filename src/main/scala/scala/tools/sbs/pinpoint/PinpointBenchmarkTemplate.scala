/*
 * PinpointBenchmarkTemplate
 * 
 * Version
 * 
 * Created on October 31th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import scala.tools.nsc.io.Directory
import scala.tools.sbs.profiling.ProfBenchmarkTemplate
import scala.tools.sbs.performance.PerfBenchmarkTemplate

trait PinpointBenchmarkTemplate extends PerfBenchmarkTemplate with ProfBenchmarkTemplate {

  val previous = Directory(".pinpointprevious")

  val depth = -1

}
