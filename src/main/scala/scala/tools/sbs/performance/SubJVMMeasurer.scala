/*
 * SubJVMMeasurer
 * 
 * Version
 * 
 * Created September 25th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import java.net.URL

import scala.collection.mutable.ArrayBuffer
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.JVMInvoker
import scala.tools.sbs.io.Log

/** Measures benchmark metric by invoking a new clean JVM.
  */
trait SubJVMMeasurer extends PerformanceMeasurer {
  self: PerfBenchmark with Configured =>

  type MeasurerType <: Measurer

  def measurementHarness: MeasurementHarness

  trait Measurer extends super.Measurer {

    /** Measures with default classpath as `config.classpathURLs ++ benchmark.classpathURLs`.
      */
    def measure(benchmark: BenchmarkType): MeasurementResult =
      measure(benchmark.info, config.classpathURLs ++ benchmark.info.classpathURLs)

    /** Launches a new process with a {@link MeasurementHarness} runs a
      * {@link scala.tools.sbs.performance.PerformanceBenchmark}.
      * User classes will be loaded from the given `classpathURLs`.
      */
    def measure(info: BenchmarkInfo, classpathURLs: List[URL]): MeasurementResult = {
	  // format: OFF
	  val invoker         = JVMInvoker(log, config)
	  val (result, error) = invoker.invoke(invoker.command(measurementHarness, info, classpathURLs),
	                                       line => try scala.xml.XML loadString line
		      	  	                               catch { case _ => log(line); null },
										     line => line,
										     info.timeout)
      // format: ON
      if (error.length > 0) {
        error foreach log.error
        ExceptionMeasurementFailure(new Exception(error mkString "\n"))
      }
      else {
        dispose(result filter null.!= head, info, mode)
      }
    }

    /** Disposes a xml string to get the {@link MeasurementResult} it represents.
      *
      * @param result	A `String` contains and xml element.
      *
      * @return	The corresponding `MeasurementResult`
      */
    protected def dispose(result: scala.xml.Elem, info: BenchmarkInfo, mode: Mode): MeasurementResult =
      try {
        val xml = scala.xml.Utility trim result
        xml match {
          case <MeasurementSuccess>{ _ }</MeasurementSuccess> =>
            MeasurementSuccess(new Series(
              config,
              log,
              ArrayBuffer((xml \\ "value") map (_.text.toLong): _*),
              (xml \\ "confidenceLevel").text.toInt))
          case <UnwarmableMeasurementFailure/> =>
            new UnwarmableMeasurementFailure
          case <UnreliableMeasurementFailure/> =>
            new UnreliableMeasurementFailure
          case <ProcessMeasurementFailure>{ exitValue }</ProcessMeasurementFailure> =>
            new ProcessMeasurementFailure(exitValue.text.toInt)
          case <ExceptionMeasurementFailure>{ ect }</ExceptionMeasurementFailure> =>
            ExceptionMeasurementFailure(new Exception(ect.text))
          case <UnsupportedBenchmarkMeasurementFailure/> =>
            UnsupportedBenchmarkMeasurementFailure(info.name, mode)
          case _ =>
            new ProcessMeasurementFailure(0)
        }
      }
      catch {
        case _: NullPointerException => {
          log.error("Benchmarking timeout")
          new TimeoutMeasurementFailure
        }
        case e: org.xml.sax.SAXParseException => {
          log.error("Malformed XML: " + result)
          throw e
        }
        case e: Exception => {
          log.error("Malformed XML: " + result)
          throw new MalformedXMLException(self, mode, result)
        }
      }

  }

}
