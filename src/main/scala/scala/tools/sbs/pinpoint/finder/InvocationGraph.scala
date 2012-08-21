/*
 * InvocationGraph
 * 
 * Version
 * 
 * Created on Novemeber 23rd, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package finder

import scala.collection.mutable.ListBuffer
import scala.tools.sbs.pinpoint.instrumentation.CodeInstrumentor

/** Represents a list of method call expressions inside a method body in order of time.
  *
  * `startOrdinum`: Property of the method starting `this` list of method call expressions.
  * It is the number that the starting method has been called at the start of the list.
  *
  * `endOrdinum`: Property of the method ending`this` list of method call expressions.
  * It is the number that the ending method has been called at the end of the list.
  */
case class InvocationGraph(methods: List[MethodCall], steps: Vector[Step], startOrdinum: Int, endOrdinum: Int) {

  def this() = this(Nil, Vector(), 0, 0)

  /** Number of method call expressions.
    */
  val length: Int = steps.length + 1

  /** The oldest method call expression in respect of time orders.
    */
  val first: MethodCall = if (steps isEmpty) methods.head else steps.head.from

  /** The latest method call expression in respect of time orders.
    */
  val last: MethodCall = if (steps isEmpty) methods.last else steps.last.to

  /** Traverses through all method call expressions in order of time.
    */
  def traverse[T](operate: MethodCall => T): List[T] =
    if (length == 0) Nil
    else {
      val result = ListBuffer(operate(first))
      steps foreach (step => result += operate(step.to))
      result.toList
    }

  /** Splits this graph into two new graphs which have equivalent lengths
    * in respect of time orders. The edge at the middle position will be
    * broken, leaving two halves of the list becoming the two new graphs.
    */
  def split: (InvocationGraph, InvocationGraph) =
    if (length < 2) throw new Error("Should not split anymore.")
    else {
      def attendIn(stepList: Vector[Step]): Option[List[MethodCall]] =
        if (stepList isEmpty) None
        else Some(methods filter (i => stepList exists (s => s.from == i || s.to == i)))

      // format: OFF
      val break        = steps.length / 2 // the edge at this index will be broken
      val firstSteps   = steps take break
      val lastSteps    = steps takeRight (steps.length - break - 1) // ignore `steps(break)`
      val firstMethods = attendIn(firstSteps) getOrElse List(first)
      val lastMethods  = attendIn(lastSteps)  getOrElse List(last)
      // format: ON
      (new InvocationGraph(firstMethods, firstSteps, startOrdinum, steps(break).fromOrdinum),
        new InvocationGraph(lastMethods, lastSteps, steps(break).toOrdinum, endOrdinum))
    }

  /** Checks whether this graph represents the same invocation list
    * with the given one.
    */
  def matches(that: InvocationGraph): Boolean =
    // format: OFF
    (startOrdinum                       == that.startOrdinum) &&
      (endOrdinum                       == that.endOrdinum) &&
      ((methods map (_ prototype))      == (that.methods map (_ prototype))) &&
      ((methods map (_ timesCalled))    == (that.methods map (_ timesCalled))) &&
      ((steps   map (_.from.prototype)) == (that.steps   map (_.from.prototype))) &&
      ((steps   map (_.to.prototype))   == (that.steps   map (_.to.prototype))) &&
      ((steps   map (_ fromOrdinum))    == (that.steps   map (_ fromOrdinum))) &&
      ((steps   map (_ toOrdinum))      == (that.steps   map (_ toOrdinum)))
  // format: ON

}

/** Builder for the graph.
  * Created once for collecting invocations.
  */
class GraphBuilder {

  private[this] val methods: ListBuffer[MethodCall] = ListBuffer()
  private[this] val steps: ListBuffer[Step] = ListBuffer()

  /** Adds new method call expression into this graph.
    *
    * @param	prottoype	Name and signature of the method has just been called.
    */
  def add(declaringClass: String, methodName: String, signature: String) {
    def addStep(to: MethodCall) {
      val from = if (steps isEmpty) methods.head else steps.last.to
      steps += Step(from, from.timesCalled, to, to.timesCalled)
    }
    val newCall = MethodCall(declaringClass, methodName, signature)
    methods find (_.prototype == newCall.prototype) match {
      case Some(existed) =>
        existed.calledAgain
        addStep(existed)
      case None =>
        methods += newCall
        if (methods.length > 1) addStep(methods.last)
    }
  }

  def result: InvocationGraph = new InvocationGraph(methods.toList, Vector() ++ steps, 1, steps.last.toOrdinum)

}

/** Method call expression - node of the graph.
  */
case class MethodCall(declaringClass: String, methodName: String, signature: String) {

  private[this] var _timesCalled = 1

  /** Number of times this method call expression has been run.
    */
  def timesCalled = _timesCalled

  /** This method should be called whenever the represented
    * method call expression is run.
    */
  def calledAgain = _timesCalled += 1

  /** Call expression identifier.
    */
  final val prototype: String = CodeInstrumentor.prototype(declaringClass, methodName, signature)

}

/** Vertex of the graph.
  */
case class Step(from: MethodCall, fromOrdinum: Int, to: MethodCall, toOrdinum: Int)


