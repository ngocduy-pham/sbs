package scala.tools.sbs

import scala.collection.mutable.ArrayBuffer
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.util.Constant

trait BenchmarkResult {

  def benchmarkName: String

  def toReport: ArrayBuffer[String]

}

trait BenchmarkSuccess extends BenchmarkResult

trait BenchmarkFailure extends BenchmarkResult

case class CompileBenchmarkFailure(info: BenchmarkInfo) extends BenchmarkFailure {

  def benchmarkName = info.name

  def toReport = ArrayBuffer(Constant.INDENT + "Compiling benchmark failed")

}

case class ExceptionBenchmarkFailure(benchmarkName: String, exception: Exception) extends BenchmarkFailure {

  def toReport =
    ArrayBuffer((exception.toString split "\n" map (Constant.INDENT + _)) ++
      (exception.getStackTraceString split "\n" map (Constant.INDENT + "  " + _)): _*)

}

/** Holds all the benchmarking results from one sbs running.
  */
class ResultPack {

  private var modes = ArrayBuffer[ReportMode](new ReportMode(DummyMode))

  def switchMode(mode: Mode) = modes :+= new ReportMode(mode)

  private def currentMode = modes.last

  def add(newResult: BenchmarkResult) = currentMode add newResult

  def total = modes./:(0)((total, mode) => total + mode.results.length)

  def ok = success.length

  def failed = total - ok

  def foreach(mode: ReportMode => Unit) = modes foreach mode

  def success = modes./:(ArrayBuffer[BenchmarkResult]())((arr, mode) => arr ++ mode.success)

  def failure = modes./:(ArrayBuffer[BenchmarkResult]())((arr, mode) => arr ++ mode.failure)

}

class ReportMode(mode: Mode) {

  private var _results = ArrayBuffer[BenchmarkResult]()

  def add(newResult: BenchmarkResult) = _results += newResult

  def results = _results

  def foreach(f: BenchmarkResult => Unit) = results foreach f

  def success = results filterNot (failure contains _)

  def failure = results filter (_.isInstanceOf[BenchmarkFailure])

  def toReport = "[" + mode.description + "]"

}
