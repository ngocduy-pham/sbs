package scala.tools.sbs

import scala.tools.sbs.util.Constant

trait BenchmarkResult {

  def benchmarkName: String

  def toReport: List[String]

  def isSuccess: Boolean

}

trait BenchmarkSuccess extends BenchmarkResult {
 
  final def isSuccess = true

}

trait BenchmarkFailure extends BenchmarkResult {

  final def isSuccess = false

}

case class CompileBenchmarkFailure(benchmarkName: String) extends BenchmarkFailure {

  def toReport = List(Constant.INDENT + "Compiling benchmark failed")

}

case class ExceptionBenchmarkFailure(benchmarkName: String, exception: Exception) extends BenchmarkFailure {

  def toReport =
    List((exception.toString split "\n" map (Constant.INDENT + _)) ++
      (exception.getStackTraceString split "\n" map (Constant.INDENT + "  " + _)): _*)

}

/** Holds all the benchmarking results from one sbs running.
  */
class ResultPack {

  // format: OFF
  private var modes       = List(new ReportMode(DummyMode))
  private def currentMode = modes.last

  def switchMode(mode: Mode)            = modes :+= new ReportMode(mode)
  def add(newResult: BenchmarkResult)   = currentMode add newResult
  def foreach(mode: ReportMode => Unit) = modes foreach mode

  def total   = (0 /: modes)(_ + _.results.length)
  def ok      = success.length
  def failed  = total - ok
  def success = (List[BenchmarkResult]() /: modes)(_ ++ _.success)
  def failure = (List[BenchmarkResult]() /: modes)(_ ++ _.failure)
  // format: ON

}

class ReportMode(mode: Mode) {

  private var _results: List[BenchmarkResult] = Nil
  def results = _results

  def add(newResult: BenchmarkResult) = _results :+= newResult
  def foreach(f: BenchmarkResult => Unit) = results foreach f

  def success  = results filter (_ isSuccess)
  def failure  = results filterNot (_ isSuccess)
  def toReport = "[" + mode.description + "]"

}
