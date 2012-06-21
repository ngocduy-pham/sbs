/*
 * ArgumentParser
 * 
 * Version 
 * 
 * Created on September 5th, 2011
 *
 * Created by ND P
 */

package scala.tools.sbs

import scala.Array.canBuildFrom
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Path
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.benchmark.InfoPack
import scala.tools.sbs.io.Log
import scala.tools.sbs.io.LogFactory
import scala.tools.sbs.io.UI
import scala.tools.sbs.util.Constant.COLON

/** Parser for the suite's arguments.
  */
object ArgumentParser {

  /** Parses the arguments from command line.
    *
    * @return
    * <ul>
    * <li>The {@link Config} object conresponding for the parsed values
    * <li>The {@link Log}
    * <li>The `List` of benchmarks to be run
    * </ul>
    */
  def parse(args: Array[String]): (Config, Log, InfoPack) = {
    val config = new Config(args)
    UI.config = config
    val log = LogFactory(config)
    val pack = new InfoPack
    config.modes foreach (mode => {
      pack switchMode mode
      val modeLocation = config.benchmarkDirectory / mode.location
      val nameList =
        if (config.parsed.residualArgs.length > 0) config.parsed.residualArgs
        else modeLocation.toDirectory.list.toList filterNot (_ hasExtension "arg") map (_ name)
      nameList map (name => getInfo(name, mode, config)) filterNot (_ isDefined) foreach (info => pack add info.get)
    })
    (config, log, pack)
  }

  /** Tries to create a {@link scala.tools.sbs.benchmark.BenchmarkInfo} object,
    * which has `name`, along with its specified arguments in the benchmark directory.
    *
    * @param name	the name of the desired benchmark
    * @param mode   the benchmarking mode
    * @param config configuration object
    *
    * @return	`Some(BenchmarkInfo)` if there is actually a benchmark with the given name
    *           `None` otherwise
    */
  def getInfo(name: String, mode: Mode, config: Config): Option[BenchmarkInfo] = {
    val maybeSrc = getSource(name, config.benchmarkDirectory / mode.location)
    val (mainClassName, argFile) = maybeSrc match {
      case Some(source) =>
        (if (source isFile) source.stripExtension else source.name, argFromSrc(source))
      case None => {
        val path = config.benchmarkDirectory / mode.location / name
        (name, path.path + (if (path.isFile && (path hasExtension "arg"))  "" else ".arg"))
      }
    }

    import BenchmarkInfo._

    val argMap = BenchmarkInfo.readInfo(argFile, List(srcOpt, argumentsOpt, classpathOpt, timeoutOpt, noncompileOpt))
    
    if (!maybeSrc.isDefined && !(argMap get srcOpt).isDefined) None
    else Some(BenchmarkInfo(
      mainClassName,
      if (maybeSrc isDefined) maybeSrc.get else Path(argMap get srcOpt get),
      argMap get argumentsOpt getOrElse "" split " " toList,
      argMap get classpathOpt getOrElse "" split COLON map (Path(_).toCanonical.toURL) toList,
      getIntoOrElse(argMap get timeoutOpt, stringToInt, config.timeout),
      argMap get noncompileOpt isDefined))
  }

  /** Checks whether a `name` in `path` directory is a benchmark.
    * If so, returns its source file(s).
    *
    * @param name	The name to be checked
    * @param path	The benchmark directory
    *
    * @return	`Some[Path]` to the source file / directory if `name` is a benchmark `None` otherwise
    */
  def getSource(name: String, path: Path): Option[Path] = {
    val src = path / name
    if (src exists) {
      if (src.isFile)
        if (src hasExtension "scala") Some(src) else None
      else if (src.toDirectory.deepFiles exists (p => p.isFile && (p hasExtension "scala"))) Some(src)
      else None
    }
    else None
  }

}
