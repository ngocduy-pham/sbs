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

import java.io.IOException
import java.net.URL

import scala.collection.mutable.HashMap
import scala.tools.nsc.io.Directory
import scala.tools.sbs.io.Log
import scala.tools.sbs.pinpoint.strategy.PreviousVersionExploiter
import scala.tools.sbs.profiling.JDI

import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.Event
import com.sun.jdi.event.ExceptionEvent
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.event.ThreadDeathEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.event.VMStartEvent
import com.sun.jdi.request.EventRequest
import com.sun.jdi.ThreadReference
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine

/** @author ND P
  *
  */
class InvocationRecorder(val config: Config,
                         val log: Log,
                         val benchmark: PinpointBenchmark.Benchmark,
                         val instrumentedPath: Directory,
                         val storagePath: Directory)
  extends JDI
  with PreviousVersionExploiter
  with Configured {

  val className  = benchmark.className
  val methodName = benchmark.methodName

  def isMatchOK  = currentGraph matches previousGraph

  val currentGraph: InvocationGraph  = record(config.classpathURLs ++ benchmark.classpathURLs)

  lazy val previousGraph: InvocationGraph = exploit(
    benchmark.previous,
    benchmark.context,
    config.classpathURLs ++ benchmark.classpathURLs,
    record)

  private def record(classpathURLs: List[URL]): InvocationGraph = {
    val jvm = new Launcher {} launch benchmark

    def reportException(exc: Exception): InvocationGraph = {
      log.error(exc.toString)
      log.error(exc.getStackTraceString)
      jvm.exit(1)
      new InvocationGraph
    }

    if (benchmark.className.isEmpty || benchmark.methodName.isEmpty)
      new InvocationGraph
    else
      try Handler handle jvm
      catch {
        case exc: IOException => reportException(new IOException("unable to launch target VM: " + exc))
        case exc: Exception   => reportException(exc)
      }
  }

  object Handler extends super.Handler[InvocationGraph] {

    /** Maps ThreadReference to ThreadTrace instances.
      */
    protected val traceMap = new HashMap[ThreadReference, ThreadTrace]()

    /** The resulting graph.
      */
    protected val graph = new InvocationGraph

    /** This class keeps context on events in one thread.
      */
    class ThreadTrace(thread: ThreadReference, jvm: VirtualMachine) {

      private val eventRequestManager = jvm.eventRequestManager
      private val methodEntryRequest  = eventRequestManager.createMethodEntryRequest
      private val methodExitRequest   = eventRequestManager.createMethodExitRequest

      private var layer     = 0
      private var recording = false

      def classPrepareEvent(event: ClassPrepareEvent) {
        log.verbose("prepare " + event.referenceType)

        benchmark.exclude foreach (methodEntryRequest addClassExclusionFilter _)
//        methodEntryRequest setSuspendPolicy EventRequest.SUSPEND_NONE
        methodEntryRequest enable

        benchmark.exclude foreach (methodExitRequest addClassExclusionFilter _)
//        methodExitRequest setSuspendPolicy EventRequest.SUSPEND_NONE
        methodExitRequest enable
      }

      def methodEntryEvent(event: MethodEntryEvent) {
        val clazz  = event.method.declaringType.name
        val method = event.method.name

        log.verbose("enter " + clazz + "." + method + " [recording: " + recording + "]")
        if (recording) {
          if (layer == 0) {
            graph.add(clazz, method, event.method.signature)
          }
          layer += 1
        }
        else if ((method equals benchmark.methodName) && (clazz equals benchmark.className)) {
          recording = true
        }
      }

      def methodExitEvent(event: MethodExitEvent) {
        val clazz  = event.method.declaringType.name
        val method = event.method.name

        log.verbose("exit  " + clazz + "." + method)
        if (recording) {
          layer -= 1

          if ((layer == 0) && (method equals benchmark.methodName) && (clazz equals benchmark.className)) {
            recording = false
            // cancel all event requests
            eventRequestManager deleteEventRequest methodEntryRequest
            eventRequestManager deleteEventRequest methodExitRequest
          }
        }

      }

      def exceptionEvent(event: ExceptionEvent) {
        log.info("exception: " + event.exception + " catch: " + event.catchLocation)
        jvm.exit(1)
        throw new Exception(event.exception + " catch: " + event.catchLocation)
      }

      def threadDeathEvent(event: ThreadDeathEvent) {
        log.info("--" + thread.name + " ends--")
      }

    }

    /** Run the event handling thread. As long as we are connected, get event
      * sets off the queue and dispatch the events within them.
      */
    def handle(jvm: VirtualMachine): InvocationGraph = {
      setInitRequests(jvm)

      val queue = jvm.eventQueue

      jvm.resume

      while (connected) {
        try {
          val eventSet      = queue.remove()
          val eventIterator = eventSet.eventIterator

          while (eventIterator hasNext) {
            handleEvent(eventIterator nextEvent, jvm)
          }
          eventSet resume
        }
        catch {
          case _: InterruptedException => ()
          case exc: VMDisconnectedException => {
            handleDisconnectedException(jvm)
            log.info("disconnected exception")
          }
        }
      }

      return graph
    }

    /** Create the desired event requests, and enable them so that we will get events.
      */
    private def setInitRequests(jvm: VirtualMachine) {
      val manager = jvm.eventRequestManager

      val tdr = manager.createThreadDeathRequest
      tdr setSuspendPolicy EventRequest.SUSPEND_ALL
      tdr enable

      val cpr = manager.createClassPrepareRequest
      cpr addClassFilter benchmark.className
      cpr setSuspendPolicy EventRequest.SUSPEND_ALL
      cpr enable
    }

    /** Dispatch incoming events
      */
    private def handleEvent(event: Event, jvm: VirtualMachine): Unit =
      event match {
        case cpe: ClassPrepareEvent => threadTrace(cpe.thread, jvm) classPrepareEvent cpe
        case mee: MethodEntryEvent  => threadTrace(mee.thread, jvm) methodEntryEvent mee
        case mee: MethodExitEvent   => threadTrace(mee.thread, jvm) methodExitEvent mee
        case exe: ExceptionEvent    => threadTrace(exe.thread, jvm) exceptionEvent exe
        case tde: ThreadDeathEvent  => threadTrace(tde.thread, jvm) threadDeathEvent tde
        case vse: VMStartEvent      => log.info("JVM started")
        case vde: VMDeathEvent      => log.info("target JVM exited")
        case vde: VMDisconnectEvent => vmDisconnectEvent
        case _                      => throw new Error("unexpected event")
      }

    /** Returns the JDIThreadTrace instance for the specified thread, creating one if needed.
      */
    def threadTrace(thread: ThreadReference, jvm: VirtualMachine): ThreadTrace =
      traceMap get thread getOrElse {
        val ret = new ThreadTrace(thread, jvm)
        traceMap += (thread -> ret)
        ret
      }

  }

}
