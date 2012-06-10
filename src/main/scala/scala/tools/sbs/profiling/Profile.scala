/*
 * Profile
 * 
 * Version
 * 
 * Created on October 2nd, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import scala.collection.mutable.ArrayBuffer

/** Class holds the profiling result.
  */
class Profile {

  /** All classes loaded in a benchmark running.
    */
  val classes = ArrayBuffer[LoadedClass]()

  def loadClass(name: String) {
    classes += LoadedClass(name)
  }

  def loadClass(clazz: LoadedClass) {
    classes += clazz
  }

  /** Number of boxing.
    */
  private var boxing = 0

  def box {
    boxing += 1
  }

  /** Number of unboxing.
    */
  private var unboxing = 0

  def unbox {
    unboxing += 1
  }
  /** Number of steps performed.
    */
  private var steps = 0

  def performStep {
    steps += 1
  }

  private var memoryActivity: MemoryActivity = _

  def useMemory(usage: MemoryActivity) {
    memoryActivity = usage
  }

  def toReport =
    (classes flatMap (_.toReport)) ++
      (if (steps > 0) ArrayBuffer("  all steps performed: " + steps) else Nil) ++
      (if (boxing > 0) ArrayBuffer("  boxing: " + boxing) else Nil) ++
      (if (unboxing > 0) ArrayBuffer("  unboxing: " + boxing) else Nil) ++
      (if (memoryActivity != null) memoryActivity.toReport else Nil)

  def toXML =
    <profile>
      <classes>{ for (clazz <- classes) yield clazz.toXML }</classes>
      <steps>{ steps }</steps>
      <boxing>{ boxing }</boxing>
      <unboxing>{ unboxing }</unboxing>
      <memoryUsage>{ memoryActivity.toXML }</memoryUsage>
    </profile>

}
