package scala.tools.sbs

import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Directory
import scala.tools.sbs.io.LogFactory
import scala.tools.sbs.io.UI
import scala.tools.sbs.performance.MeasurementSuccess
import scala.tools.sbs.performance.Series
import scala.tools.sbs.performance.MeasurementSuccess

package object test {

  val testDir = Directory("D:/Enjoy/Scala/sbs/benchmark/test").createDirectory()
  val args = Array(
    "--benchmarkdir",
    testDir.path,
    "--multiplier",
    "11",
    "--measurement",
    "31",
    "--verbose",
    "--steady-performance",
    "test.Benchmark")
  val testConfig = new Config(args)
  UI.config = testConfig

  val testLog = LogFactory(testConfig)

  val about5k5Arr = ArrayBuffer[Long](5527, 5549, 5601, 5566, 5481, 5487, 5547, 5484, 5542, 5485, 5587)
  val about1kArr1 = ArrayBuffer[Long](1054, 1044, 1043, 1045, 1045, 1046, 1066, 1048, 1051, 1050, 1050)
  val about1kArr2 = ArrayBuffer[Long](1050, 1048, 1044, 1045, 1044, 1049, 1053, 1051, 1048, 1052, 1102)
  val about1kArr3 = ArrayBuffer[Long](1059, 1045, 1052, 1043, 1046, 1049, 1066, 1044, 1046, 1047, 1058)

  val about5k5Series = new Series(testConfig, testLog, about5k5Arr, 99)
  val about1kSeries1 = new Series(testConfig, testLog, about1kArr1, 99)
  val about1kSeries2 = new Series(testConfig, testLog, about1kArr2, 99)
  val about1kSeries3 = new Series(testConfig, testLog, about1kArr3, 99)

  val success1k1 = MeasurementSuccess(about1kSeries1)
  val success1k2 = MeasurementSuccess(about1kSeries2)
  val success1k3 = MeasurementSuccess(about1kSeries3)
  val success5k5 = MeasurementSuccess(about5k5Series)

}
