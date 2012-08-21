/*
 * InstrumentationUtility
 * 
 * Version
 * 
 * Created on May 26th, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package strategy

import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.File
import scala.tools.sbs.common.Backuper

trait InstrumentationUtility {
  self: Configured =>

  val instrumentedPath: Directory
  val storagePath: Directory

  private[this] val backupers = ArrayBuffer[Backuper]()

  /** One new Backuper holds the information each time we do the backup-ing
    */
  def backup(files: List[File], location: Directory) = {
    backupers += Backuper(log, config, files, location, storagePath)
    backupers.last.backup()
  }

  /** Restore the last list of files
    */
  def restoreLast() {
    backupers.last.restore()
  }

  /** Restore all the files that have been moved to storage
    */
  def restoreAll() = {
    backupers foreach (_.restore())
    backupers.clear()
  }

}
