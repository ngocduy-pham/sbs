/*
 * Reflector
 * 
 * Version
 * 
 * Created on October 15th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package common

import java.net.URL

import scala.tools.nsc.io.Path
import scala.tools.nsc.util.ScalaClassLoader
import scala.tools.sbs.benchmark.Template
import scala.tools.sbs.io.Log
import scala.tools.sbs.util.Constant.COMPANION_FIELD
import scala.tools.sbs.util.Constant.DOLLAR

/** All the reflecting stuff should be done here.
  */
trait Reflection {

  import Reflection.Reflector

  /** A simple implement of {@link Reflection}.
    */
  class SimpleReflector(config: Config, log: Log) extends Reflector {

    def getClass(name: String, classpathURLs: List[URL]): Class[_] = {
      val classLoader = ScalaClassLoader.fromURLs(classpathURLs, classOf[Template].getClassLoader)
      classLoader tryToInitializeClass name getOrElse (throw new ClassNotFoundException(name))
    }

    def getObject[T](name: String, classpathURLs: List[URL]): T = {
      val clazz = getClass(name, classpathURLs)
      try {
        clazz.newInstance.asInstanceOf[T]
      }
      catch {
        case _: InstantiationException => {
          val clazz$ = getClass(name + DOLLAR, classpathURLs)
          (clazz$ getField COMPANION_FIELD get null).asInstanceOf[T]
        }
      }
    }

    def locationOf(name: String, classLoader: ClassLoader): Path = {
      try {
        val clazz = Class forName (name, false, classLoader)
        Path(clazz.getProtectionDomain.getCodeSource.getLocation.getPath).toCanonical
      }
      catch {
        case f: ClassNotFoundException => {
          log.debug("Class not found: " + name)
          throw f
        }
      }
    }

  }

}

object Reflection extends Reflection {

  trait Reflector {

    /** Loads the class with the given name in the given classpath.
      */
    def getClass(name: String, classpathURLs: List[URL]): Class[_]

    /** Gets the compinion object with the given name in the given classpath.
      */
    def getObject[T](name: String, classpathURLs: List[URL]): T

    /** Gets the location where the class with given name was loaded.
      */
    def locationOf(name: String, classLoader: ClassLoader): Path

  }

  def apply(config: Config, log: Log): Reflector = new SimpleReflector(config, log)

}
