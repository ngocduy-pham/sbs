package scala.tools.sbs
package test
package common

import scala.actors.Actor.actor
import scala.actors.Actor.exit
import scala.actors.Actor.receiveWithin
import scala.actors.Actor.self
import scala.actors.TIMEOUT
import scala.tools.nsc.util.ClassPath
import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.JVMInvoker
import scala.tools.sbs.common.ObjectHarness
import org.scalatest.Spec
import scala.tools.sbs.benchmark.BenchmarkCreator


class JVMInvokerSpec extends Spec with BenchmarkCreator with Configured {

  val log = testLog
  val config = testConfig

  object Dummy extends Benchmark {
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

  case object FINISH

  val invoker = JVMInvoker(testLog, testConfig)

  def timing(time: Long, command: Seq[String]): Boolean = {
    val thiz = self
    actor {
      invoker.invoke(command, _ => (), _ => (), time)
      //      receiveWithin(time) {
      //        case _ => ()
      //      }
      thiz ! FINISH
      exit
    }
    return self.receiveWithin(time + 100) {
      case FINISH  => testLog.verbose("test finish"); true
      case TIMEOUT => testLog.verbose("test timeout"); false
    }
  }

  describe("A JVMInvoker") {

    it("should create precise OS java arguments which intended to launch a harness") {
      expect(Seq(
        "-cp",
        ClassPath.fromURLs(
          (testConfig.classpathURLs ++
            Dummy.info.classpathURLs ++
            List(testConfig.scalaLibraryJar.toURL, testConfig.scalaCompilerJar.toURL)): _*),
        testConfig.javaProp,
        "scala.tools.nsc.MainGenericRunner",
        "-cp",
        ClassPath.fromURLs(testConfig.classpathURLs ++ Dummy.info.classpathURLs: _*),
        DummyHarness.getClass.getName.replace("$", ""),
        scala.xml.Utility.trim(Dummy.info.toXML).toString) ++ testConfig.args)(
        invoker.asJavaArgument(DummyHarness, Dummy.info, testConfig.classpathURLs ++ Dummy.info.classpathURLs))
    }

    it("should create precise OS java arguments which intended to launch a snippet benchmark") {
      expect(Seq(
        "-cp",
        ClassPath.fromURLs(
          (testConfig.classpathURLs ++
            Dummy.info.classpathURLs ++
            List(testConfig.scalaLibraryJar.toURL, testConfig.scalaCompilerJar.toURL)): _*),
        testConfig.javaProp,
        "scala.tools.nsc.MainGenericRunner",
        "-cp",
        ClassPath.fromURLs(testConfig.classpathURLs ++ Dummy.info.classpathURLs: _*),
        Dummy.info.name) ++ Dummy.info.arguments)(
        invoker.asJavaArgument(Dummy.info, testConfig.classpathURLs ++ Dummy.info.classpathURLs))
    }

    it("should invoke a JVM running within a specified amount of time - timeout") {
      val time = 1 * 1000
      assert(!timing(time, Seq("cmd")))
    }

    it("should invoke a JVM running within a specified amount of time - finish") {
      val time = 1 * 1000
      assert(timing(time, Seq("java")))
    }

  }

}
