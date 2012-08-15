package scala.tools

import scala.tools.nsc.io.Path
import scala.tools.sbs.util.Constant

package object sbs {

  private[sbs] val stringToInt  = (str: String) => str.toInt
  private[sbs] val stringToList = (str: String) => str split Constant.COLON toList

  def argFromSrc(src: Path): String =
    if (src isFile) (src.path stripSuffix "scala") + "arg"
    else src.path + ".arg"

  def toOption(optionName: String): String =
    if (optionName startsWith Constant.ARG) optionName else Constant.ARG + optionName

  def getIntoOrElse[R](arg: Option[String], convert: String => R, default: => R): R =
    arg match {
      case Some(str) => convert(str)
      case _         => default
    }

}
