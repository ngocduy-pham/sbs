package scala.tools.sbs
package common

import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.io.UI
import scala.xml.XML

object RunOnlyHarness extends ObjectHarness {

  /** Entry point of the new process.
    */
  def main(args: Array[String]): Unit = {
    val config    = Config(args.tail.array)
    UI.config     = config
    val benchmark = BenchmarkBase.factory(UI, config) createFrom (XML loadString args.head)
    val log       = benchmark createLog DummyMode
    benchmark.run
  }

}
