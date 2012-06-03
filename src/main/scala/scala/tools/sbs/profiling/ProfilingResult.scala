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
 *  In the mean time, also a {@link BenchmarkResult} for reporting.
 */
trait ProfilingResult extends BenchmarkResult

case class ProfilingSuccess(benchmark: Benchmark, profile: Profile)
  extends BenchmarkSuccess with ProfilingResult {

  def benchmarkName = benchmark.name

  def toReport =
    (profile.classes flatMap (_.toReport)) ++
      (if (profile.steps > 0) ArrayBuffer("  All steps performed: " + profile.steps) else Nil) ++
      (if (profile.boxing > 0) ArrayBuffer("  Boxing: " + profile.boxing) else Nil) ++
      (if (profile.unboxing > 0) ArrayBuffer("  Unboxing: " + profile.boxing) else Nil) ++
      (if (profile.memoryActivity != null) profile.memoryActivity.toReport else Nil)

}

trait ProfilingFailure extends BenchmarkFailure with ProfilingResult

class ProfilingException(_benchmark: Benchmark, exception: Exception)
  extends ExceptionBenchmarkFailure(_benchmark.name, exception)
  with ProfilingFailure {

  def benchmark = _benchmark

}

object ProfilingException {

  def apply(benchmark: Benchmark, exception: Exception) = new ProfilingException(benchmark, exception)

  def unapply(pe: ProfilingException): Option[(Benchmark, Exception)] =
    Some((pe.benchmark, pe.exception))

}
