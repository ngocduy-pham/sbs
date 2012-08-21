/*
 * BenchmarkDriver
 * 
 * Version 
 * 
 * Created on September 5th, 2011
 *
 * Created by ND P
 */

package scala.tools.sbs

import java.text.SimpleDateFormat
import java.util.Date

import scala.tools.nsc.io.Path.string2path
import scala.tools.sbs.common.BenchmarkCompiler
import scala.tools.sbs.io.ReportFactory
import scala.tools.sbs.io.UI
import scala.tools.sbs.util.FileUtil

/** Object controls the runtime of benchmark classes to do measurements.
  *
  * @author ND P
  */
object Driver {

  /** Start point of the benchmark driver.
    * Does the following:
    * <ul>
    * <li>Parse input parameters
    * <li>Compile the sources of the benchmarks if necessary
    * <li>Run all the benchmarks with the specified parameters
    * <li>Run comparisons to previous results
    * <li>Stores the benchmark result into file
    * </ul>
    */
  def main(args: Array[String]): Unit = try {

    val (config, log, benchmarkInfos) = ArgumentParser parse args

    if (config.isHelp) {
      println(config.helpMsg)
      System exit 0
    }
    log.debug(config.toString)

    // Clean up in case demanded
    if (config.isCleanup) {
      log.info("[Cleaning up]")
      for (each <- config.history.deepFiles) if (!(each hasExtension "scala") && !(each hasExtension "arg")) each.delete
    }

    val resultPack = new ResultPack

    log.info("[Compiling benchmarks]")
    val compiler = BenchmarkCompiler(log, config)

    val compiled = benchmarkInfos filter (_.isCompiledOK(compiler, config))

    log.debug(compiled.toString)

    // Add failure compiles for reporting
    benchmarkInfos filterNot compiled.contains foreach (_ foreach (failure => {
      if (config.isQuiet) notify(None, null, failure.name)
      resultPack add CompileBenchmarkFailure(failure.name)
    }))

    config.modes foreach (mode => {

      log.info("[Benchmarking mode: " + mode.description + "]")

      resultPack switchMode mode

      FileUtil.mkDir(config.benchmarkDirectory / mode.location) match {
        case Right(s) => log.error(s)
        case _        => ()
      }

      val runner = Runner(config, log, mode)
      log.debug("Runner: " + runner.getClass.getName)

      /*log.info("[Generating sample histories]")
      try benchmarks filter (_.sampleNumber > 0) foreach (runner generate _)
      catch {
        case e => log.debug(e.toString)
      }*/

      // Benchmarking
      log.info("[Start benchmarking]")

      compiled(mode) foreach (info => {
        log.info("Benchmark: " + info.name)
        try {
          val result = runner run info
          if (config.isQuiet) notify(result, mode, info.name)
          if (result.isDefined) resultPack add result.get
        }
        catch {
          case e: Exception => {
            log.info("[    Run FAILED    ]")
            resultPack add new ExceptionBenchmarkFailure(info.name, e)
          }
        }
      })

      if (!config.isNoCleanLog) {
        FileUtil.cleanLog(config.benchmarkDirectory / mode.location)
      }
    })
    overallReport(config, resultPack)
    ReportFactory(config)(resultPack)
  }
  catch {
    case e: Throwable => {
      UI.error(e.toString)
      UI.error(e.getStackTraceString)
      throw e
    }
  }

  def notify(each: Option[BenchmarkResult], mode: Mode, name: String) = {
    val last =
      if (each.isDefined) each.get match {
        case _: BenchmarkSuccess => "[  OK  ]"
        case _                   => "[FAILED]"
      }
      else { "[FAILED]" }
    val modename = if (mode == null) "compile" else mode.location
    System.out.format("%-10s | %-30s %10s\n", modename, name, last)
  }

  def overallReport(config: Config, pack: ResultPack) {
    println("========================================================================")
    println("Benchmarking date:   " + new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss").format(new Date))
    println("Directory:           " + config.benchmarkDirectory.path)
    println("Total benchmarks:    " + pack.total)
    println("OK:                  " + pack.ok)
    println("Failed:              " + pack.failed)
    println("========================================================================")
  }

}
