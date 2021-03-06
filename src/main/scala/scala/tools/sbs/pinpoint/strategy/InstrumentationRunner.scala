/*
 * InstrumentationRunner
 * 
 * Version
 * 
 * Created on November 6th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint
package strategy

import scala.tools.nsc.io.Path
import scala.tools.sbs.util.FileUtil

import instrumentation.CodeInstrumentor.InstrumentingMethod
import instrumentation.CodeInstrumentor

trait InstrumentationRunner extends InstrumentationUtility {
  self: PinpointBenchmark with Configured =>

  def instrumentAndRun[R](benchmark: BenchmarkType,
                          instrument: (InstrumentingMethod, CodeInstrumentor) => Unit,
                          originalClasspathURLs: List[java.net.URL],
                          run: List[java.net.URL] => R): R = {
    // format: OFF
    val className       = benchmark.className
    val methodName      = benchmark.methodName
    val instrumentor    = CodeInstrumentor(config, log, benchmark.exclude)
    val (clazz, method) = instrumentor.getClassAndMethod(className, methodName, originalClasspathURLs)
    // format: ON
    if (method == null) throw new PinpointingMethodNotFoundException(className, methodName)
    instrument(method, instrumentor)
    instrumentor.writeFile(clazz, instrumentedPath)

    val classFile = Path(clazz.getURL.getPath)
    backup(
      List(classFile.toFile),
      (classFile /: (clazz.getName split "/."))((path, _) => path.parent) toDirectory)

    val result = run(instrumentedPath.toURL :: originalClasspathURLs)

    restoreAll()
    FileUtil clean instrumentedPath

    result
  }

}
