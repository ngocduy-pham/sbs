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

import ProfBenchmark.Benchmark

/** A {@link RunResult} from a running of {@link Runner}.
  * In the mean time, also a {@link BenchmarkResult} for reporting.
  */
trait ProfilingResult extends BenchmarkResult

case class ProfilingSuccess(benchmark: Benchmark, profile: Profile)
  extends BenchmarkSuccess with ProfilingResult {

  def benchmarkName = benchmark.info.name

  def toReport = profile.toReport

}

trait ProfilingFailure extends BenchmarkFailure with ProfilingResult

class ProfilingException(val benchmark: Benchmark, exception: Exception)
  extends ExceptionBenchmarkFailure(benchmark.info.name, exception) with ProfilingFailure

object ProfilingException {

  def apply(benchmark: Benchmark, exception: Exception) = new ProfilingException(benchmark, exception)

  def unapply(pe: ProfilingException): Option[(Benchmark, Exception)] =
    Some((pe.benchmark, pe.exception))

}
