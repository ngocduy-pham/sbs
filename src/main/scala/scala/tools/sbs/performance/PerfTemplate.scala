/*
 * PerformanceBenchmarkTemplate
 * 
 * Version
 * 
 * Created on October 30th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package performance

import scala.tools.sbs.benchmark.Template

trait PerfTemplate extends Template {

  val measurement = 11

  /** Number of history files to be created for future use.
    * Benchmarks may need their measurement histories for
    * regression detection, this value specifies the quantity of
    * these histories to be generated at the first time a
    * benchmark runs.
    */
  val sampleNumber = 0

}
