/*
 * BenchmarkInfoSpec
 * 
 * Version
 * 
 * Created on June, 21st 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs.test
package benchmark

import scala.tools.nsc.io.Path.string2path
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.util.FileUtil

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Spec

class BenchmarkInfoSpec extends Spec with BeforeAndAfterAll {

  val argfile = testDir / "test.arg" toFile

  def define(content: List[String]) = {
    argfile.deleteIfExists
    FileUtil createFile argfile.path
    FileUtil.write(argfile.path, ("" /: content)(_ + "\n" + _))
  }

  override def afterAll = FileUtil clean testDir

  describe("object BenchmarkInfo") {

    it("does nothing when read a not existed file") {
      afterAll
      val map = BenchmarkInfo.readInfo(argfile.path, List("option"))
      expect(map isEmpty)(true)
    }

    it("reads nothing when the provided options not specified in .arg file") {
      define(List("abc skdfj", "xyz aslkdfj"))
      val map = BenchmarkInfo.readInfo(argfile.path, List("option"))
      expect(map isEmpty)(true)
    }

    it("reads arguments normally - one option") {
      define(List("--zzz abc", "--y 123"))
      val map = BenchmarkInfo.readInfo(argfile.path, List("y"))
      expect(map get "y")(Some("123"))
    }

    it("reads arguments normally - many options") {
      define(List("--zzz abc", "--y 123"))
      val map = BenchmarkInfo.readInfo(argfile.path, List("y", "zzz"))
      expect(map get "y")(Some("123"))
      expect(map get "zzz")(Some("abc"))
    }

    it("reads arguments normally - option starts with --") {
      define(List("--zzz abc", "--y 123"))
      val map = BenchmarkInfo.readInfo(argfile.path, List("y", "--zzz"))
      expect(map get "--zzz")(Some("abc"))
      expect(map get "zzz")(None)
    }

  }

}
