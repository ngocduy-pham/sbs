/*
 * BenchmarkMode
 * 
 * Version
 * 
 * Created on September 17th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs

/** Benchmarking modes, includes:
 *  <ul>
 *  <li>Benchmarking in startup state
 *  <li>Benchmarking in steady state
 *  <li>Measuring memory usage
 *  <li>Profiling
 *  <li>Pinpointing
 *  </ul>
 */
trait Mode {

  /** Path from benchmark directory to save logs,
   *  and from history directory to save measurement results.
   */
  def location: String

  def description: String

}

/** Used for reporting error on compiling results and
 *  benchmarks that run independently from mode.
 */
object DummyMode extends Mode {

  val location    = "dummy"
  val description = "Just running"

  override val toString = "dummy"

}

object StartUpState extends Mode {

  val location    = "startup"
  val description = "Benchmarking performance in start-up state"

  override val toString = "startup"

}

object SteadyState extends Mode {

  val location    = "steady"
  val description = "Benchmarking performance in steady state"

  override val toString = "steady"

}

object MemoryUsage extends Mode {

  val location    = "memory"
  val description = "Benchmarking memory consumption in steady state"

  override val toString = "memory"
      
}

object Profiling extends Mode {

  val location    = "profile"
  val description = "Profiling metrics"

  override val toString = "profile"

}

object Pinpointing extends Mode {

  val location    = "pinpoint"
  val description = "Pinpointing regression detection"

  override val toString = "pinpoint"

}
