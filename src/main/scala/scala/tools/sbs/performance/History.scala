/*
 * History
 * 
 * Version
 * 
 * Created on September 25th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import scala.collection.generic.CanBuildFrom

trait History {

  type subHistorian <: Historian

  trait Historian {

    def add(ele: Series): subHistorian

    def append(tag: subHistorian): Unit

    def apply(i: Int): Series

    def foldLeft[B](z: B)(op: (B, Series) => B): B

    def head: Series

    def last: Series

    def tail: subHistorian

    def +:(elem: Series): subHistorian

    def length: Int

    def map[B, That](f: Series => B)(implicit bf: CanBuildFrom[List[Series], B, That]): That

    def foreach(f: Series => Unit): Unit

    def forall(op: Series => Boolean): Boolean

  }

  def apply(): subHistorian

}

object History extends History {

  type subHistorian = Historian

  /** An implement of {@link History}. Uses `List` to hold previous measurement data.
    */
  class ListBasedHistorian extends Historian {

    /** The {@link Series} list.
      */
    private var data = List[Series]()

    def this(data: List[Series]) {
      this
      this.data = data
    }

    /** Benchmarking mode of the history.
      */
    def mode(): Mode = mode

    /** Adds a `Series` to `data`.
      */
    def add(ele: Series) = {
      data :+= ele
      this
    }

    /** Appends a `History` to `this`.
      */
    def append(tag: subHistorian) {
      tag foreach (this add _)
      this
    }

    def apply(i: Int) = data(i)

    def foldLeft[B](z: B)(op: (B, Series) => B): B = data.foldLeft[B](z)(op)

    def head: Series = data.head

    def last = data.last

    def tail = new ListBasedHistorian(data.tail)

    def +:(elem: Series) = new ListBasedHistorian(data :+ elem)

    def length = data.length

    def map[B, That](f: Series => B)(implicit bf: CanBuildFrom[List[Series], B, That]): That = data map f

    def foreach(f: Series => Unit): Unit = data foreach f

    def forall(op: Series => Boolean) = data forall op

  }

  def apply(): subHistorian = new ListBasedHistorian

}
