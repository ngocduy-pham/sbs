/*
 * SubJVMPinpointMeasurer
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package strategy

import scala.tools.sbs.performance.SubJVMMeasurer

trait SubJVMPinpointMeasurer extends SubJVMMeasurer {
  self: PinpointBenchmark with Configured =>

  // format: OFF
  val mode               = Pinpointing
  val measurementHarness = PinpointHarness
  // format: ON

}
