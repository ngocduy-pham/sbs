/*
 * JDIProfiler
 * 
 * Version
 * 
 * Created on June 7th, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import java.io.IOException

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack
import scala.tools.sbs.io.Log

import com.sun.jdi.event.AccessWatchpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.ClassUnloadEvent
import com.sun.jdi.event.Event
import com.sun.jdi.event.ExceptionEvent
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.event.ModificationWatchpointEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.event.ThreadDeathEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.event.VMStartEvent
import com.sun.jdi.request.DuplicateRequestException
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.StepRequest
import com.sun.jdi.ThreadReference
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine

import ProfBenchmark.Benchmark

class JDIProfiler(val config: Config, val log: Log) extends Profiler with JDI with Configured {

  protected def profile(benchmark: ProfBenchmark.Benchmark): ProfilingResult = {

    val jvm = new Launcher {} launch benchmark

    def reportException(exc: Exception): ProfilingException = {
      log.error(exc.toString)
      log.error(exc.getStackTraceString)
      jvm.exit(1)
      ProfilingException(benchmark, exc)
    }

    try {
      val profile =
        if (benchmark.methodName.isEmpty && benchmark.fieldName.isEmpty && !config.shouldGC && !config.shouldBoxing) {
          log.debug("no JDI profiling")
          new Profile
        }
        else {
          new Handler(benchmark) handle jvm
        }

      if (config.shouldGC) {
        new MemoryProfiler(log, config).profile(benchmark, profile)
      }
      else {
        ProfilingSuccess(benchmark, profile)
      }
    }
    catch {
      case exc: IOException => reportException(new IOException("unable to launch target VM: " + exc))
      case exc: Exception   => reportException(exc)
    }
  }

  class Handler(benchmark: ProfBenchmark.Benchmark) extends super.Handler[Profile] {

    val primitives =
      List("scala.Int", "scala.Short", "scala.Long", "scala.Double",
        "scala.Float", "scala.Boolean", "scala.Byte", "scala.Char")
    val predefBoxes =
      List("byte2Byte", "short2Short", "char2Character", "int2Integer",
        "long2Long", "float2Float", "double2Double", "boolean2Boolean")
    val predefUnboxes =
      List("Byte2byte", "Short2short", "Character2char", "Integer2int",
        "Long2long", "Float2float", "Double2double", "Boolean2boolean")
    val runtimeBoxes =
      List("boxToBoolean", "boxToCharacter", "boxToByte", "boxToShort",
        "boxToInteger", "boxToLong", "boxToFloat", "boxToDouble")
    val runtimeUnboxes =
      List("unboxToBoolean", "unboxToChar", "unboxToByte", "unboxToShort",
        "unboxToInt", "unboxToLong", "unboxToFloat", "unboxToDouble")
    val boxers =
      (predefBoxes map ("scala.Predef." + _)) ++
        (primitives map (_ + "box")) ++
        (runtimeBoxes map ("scala.runtime.BoxesRunTime" + _))
    val unboxers =
      (predefUnboxes map ("scala.Predef." + _)) ++
        (primitives map (_ + "unbox")) ++
        (runtimeUnboxes map ("scala.runtime.BoxesRunTime" + _))

    /** This class keeps context on events in one thread.
      */
    class JDIThreadTrace(profile: Profile, thread: ThreadReference, jvm: VirtualMachine) {

      /** Instruction steps of methods in call stack.
        */
      private val steps = Stack[Int]()

      /** Convert the `com.sun.jdi.Method` name into the format of <class>.<method>
        */
      private def canonicalName(method: com.sun.jdi.Method) =
        method.declaringType.name + "." + method.name

      /** Convert the `com.sun.jdi.Method` name into the format of <class>.<method><signature>
        */
      private def wrapName(method: com.sun.jdi.Method) =
        canonicalName(method) + method.signature

      /** Push new method on the call stack.
        */
      def methodEntryEvent(event: MethodEntryEvent) {

        log.verbose("enter " + wrapName(event.method))

        steps push 0

        if (boxers contains canonicalName(event.method)) {
          profile.box
        }
        else if (unboxers contains canonicalName(event.method)) {
          profile.unbox
        }
      }

      /** Tries to update the method's invocation in profile.
        * Adds the method and/or its declaring class into profile
        * if not existed in.
        */
      def methodExitEvent(event: MethodExitEvent) {
        if (!steps.isEmpty) {
          profile.classes find (_.name equals event.method.declaringType.name) match {
            case Some(clazz) => {
              clazz.methodInvoked find (_.name equals wrapName(event.method)) match {
                case Some(method) => {
                  method.hasInvoked(steps pop)
                }
                case None => {
                  val invoked = InvokedMethod(wrapName(event.method))
                  invoked.hasInvoked(steps pop)
                  clazz.invokeMethod(invoked)
                }
              }
            }
            case None => {
              val loaded = LoadedClass(event.method.declaringType.name)
              val invoked = InvokedMethod(wrapName(event.method))
              invoked.hasInvoked(steps pop)
              loaded.invokeMethod(invoked)
              profile.loadClass(loaded)
            }
          }
        }
      }

      /** Actually do the accessing and/or modifying.
        */
      private def accessOrModify(event: com.sun.jdi.event.WatchpointEvent) {
        profile.classes find (_.name equals event.field.declaringType.name) match {
          case Some(clazz) => {
            clazz.fields find (_.name equals event.field.name) match {
              case Some(field) => event match {
                case _: AccessWatchpointEvent       => field.access
                case _: ModificationWatchpointEvent => field.modify
              }
              case None => {
                val field = Field(event.field.name)
                event match {
                  case _: AccessWatchpointEvent       => field.access
                  case _: ModificationWatchpointEvent => field.modify
                }
                clazz.addField(field)
              }
            }
          }

          case None => {
            val loaded = LoadedClass(event.field.declaringType.name)
            val field = Field(event.field.name)
            event match {
              case _: AccessWatchpointEvent       => field.access
              case _: ModificationWatchpointEvent => field.modify
            }
            loaded.addField(field)
            profile.loadClass(loaded)
          }
        }
      }

      /** Tries to update the field's accessing times in profile.
        * Adds the field and/or its declaring class into profile
        * if not existed in.
        */
      def fieldAccessEvent(event: AccessWatchpointEvent) = accessOrModify(event)

      /** Tries to update the field's modifying times in profile.
        * Adds the field and/or its declaring class into profile
        * if not existed in.
        */
      def fieldModifyEvent(event: ModificationWatchpointEvent) = accessOrModify(event)

      def exceptionEvent(event: ExceptionEvent) {
        log.info("Exception: " + event.exception + " catch: " + event.catchLocation)
        jvm.exit(1)
        throw new Exception(event.exception + " catch: " + event.catchLocation)
      }

      def stepEvent(event: StepEvent) {
        //    if (!steps.isEmpty) {
        //      steps push (steps.pop + 1)
        //    }
        //    val mgr = jvm.eventRequestManager
        //    mgr deleteEventRequest event.request
        steps push (steps.pop + 1)
        profile performStep
      }

      def threadDeathEvent(event: ThreadDeathEvent) {
        log.info("--" + thread.name + " ends--")
      }

    }

    /** Maps ThreadReference to ThreadTrace instances.
      */
    private val traceMap = new HashMap[ThreadReference, JDIThreadTrace]()

    /** Run the event handling thread. As long as we are connected, get event
      * sets off the queue and dispatch the events within them.
      */
    def handle(jvm: VirtualMachine): Profile = {
      val profile = new Profile
      setEventRequests(jvm)
      val queue = jvm.eventQueue
      jvm.resume
      while (connected) {
        try {
          val eventSet = queue.remove()
          val it = eventSet.eventIterator
          while (it hasNext) {
            handleEvent(it nextEvent, jvm, profile)
          }
          eventSet resume
        }
        catch {
          case _: InterruptedException => ()
          case exc: VMDisconnectedException => {
            handleDisconnectedException(jvm)
            log.info("Disconnected exception")
          }
        }
      }
      profile
    }

    /** Create the desired event requests, and enable them so that we will get events.
      */
    private def setEventRequests(jvm: VirtualMachine) {
      val mgr = jvm.eventRequestManager

      // want all exceptions
      //    val excReq = mgr.createExceptionRequest(null, true, true)
      //    excReq setSuspendPolicy EventRequest.SUSPEND_ALL
      //    excReq enable

      val tdr = mgr.createThreadDeathRequest
      // Make sure we sync on thread death
      tdr setSuspendPolicy EventRequest.SUSPEND_ALL
      tdr enable

      benchmark.classes foreach (pattern => {
        val cpr = mgr.createClassPrepareRequest
        benchmark.exclude foreach (cpr addClassExclusionFilter _)
        cpr addClassFilter pattern
        cpr setSuspendPolicy EventRequest.SUSPEND_ALL
        cpr enable
      })

      if (config.shouldBoxing) ("scala.Predef" :: "scala.runtime.BoxesRunTime" :: primitives) foreach (boxingClass => {
        val boxingMethodRequest = mgr.createMethodEntryRequest
        boxingMethodRequest addClassFilter boxingClass
        boxingMethodRequest setSuspendPolicy EventRequest.SUSPEND_NONE
        boxingMethodRequest enable
      })
    }

    /** Dispatch incoming events
      */
    private def handleEvent(event: Event, jvm: VirtualMachine, profile: Profile) {

      /** Returns the JDIThreadTrace instance for the specified thread, creating one if needed.
        */
      def threadTrace(thread: ThreadReference): JDIThreadTrace = {
        traceMap.get(thread) match {
          case None => {
            val ret = new JDIThreadTrace(profile, thread, jvm)
            traceMap.put(thread, ret)
            ret
          }
          case Some(trace) => trace
        }
      }

      /** A new class has been loaded.
        * <ul>
        * <li>Test whether it is the benchmark main class
        * <li>If so, set event requests for fields, methods and steps
        * <li>Otherwise, add to profile
        * <ul>
        */
      def classPrepareEvent(event: ClassPrepareEvent) {
        log.verbose("Prepared " + event.referenceType)

        profile loadClass event.referenceType.name

        val mgr = jvm.eventRequestManager

        // Add watch point requests
        event.referenceType.visibleFields.asScala.toSeq find (_.name == benchmark.fieldName) match {
          case Some(field) => {
            val mwrReq = mgr createModificationWatchpointRequest field
            mwrReq setSuspendPolicy EventRequest.SUSPEND_NONE
            mwrReq enable

            val awrReq = mgr createAccessWatchpointRequest field
            awrReq setSuspendPolicy EventRequest.SUSPEND_NONE
            awrReq enable
          }
          case None => ()
        }

        // Add step request
        if (config.shouldStep) {
          try {
            val str = mgr.createStepRequest(event.thread, StepRequest.STEP_MIN, StepRequest.STEP_INTO)
            benchmark.exclude foreach (str addClassExclusionFilter _)
            str setSuspendPolicy EventRequest.SUSPEND_NONE
            str enable
          }
          catch { case _: DuplicateRequestException => () }
        }

        // Add method entry request
        val menr = mgr.createMethodEntryRequest
        menr addClassFilter event.referenceType.name
        menr setSuspendPolicy EventRequest.SUSPEND_NONE
        menr enable

        // Add method exit request
        val mexr = mgr.createMethodExitRequest
        mexr addClassFilter event.referenceType.name
        mexr setSuspendPolicy EventRequest.SUSPEND_NONE
        mexr enable
      }

      def classUnloadEvent(event: ClassUnloadEvent) {
        log.verbose("Unloaded " + event.className)
      }

      event match {
        case ee: ExceptionEvent => {
          threadTrace(ee.thread) exceptionEvent ee
        }
        case awe: AccessWatchpointEvent => {
          threadTrace(awe.thread) fieldAccessEvent awe
        }
        case mwe: ModificationWatchpointEvent => {
          threadTrace(mwe.thread) fieldModifyEvent mwe
        }
        case mee: MethodEntryEvent => {
          threadTrace(mee.thread) methodEntryEvent mee
        }
        case mee: MethodExitEvent => {
          threadTrace(mee.thread) methodExitEvent mee
        }
        case se: StepEvent => {
          threadTrace(se.thread) stepEvent se
        }
        case tde: ThreadDeathEvent => {
          threadTrace(tde.thread) threadDeathEvent tde
        }
        case cpe: ClassPrepareEvent => {
          classPrepareEvent(cpe)
        }
        case cue: ClassUnloadEvent => {
          classUnloadEvent(cue)
        }
        case vse: VMStartEvent => {
          log.info("--JVM Started--")
        }
        case vde: VMDeathEvent => {
          log.info("--Target JVM exited--")
        }
        case vde: VMDisconnectEvent => {
          vmDisconnectEvent
        }
        case _ => {
          // TODO
          throw new Error("Unexpected event type")
        }
      }
    }

  }

}
