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
import scala.tools.sbs.profiling.ProfTemplate
import scala.tools.sbs.performance.PerfTemplate

trait PinpointTemplate extends PerfTemplate with ProfTemplate {

  val className = ""
  val previous  = Directory(".pinpointprevious")
  val depth     = 1

}
