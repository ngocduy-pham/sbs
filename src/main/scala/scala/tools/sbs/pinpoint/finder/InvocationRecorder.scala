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
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.event.ThreadDeathEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.event.VMStartEvent
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.EventRequestManager
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

  def isMatchOK          = currentGraph matches previousGraph
  val currentGraph       = record(config.classpathURLs ++ benchmark.info.classpathURLs)
  lazy val previousGraph = exploit(
    benchmark.previous,
    benchmark.context,
    config.classpathURLs ++ benchmark.info.classpathURLs,
    record)

  private def record(classpathURLs: List[URL]): InvocationGraph = {
    val jvm = new Launcher {} launch benchmark

    def reportException(exc: Exception): InvocationGraph = {
      log.error(exc.toString)
      log.error(exc.getStackTraceString)
      jvm exit 1
      new InvocationGraph
    }

    if (benchmark.className.isEmpty || benchmark.methodName.isEmpty)
      new InvocationGraph
    else
      try new Handler(jvm.eventRequestManager) handle jvm
      catch {
        case exc: IOException => reportException(new IOException("unable to launch target VM: " + exc))
        case exc: Exception   => reportException(exc)
      }
  }

  class Handler(eventRequestManager: EventRequestManager) extends super.Handler[InvocationGraph] {

    /** Maps ThreadReference to ThreadTrace instances.
      */
    protected val traceMap = new HashMap[ThreadReference, ThreadTrace]()

    /** The resulting graph.
      */
    protected val graph = new InvocationGraph

    private val pinpointClassRequest  = eventRequestManager.createClassPrepareRequest
    private val pinpointMethodRequest = eventRequestManager.createMethodEntryRequest
    private val methodEntryRequest    = eventRequestManager.createMethodEntryRequest
    private val methodExitRequest     = eventRequestManager.createMethodExitRequest

    private var pinpointThread = None : Option[ThreadReference]
    private var layer          = 0

    /** This class keeps context on events in one thread.
      */
    class ThreadTrace(thread: ThreadReference) {

      def handle(event: Event) =
        event match {
          case cpe: ClassPrepareEvent => classPrepareEvent(cpe)
          case mee: MethodEntryEvent  => methodEntryEvent(mee)
          case mee: MethodExitEvent   => methodExitEvent(mee)
          case tde: ThreadDeathEvent  => threadDeathEvent(tde)
          case _                      => ()
        }

      def classPrepareEvent(event: ClassPrepareEvent) {
        log.verbose("prepare " + event.referenceType)
        eventRequestManager deleteEventRequest pinpointClassRequest

        pinpointMethodRequest setSuspendPolicy EventRequest.SUSPEND_ALL
        pinpointMethodRequest.enable
      }

      def methodEntryEvent(event: MethodEntryEvent) {
        val clazz = event.method.declaringType.name
        val method = event.method.name

        if (pinpointMethodRequest.isEnabled) {
          if ((method equals methodName) && (clazz equals className)) {
            log.verbose("enter inspected method " + clazz + "." + method)

            pinpointThread = Some(thread)

	        methodEntryRequest setSuspendPolicy EventRequest.SUSPEND_ALL
            methodEntryRequest.enable

            methodExitRequest setSuspendPolicy EventRequest.SUSPEND_ALL
            methodExitRequest.enable

	        pinpointMethodRequest.disable
	      }
        }
        else {
          layer += 1
          if (layer == 1) graph.add(clazz, method, event.method.signature)
          log.verbose("enter " + clazz + "." + method + " [layer: " + layer + "]")
        }
      }

      def methodExitEvent(event: MethodExitEvent) {
        log.verbose("exit  " + event.method.declaringType.name + "." + event.method.name + " [layer: " + layer + "]")
        if (layer == 0) {
          log.verbose("delete requests")
          eventRequestManager deleteEventRequest methodEntryRequest
          eventRequestManager deleteEventRequest methodExitRequest
        }
        else {
          layer -= 1
        }
      }

      def threadDeathEvent(event: ThreadDeathEvent) {
        log.info("" + thread.name + " ends")
      }

    }

    /** Run the event handling thread. As long as we are connected, get event
      * sets off the queue and dispatch the events within them.
      */
    def handle(jvm: VirtualMachine): InvocationGraph = {
      setInitRequests()
      jvm.resume
      val queue = jvm.eventQueue

      while (connected) {
        try {
          val eventSet      = queue.remove()
          val eventIterator = eventSet.eventIterator

          while (eventIterator hasNext) {
            handleEvent(eventIterator nextEvent)
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
    private def setInitRequests() {
      val tdr = eventRequestManager.createThreadDeathRequest
      tdr setSuspendPolicy EventRequest.SUSPEND_ALL
      tdr enable

      pinpointClassRequest addClassFilter benchmark.className
      pinpointClassRequest setSuspendPolicy EventRequest.SUSPEND_ALL
      pinpointClassRequest enable
    }

    /** Dispatch incoming events
      */
    private def handleEvent(event: Event) {
      def traceOrElse(event: Event { def thread(): ThreadReference }) {
        if (pinpointThread isDefined) {
          if (event.thread == pinpointThread.get) {
            (traceMap get pinpointThread.get get) handle event
          }
        }
        else {
          threadTrace(event.thread) handle event
        }
      }

      event match {
        case cpe: ClassPrepareEvent => threadTrace(cpe.thread) handle cpe
        case mee: MethodEntryEvent  => traceOrElse(mee)
        case mxe: MethodExitEvent   => traceOrElse(mxe)
        case tde: ThreadDeathEvent  => threadTrace(tde.thread) handle tde
        case vse: VMStartEvent      => log.info("JVM started")
        case vde: VMDeathEvent      => log.info("target JVM exited")
        case vde: VMDisconnectEvent => vmDisconnectEvent
        case _                      => throw new Error("unexpected event")
      }
    }

    /** Returns the JDIThreadTrace instance for the specified thread, creating one if needed.
      */
    def threadTrace(thread: ThreadReference): ThreadTrace =
      traceMap.getOrElseUpdate(thread, {
        log.verbose("<new thread>")
        new ThreadTrace(thread)
      })

  }

}
