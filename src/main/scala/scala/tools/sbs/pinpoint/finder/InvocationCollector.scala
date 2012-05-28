/*
 * InvocationCollector
 * 
 * Version
 * 
 * Created on November 25th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package finder

import java.net.URL
import scala.tools.nsc.io.Directory
import scala.tools.sbs.common.JVMInvokerFactory
import scala.tools.sbs.common.RunOnlyHarness
import scala.tools.sbs.io.Log
import scala.tools.sbs.pinpoint.instrumentation.JavaUtility
import scala.tools.sbs.pinpoint.strategy.InstrumentationRunner
import scala.tools.sbs.pinpoint.strategy.PreviousVersionExploiter
import scala.tools.sbs.pinpoint.strategy.InstrumentationUtility

class InvocationCollector(val config: Config,
                          val log: Log,
                          val benchmark: PinpointBenchmark,
                          val className: String,
                          val methodName: String,
                          val instrumentedPath: Directory,
                          val storagePath: Directory)
  extends InstrumentationRunner
  with PreviousVersionExploiter
  with InstrumentationUtility 
  with Configured
   {

  def graph: InvocationGraph = currentGraph

  def isMatchOK = currentGraph matches previousGraph

  val currentGraph: InvocationGraph = collect(config.classpathURLs ++ benchmark.classpathURLs)

  val previousGraph: InvocationGraph = exploit(
    benchmark.pinpointPrevious,
    benchmark.context,
    config.classpathURLs ++ benchmark.classpathURLs,
    collect)

  private def collect(classpathURLs: List[URL]): InvocationGraph = {

    val graph = new InvocationGraph
    var finished = false
    var recursionDepth = 0

    def addToGraph(line: String) = try scala.xml.XML loadString line match {
      case <call><class>{ clazz }</class><method>{ method }</method><signature>{ signature }</signature></call> =>
        if (recursionDepth == 1 && !finished) graph.add(clazz.text, method.text, signature.text)
      case <entry/> => recursionDepth += 1
      case <exit/>  => recursionDepth -= 1; if (recursionDepth == 0) finished = true
      case _        => throw new Exception
    }
    catch { case _ => log(line) }

    def callNotifying(clazz: String, method: String, signature: String) =
      JavaUtility.javaSysout(JavaUtility.doubleQuote(
        "<call>" +
          "<class>" + clazz + "</class>" +
          "<method>" + method + "</method>" +
          "<signature>" + signature + "	</signature>" +
          "</call>"))

    val invoker = JVMInvokerFactory(log, config)

    instrumentAndRun(
      (method, instrumentor) => {
        instrumentor.notifyCallExpression(method, callNotifying)
        instrumentor.sandwich(
          method,
          JavaUtility.javaSysout(JavaUtility.doubleQuote("<entry/>")),
          JavaUtility.javaSysout(JavaUtility.doubleQuote("<exit/>")))
      },
      classpathURLs,
      cpURLs => invoker.invoke(
        invoker.command(RunOnlyHarness, benchmark, cpURLs),
        addToGraph,
        log.error,
        benchmark.timeout))

    graph
  }

}
