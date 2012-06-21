package scala.tools.sbs
package test
package common

import scala.tools.nsc.util.ClassPath
import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.JVMInvoker
import scala.tools.sbs.common.ObjectHarness

import org.scalatest.Spec

class JVMInvokerSpec extends Spec {

  object DummyBenchmark extends BenchmarkBase.Benchmark {
    val info: BenchmarkInfo = new BenchmarkInfo(
      "dummy",
      testDir,
      List("$1", "$2"),
      List(testDir.toURL),
      60000,
      true
    )
    def createLog(mode: Mode) = testLog
    def init() = ()
    def run() = ()
    def reset() = ()
    def context = ClassLoader.getSystemClassLoader
  }

  object DummyHarness extends ObjectHarness {

    def main(args: Array[String]) {}

  }

  val invoker = JVMInvoker(testLog, testConfig)

  describe("A JVMInvoker") {

    it("should create precise OS java arguments which intended to launch a harness") {
      expect(Seq(
        "-cp",
        ClassPath.fromURLs(
          (testConfig.classpathURLs ++
            DummyBenchmark.info.classpathURLs ++
            List(testConfig.scalaLibraryJar.toURL, testConfig.scalaCompilerJar.toURL)): _*),
        testConfig.javaProp,
        "scala.tools.nsc.MainGenericRunner",
        "-cp",
        ClassPath.fromURLs(testConfig.classpathURLs ++ DummyBenchmark.info.classpathURLs: _*),
        DummyHarness.getClass.getName.replace("$", ""),
        scala.xml.Utility.trim(DummyBenchmark.info.toXML).toString) ++ testConfig.args)(
        invoker.asJavaArgument(DummyHarness, DummyBenchmark, testConfig.classpathURLs ++ DummyBenchmark.info.classpathURLs))
    }

    it("should create precise OS java arguments which intended to launch a snippet benchmark") {
      expect(Seq(
        "-cp",
        ClassPath.fromURLs(
          (testConfig.classpathURLs ++
            DummyBenchmark.info.classpathURLs ++
            List(testConfig.scalaLibraryJar.toURL, testConfig.scalaCompilerJar.toURL)): _*),
        testConfig.javaProp,
        "scala.tools.nsc.MainGenericRunner",
        "-cp",
        ClassPath.fromURLs(testConfig.classpathURLs ++ DummyBenchmark.info.classpathURLs: _*),
        DummyBenchmark.info.name) ++ DummyBenchmark.info.arguments)(
        invoker.asJavaArgument(DummyBenchmark, testConfig.classpathURLs ++ DummyBenchmark.info.classpathURLs))
    }

  }

}
