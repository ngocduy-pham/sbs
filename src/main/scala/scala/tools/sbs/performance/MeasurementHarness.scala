/*
 * MeasurementHarness
 * 
 * Version
 * 
 * Created on September 21st, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import scala.tools.sbs.common.ObjectHarness
import scala.tools.sbs.io.Log
import scala.tools.sbs.io.UI
import scala.xml.XML

/** Driver for measurement in a separated JVM.
  * Choose the harness to run and write the result to output stream.
  */
trait MeasurementHarness extends ObjectHarness {
  self: PerfBenchmark with Configured =>

  private[this] var _log: Log = _
  def log: Log = _log

  protected var seriesAchiever: SeriesAchiever[BenchmarkType] = _

  private[this] var _config: Config = _
  def config: Config = _config

  val mode: Mode

  /** Entry point of the new process.
    */
  def main(args: Array[String]): Unit = {
    _config = Config(args.tail.array)
    UI.config = config
    factory expand (XML loadString args.head) foreach (benchmark => {
      _log = benchmark createLog mode
      seriesAchiever = new SeriesAchiever(config, log)
      try reportResult(this measure benchmark.asInstanceOf[BenchmarkType])
      catch { case e: Exception => reportResult(new ExceptionMeasurementFailure(e)) }
    })
  }

  def measure(benchmark: BenchmarkType): MeasurementResult

}

trait MeasurementHarnessFactory {

  def apply(mode: Mode): MeasurementHarness

}

/** Factory object of {@link SubProcessMeasurer}.
  */
object MeasurementHarnessFactory extends MeasurementHarnessFactory {

  def apply(mode: Mode): MeasurementHarness = mode match {
    case SteadyState => SteadyHarness
    case MemoryUsage => MemoryHarness
    case _           => throw new AlgorithmFlowException(this.getClass)
  }

}
