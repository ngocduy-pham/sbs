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

  val location = "dummy"

  override val toString = "dummy"

  val description = "Just running"

}

object StartUpState extends Mode {

  val location = "startup"

  override val toString = "startup"

  val description = "Benchmarking performance in start-up state"

}

object SteadyState extends Mode {

  val location = "steady"

  override val toString = "steady"

  val description = "Benchmarking performance in steady state"

}

object MemoryUsage extends Mode {

  val location = "memory"

  override val toString = "memory"

  val description = "Benchmarking memory consumption in steady state"

}

object Profiling extends Mode {

  val location = "profile"

  override val toString = "profile"

  val description = "Profiling metrics"

}

object Pinpointing extends Mode {

  val location = "pinpoint"

  override val toString = "pinpoint"

  val description = "Pinpointing regression detection"

}
