/*
 * BenchmarkInfo
 * 
 * Version
 * 
 * Created on Octber 7th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package benchmark

import java.net.URL

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.io.Source
import scala.tools.nsc.io.Path
import scala.tools.sbs.common.BenchmarkCompiler
import scala.tools.sbs.io.UI

/** Contains information about the benchmark which include:
  * <ul> benchmark's name
  * <li> location of the source files
  * <li> arguments for the benchmark main
  * <li> classpath necessary to run the benchmark
  * <li> maximum time for each benchmarking, default to 15 seconds
  * <li> whether to recompile the benchmark classes
  * </ul>
  */
case class BenchmarkInfo(name: String,
                         src: Path,
                         arguments: List[String],
                         classpathURLs: List[URL],
                         timeout: Int,
                         notCompile: Boolean) {

  def this(xml: scala.xml.Elem) {
    this(
      (xml \\ "name").text,
      Path(xml \\ "src" text),
      (xml \\ "arg") map (_.text) toList,
      (xml \\ "cp") map (cp => Path(cp.text).toURL) toList,
      (xml \\ "timeout").text toInt,
      (xml \\ "notcompile").text toBoolean)
  }

  /** Produces a XML element representing this benchmark.
    */
  val toXML =
    <benchmark>
      <name>{ name }</name>
      <src>{ src.path }</src>
      <arguments>{ for (arg <- arguments) yield <arg>{ arg }</arg> }</arguments>
      <classpath>{ for (cp <- classpathURLs) yield <cp> { cp.getPath } </cp> }</classpath>
      <timeout>{ timeout }</timeout>
      <notcompile>{ notCompile }</notcompile>
    </benchmark>

  import common.BenchmarkCompiler.Compiler

  /** Compiles the benchmark using given compiler.
    *
    * @return
    *      true  if compile OK
    *      false otherwise
    */
  def isCompiledOK(compiler: Compiler, config: Config): Boolean =
    if (!notCompile && !(compiler compile this)) {
      UI.error("compile failed: " + this.name + " src: " + src.path)
      false
    }
    else {
      true
    }

}

object BenchmarkInfo {

  // format: OFF
  val srcOpt        = "src"
  val argumentsOpt  = "arguments"
  val classpathOpt  = "classpath"
  val timeoutOpt    = "timeout"
  val noncompileOpt = "noncompile"
  // format: ON

  /** `--` is automatically prepend to any option.
    * Pass the list of option names only.
    */
  def readInfo(argFile: String, options: List[String]): HashMap[String, String] = {
    val map = HashMap[String, String]()
    try {
      val argBuffer = Source.fromFile(argFile)
      for (line <- argBuffer.getLines; option <- options)
        if (line startsWith toOption(option)) map put (option, line dropWhile (_ != ' ') drop 1)
      argBuffer close
    }
    catch { case e => UI.debug("[read failed] " + argFile + "\n" + e.toString) }
    map
  }

}

class InfoPack {

  private var modes = ArrayBuffer[InfoMode]()
  private def currentMode = modes.last

  def switchMode(mode: Mode) = modes :+= new InfoMode(mode)
  def apply(mode: Mode) = modes find (_.mode == mode) get
  def add(newInfo: BenchmarkInfo) = currentMode add newInfo
  def foreach(f: InfoMode => Unit) = modes foreach f

  def filter(f: BenchmarkInfo => Boolean): InfoPack = {
    val pack = new InfoPack
    modes foreach (mode => {
      pack switchMode mode.mode
      mode.infos filter (f(_)) foreach (pack add _)
    })
    pack
  }

  def filterNot(f: BenchmarkInfo => Boolean): InfoPack = {
    val pack = new InfoPack
    modes foreach (mode => {
      pack switchMode mode.mode
      mode.infos filterNot (f(_)) foreach (pack add _)
    })
    pack
  }

  def contains(info: BenchmarkInfo) = modes exists (_.infos contains info)

}

class InfoMode(val mode: Mode) {

  private var _infos = ArrayBuffer[BenchmarkInfo]()

  def add(newInfo: BenchmarkInfo) = _infos += newInfo
  def infos = _infos
  def foreach(f: BenchmarkInfo => Unit) = infos foreach f
  def map[B](f: BenchmarkInfo => B) = infos map f

}
