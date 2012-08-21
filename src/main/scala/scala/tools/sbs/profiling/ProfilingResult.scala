/*
 * ProfilingResult
 * 
 * Version
 * 
 * Created on October 5th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import scala.collection.mutable.ArrayBuffer
import scala.tools.sbs.benchmark.BenchmarkInfo

/** A {@link RunResult} from a running of {@link Runner}.
  * In the mean time, also a {@link BenchmarkResult} for reporting.
  */
trait ProfilingResult extends BenchmarkResult

case class ProfilingSuccess(info: BenchmarkInfo, profile: Profile)
  extends BenchmarkSuccess with ProfilingResult {

  def benchmarkName = info.name

  def toReport = profile.toReport

}

trait ProfilingFailure extends BenchmarkFailure with ProfilingResult

class ProfilingException(val info: BenchmarkInfo, exception: Exception)
  extends ExceptionBenchmarkFailure(info.name, exception) with ProfilingFailure

object ProfilingException {

  def apply(info: BenchmarkInfo, exception: Exception) = new ProfilingException(info, exception)

  def unapply(pe: ProfilingException): Option[(BenchmarkInfo, Exception)] =
    Some((pe.info, pe.exception))

}
