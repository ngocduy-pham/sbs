/*
 * InvocationRecorder
 * 
 * Version
 * 
 * Created on May 26th, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package finder

import java.net.URL
import scala.tools.sbs.common.JVMInvoker
import scala.tools.sbs.common.RunOnlyHarness
import scala.tools.sbs.pinpoint.instrumentation.JavaUtility
import scala.tools.sbs.pinpoint.strategy.InstrumentationRunner
import scala.tools.sbs.pinpoint.strategy.PreviousVersionExploiter
import scala.tools.sbs.pinpoint.strategy.RequiredInfo

/** @author ND P
  *
  */
trait InvocationRecorder {
  self: RequiredInfo with InstrumentationRunner with PreviousVersionExploiter with Configured =>

  def isMatchOK = currentGraph matches previousGraph

  val currentGraph: InvocationGraph = record(config.classpathURLs ++ benchmark.classpathURLs)

  val previousGraph: InvocationGraph = exploit(
    benchmark.previous,
    benchmark.context,
    config.classpathURLs ++ benchmark.classpathURLs,
    record)

  private def record(classpathURLs: List[URL]): InvocationGraph = {

    val graph = new InvocationGraph
    var finished = false
    var layerDepth = 0

    def addToGraph(line: String) = try scala.xml.XML loadString line match {
      case <call><class>{ clazz }</class><method>{ method }</method><signature>{ signature }</signature></call> =>
        if (layerDepth == 1 && !finished) graph.add(clazz.text, method.text, signature.text)
      case <entry/> => layerDepth += 1
      case <exit/>  => layerDepth -= 1; if (layerDepth == 0) finished = true
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

    val invoker = JVMInvoker(log, config)

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
