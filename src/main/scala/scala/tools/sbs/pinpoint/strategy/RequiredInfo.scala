/*
 * PinpointInformation
 * 
 * Version
 * 
 * Created on May 27th, 2012
 * 
 * Created by ND P
  */
package scala.tools.sbs
package pinpoint
package strategy

import scala.tools.nsc.io.Directory

/** @author Sthy
  *
  */
trait RequiredInfo {

  val benchmark: PinpointBenchmark

  val className: String

  val methodName: String

  val instrumentedPath: Directory

  val storagePath: Directory

}
