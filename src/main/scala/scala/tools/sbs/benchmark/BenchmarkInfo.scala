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

/** Holds the information of benchmarks before compiling.
  */
case class BenchmarkInfo(name: String,
                         src: Path,
                         arguments: List[String],
                         classpathURLs: List[URL],
                         timeout: Int,
                         shouldCompile: Boolean) {

  import BenchmarkBase.Benchmark
  import common.BenchmarkCompiler.Compiler

  def isCompiledOK(compiler: Compiler, config: Config): Boolean =
    if (shouldCompile && !(compiler compile this)) {
      UI.error("Compile failed: " + this.name + " src: " + src.path)
      false
    }
    else {
      true
    }

  def expand(factory: BenchmarkBase#Factory, config: Config): Benchmark = factory createFrom this

}

object BenchmarkInfo {

  def readInfo(src: Path, options: List[String]): HashMap[String, String] = {
    val argFile =
      if (src isFile) (src.path stripSuffix "scala") + "arg"
      else src.path + ".arg"
    val map = HashMap[String, String]()
    try {
      val argBuffer = Source.fromFile(argFile)
      for (line <- argBuffer.getLines; option <- options)
        if (line startsWith option) map put (option, (line split " ")(1))
      argBuffer close
    }
    catch { case e => UI.debug("[Read failed] " + argFile + "\n" + e.toString) }
    map
  }

}

class InfoPack {

  private var modes       = ArrayBuffer[InfoMode]()
  private def currentMode = modes.last

  def switchMode(mode: Mode)       = modes :+= new InfoMode(mode)
  def apply(mode: Mode)            = modes find (_.mode == mode) get
  def add(newInfo: BenchmarkInfo)  = currentMode add newInfo
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

  def add(newInfo: BenchmarkInfo)       = _infos += newInfo
  def infos                             = _infos
  def foreach(f: BenchmarkInfo => Unit) = infos foreach f
  def map[B](f: BenchmarkInfo => B)     = infos map f

}
