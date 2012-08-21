package scala.tools.sbs
package common

import scala.tools.sbs.benchmark.BenchmarkCreator
import scala.tools.sbs.io.UI
import scala.xml.XML

object RunOnlyHarness extends ObjectHarness {

  /** Entry point of the new process.
    */
  def main(args: Array[String]): Unit = {
    val recreatedConfig = Config(args.tail.array)
    UI.config = recreatedConfig
    new BenchmarkCreator with Configured {
      val log = UI
      val config = recreatedConfig
    }.factory expand (XML loadString args.head) foreach (benchmark => {
      benchmark createLog DummyMode
      benchmark.run
    })
  }

}
