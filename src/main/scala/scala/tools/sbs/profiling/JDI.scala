/*
 * JDI
 * 
 * Version
 * 
 * Created on June 7th, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import java.util.{ Map => JMap }

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.tools.sbs.common.JVMInvoker

import com.sun.jdi.connect.Connector
import com.sun.jdi.connect.LaunchingConnector
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine

trait JDI {
  self: Configured =>

  /** Java Debug Interface based implement of {@link Profiler}.
    */
  trait Launcher {

    val connectorName = "com.sun.jdi.CommandLineLaunch"

    def launch(benchmark: ProfBenchmark.Benchmark): VirtualMachine = {
      val javaArgument =
        JVMInvoker(log, config).asJavaArgument(benchmark, config.classpathURLs ++ benchmark.classpathURLs)

      log.debug("profile command: " + (javaArgument mkString " "))

      val connector =
        Bootstrap.virtualMachineManager.allConnectors.asScala.toSeq find (_.name equals connectorName) match {
          case Some(cnt) => cnt.asInstanceOf[LaunchingConnector]
          case None      => throw new Exception("no launching connector")
        }
      connector launch connectorArguments(connector, javaArgument mkString " ")
    }

    /** Return the launching connector's arguments.
      */
    def connectorArguments(connector: LaunchingConnector, mainArgs: String): JMap[String, Connector.Argument] = {
      val arguments = connector.defaultArguments

      val mainArg = arguments get "main"
      if (mainArg == null) {
        throw new Exception("bad launching connector")
      }
      mainArg setValue mainArgs

      // We need a VM that supports watch points
      val optionArg = arguments get "options"
      if (optionArg == null) {
        throw new Exception("bad launching connector")
      }
      optionArg setValue "-classic"

      arguments
    }

  }

  trait Handler[T] {

    /** Connected to target JVM.
      */
    protected var connected = true

    def handle(jvm: VirtualMachine): T

    /** A VMDisconnectedException has happened while dealing with another event.
      * We need to flush the event queue, dealing only with exit events (VMDeath,
      * VMDisconnect) so that we terminate correctly.
      */
    def handleDisconnectedException(jvm: VirtualMachine) {
      val queue = jvm.eventQueue
      while (connected) {
        try {
          val eventSet = queue.remove
          val iter = eventSet.eventIterator
          while (iter hasNext) {
            iter.nextEvent match {
              case vde: VMDeathEvent => {
                log.info("target JVM exited")
              }
              case vde: VMDisconnectEvent => {
                vmDisconnectEvent()
              }
            }
          }
          eventSet.resume
        }
        catch { case _: InterruptedException => () }
      }
    }

    protected def vmDisconnectEvent() {
      connected = false
      log.info("target JVM disconnected")
    }

  }

}
