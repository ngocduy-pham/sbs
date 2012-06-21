package scala.tools.sbs
package test
package common

import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Path
import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.util.FileUtil

import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Spec

class ReflectionSpec extends Spec with BeforeAndAfter with BeforeAndAfterAll {

  val name = "A"

  val classDef = "class " + name

  val traitDef = "trait " + name

  val objectDef = "object " + name

  val extendsTemplate = "extends " + classOf[DummyTemplate].getName

  val reflectDir = testDir / "ReflectorSpec" createDirectory ()

  val reflectFile = reflectDir / (name + ".scala") toFile

  val reflectSub = reflectDir / "sub" toDirectory

  def define(content: String) {
    FileUtil createFile reflectFile.path
    FileUtil.write(reflectFile.path, content)
  }

  def compile(out: Path): Boolean = {
    val settings = new Settings(Console.println)
    val (ok, _) = settings.processArguments(
      List("-cp", ClassPath.fromURLs(
        classOf[DummyTemplate].getProtectionDomain.getCodeSource.getLocation :: testConfig.classpathURLs: _*)),
      false)

    if (ok) {
      FileUtil mkDir out
      settings.outdir.value = out.path
      val compiler = new Global(settings)
      new compiler.Run compile List(reflectFile.path)
      !compiler.reporter.hasErrors
    }
    else {
      false
    }
  }

  def init(content: String, out: Path): Boolean = {
    define(content)
    compile(out)
  }

  val reflector = Reflection(testConfig, testLog)

  override def afterAll = FileUtil clean testDir

  before(FileUtil clean reflectDir)

  describe("A Reflector") {

    it("should load the right class from given classpath") {
      val out = reflectDir
      init(classDef, out)
      val clazz = reflector.getClass(name, List(out.toURL))
      assert(clazz.getCanonicalName == name)
    }

    it("should load the right trait from given classpath") {
      val out = reflectDir
      init(traitDef, out)
      val clazz = reflector.getClass(name, List(out.toURL))
      assert(clazz.getCanonicalName == name)
      assert(clazz.isInterface)
    }

    it("should load the right object from given classpath") {
      val out = reflectDir
      init(objectDef, out)
      val clazz = reflector.getClass(name, List(out.toURL))
      assert(clazz.getCanonicalName == name)
    }

    it("should load the right class from given classpath: parent rather then child dir") {
      val parent = reflectDir
      init(classDef, parent)
      val child = reflectSub
      reflectFile.deleteIfExists
      define(traitDef)
      compile(child)

      val clazz = reflector.getClass(name, List(parent.toURL))
      assert(clazz.getCanonicalName == name)
      assert(!clazz.isInterface)
    }

    it("should load the right class from given classpath: child rather then parent dir") {
      val parent = reflectDir
      init(classDef, parent)
      val child = reflectSub
      reflectFile.deleteIfExists
      define(traitDef)
      compile(child)

      val clazz = reflector.getClass(name, List(child.toURL))
      assert(clazz.getCanonicalName == name)
      assert(clazz.isInterface)
    }

    it("should load the right class from given classpath: from classpath comes first") {
      val parent = reflectDir
      init(classDef, parent)
      val child = reflectSub
      reflectFile.deleteIfExists
      define(traitDef)
      compile(child)

      val clazz = reflector.getClass(name, List(child.toURL, parent.toURL))
      assert(clazz.getCanonicalName == name)
      assert(clazz.isInterface)
    }

    it("should rase ClassNotFoundException when the class cannot be found in the given classpath") {
      intercept[ClassNotFoundException] {
        reflector.getClass(name, Nil)
      }
    }

    it("should create the right object from given classpath: get from class") {
      val out = reflectDir
      init(classDef + " " + extendsTemplate, out)
      reflector.getObject[DummyTemplate](name, List(out.toURL))
    }

    it("should create the right object from given classpath: load object") {
      val out = reflectDir
      init(objectDef + " " + extendsTemplate, out)
      reflector.getObject[DummyTemplate](name, List(out.toURL))
    }

    it("should rase ClassNotFoundException when the class with given name is a trait") {
      val out = reflectDir
      init(traitDef + " " + extendsTemplate, out)
      intercept[ClassNotFoundException] {
        reflector.getObject[DummyTemplate](name, List(out.toURL))
      }
    }

    it("should rase ClassNotFoundException when the class with given name is a abstract class") {
      val out = reflectDir
      init("abstract " + classDef + " " + extendsTemplate, out)
      intercept[ClassNotFoundException] {
        reflector.getObject[DummyTemplate](name, List(out.toURL))
      }
    }

    it("should rase ClassCastException incase the class defined with given name does not implement the given trait") {
      val out = reflectDir
      init(classDef, out)
      intercept[ClassCastException] {
        reflector.getObject[DummyTemplate](name, List(out.toURL))
      }
    }

    it("should return the location where a class loaded") {
      val out = reflectDir
      init(classDef, out)
      val clazz = reflector.getClass(name, List(out.toURL))
      expect(out)(reflector.locationOf(name, clazz.getClassLoader))
    }

    it("should return the location where a class loaded: parent rather than child dir") {
      val parent = reflectDir
      init(classDef, parent)
      val child = reflectSub
      reflectFile.deleteIfExists
      define(traitDef)
      compile(child)

      val clazz = reflector.getClass(name, List(parent.toURL, child.toURL))
      expect(parent)(reflector.locationOf(name, clazz.getClassLoader))
    }

    it("should return the location where a class loaded: child rather than parent dir") {
      val parent = reflectDir
      init(classDef, parent)
      val child = reflectSub
      reflectFile.deleteIfExists
      define(traitDef)
      compile(child)

      val clazz = reflector.getClass(name, List(child.toURL, parent.toURL))
      expect(child)(reflector.locationOf(name, clazz.getClassLoader))
    }

  }

}

trait DummyTemplate
