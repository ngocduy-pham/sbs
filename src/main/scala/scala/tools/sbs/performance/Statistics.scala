/*
 * Statistic
 * 
 * Version 
 * 
 * Created on September 5th, 2011
 *
 * Created by ND P
 */

package scala.tools.sbs
package performance

import scala.collection.mutable.ArrayBuffer
import scala.math.sqrt
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.io.Log

import org.apache.commons.math.distribution.FDistributionImpl
import org.apache.commons.math.distribution.NormalDistributionImpl
import org.apache.commons.math.distribution.TDistributionImpl

/** Class stores the significant level and computes statistical arguments for a given sample.
  */
trait Statistics {

  import Statistics.Statistician

  type subStatistician <: Statistician

  /** An simple implement of {@link Statistics}.
    */
  class SimpleStatistics(config: Config, log: Log, var alpha: Double = 0) extends Statistician {

    /** Reduces the confidence level time by time to by 5% each time,
      * except 2 cases:
      * <ul>
      * <li>From 100% to 99%
      * <li>From 99% to 95%
      * </ul>
      *
      * @return `true` if success, `false` if the confidence leval is at the minimum value.
      */
    def reduceConfidenceLevel(): Int = {
      if (confidenceLevel == 100) {
        alpha = 0.01
        log.verbose("Confidence level was reduced to " + confidenceLevel + "%")
      }
      else if (confidenceLevel == 99) {
        alpha = 0.05
        log.verbose("Confidence level was reduced to " + confidenceLevel + "%")
      }
      else if (confidenceLevel >= config.leastConfidenceLevel) {
        alpha += 0.05
        log.verbose("Confidence level was reduced to " + confidenceLevel + "%")
      }
      else {
        log.verbose("Confidence level is not reducible")
      }
      confidenceLevel
    }

    /** @return `true` if the confidence level is GE `1 - MAX_ALPHA`, `false` otherwise
      */
    def isConfidenceLevelAcceptable = if (confidenceLevel >= config.leastConfidenceLevel) true else false

    def resetConfidenceInterval() {
      alpha = 0
    }

    /** Computes the confidence interval for the given sample.
      *
      * @param series	The result of benchmarking
      *
      * @return	The left and right end points of the confidence interval.
      */
    def confidenceInterval(series: Series): (Double, Double) = {
      val SD = standardDeviation(series)
      var diff: Double = 0

      if (SD == 0) {
        diff = 0
      }
      else if (series.length >= 30) {
        diff = inverseGaussianDistribution * SD / sqrt(series.length)
      }
      else {
        diff = inverseStudentDistribution(series.length - 1) * SD / sqrt(series.length)
      }
      (mean(series) - diff, mean(series) + diff)
    }

    /** Computes z value of the standard normal distribution with mean 0 and variance 1.
      *
      * @param alpha	The significant level
      *
      * @return	The z value
      */
    private def inverseGaussianDistribution() =
      new NormalDistributionImpl(0, 1).inverseCumulativeProbability(1 - alpha / 2)

    /** Computes the t value of the Student distribution using:
      * <ul>
      * <li>A given degree of freedom
      * <li>The pre-defined significant level
      * </ul>
      *
      * @param df	The degree of freedom
      *
      * @return	The t value
      */
    private def inverseStudentDistribution(df: Int) =
      new TDistributionImpl(df).inverseCumulativeProbability(1 - alpha / 2)

    /** Computes the F value of the Fisher F distribution using:
      * <ul>
      * <li>Given degrees of freedom
      * <li>The pre-defined significant level
      * </ul>
      *
      * @param n1	The first degree of freedom
      * @param n2	The second degree of freedom
      *
      * @return	The F value
      */
    private def inverseFDistribution(n1: Int, n2: Int) =
      new FDistributionImpl(n1, n2).inverseCumulativeProbability(1 - alpha)

    /** @param series	The result of benchmarking
      *
      * @return	The minimum value
      */
    def min(series: Series) = series.foldLeft(series.head) { (min, s) => if (min < s) min else s }

    /** @param series	The result of benchmarking
      *
      * @return	The maximum value
      */
    def max(series: Series) = series.foldRight(series.last) { (max, s) => if (max > s) max else s }

    /** Computes the sample mean.
      *
      * @param series	The result of benchmarking
      *
      * @return	The average
      */
    def mean(series: Series) =
      if (series.length == 0) 0 else series.foldLeft(0: Double) { (sum: Double, s) => sum + s } / series.length

    /** Computes the standard deviation of a given sample.
      *
      * @param series	The result of benchmarking
      *
      * @return	The standard deviation
      */
    def standardDeviation(series: Series): Double = {
      if (series.length == 0) {
        0
      }
      else {
        val mean = this.mean(series)
        sqrt(series.foldLeft(0: Double) { (squareSum, s) => squareSum + (s - mean) * (s - mean) } / (series.length - 1))
      }
    }

    /** Computes the coefficient of variation of a given sample.
      *
      * @param series	The result of benchmarking
      *
      * @return	The coefficient of variation
      */
    def CoV(series: Series) = {
      val mean = this mean series
      if (mean == 0) 0 // each value of series >=0
      else standardDeviation(series) / mean
    }

    /** @return	The significant level alpha
      */
    def significantLevel = alpha

    /** @return	The confident level
      */
    def confidenceLevel: Int = (1 - alpha) * 100 toInt

    /** Statistically rigorously compares means of samples using statistically rigorous evaluation method:
      * <ul>
      * <li>Confidence intervals for comparing 2 alternatives.
      * <li>ANOVA for comparing 3 or more alternatives.
      * </ul>
      *
      * @param benchmark			The benchmark to be test
      * @param mode				The benchmarking mode
      * @param current			The just measured result
      * @param history			The list of previous results
      *
      * @return	The test result
      */
    def testDifference(info: BenchmarkInfo,
                       current: Series,
                       historian: History.Historian): RegressionResult = {
      if (historian.length < 1) {
        throw new Exception("Not enough result files specified")
      }
      if (historian.length == 1) {
        testConfidenceIntervals(info, current, historian)
      }
      else {
        testANOVA(info, current, historian)
      }
    }

    /** Statistically rigorously compares means of two samples using confidence intervals.
      *
      * @param benchmark			The benchmark to be test
      * @param mode				The benchmarking mode
      * @param current			The just measured result
      * @param history			The list of previous results
      *
      * @return	The test result
      */
    private def testConfidenceIntervals(info: BenchmarkInfo,
                                        current: Series,
                                        historian: History.Historian): RegressionResult = {
      val currentMean  = mean(current)
      val currentSD    = standardDeviation(current)
      val currentN     = current.length
      val previous     = historian.head
      val previousMean = mean(previous)
      val previousSD   = standardDeviation(previous)
      val previousN    = previous.length
      val diff         = previousMean - currentMean
      val slower       = currentMean > previousMean

      if ((current.confidenceLevel == 100) && (previous.confidenceLevel == 100) && !slower) {
        CIRegressionSuccess(
          info.name,
          100,
          (currentMean, currentSD),
          List((previousMean, previousSD)),
          (0, 0))
      }
      else {
        reduceConfidenceLevel()

        val s               = sqrt(previousSD * previousSD / previousN + currentSD * currentSD / currentN)
        var ciLeft: Double  = 0
        var ciRight: Double = 0

        var ok = false

        while (isConfidenceLevelAcceptable && !ok) {
          if ((previousN >= 30) && (currentN >= 30)) {
            ciLeft = diff - inverseGaussianDistribution * s
            ciRight = diff + inverseGaussianDistribution * s
          }
          else {
            var ndf: Int = ((previousSD * previousSD / previousN + currentSD * currentSD / currentN) * (previousSD * previousSD / previousN + currentSD * currentSD / currentN) /
              ((previousSD * previousSD / previousN) * (previousSD * previousSD / previousN) / (previousN - 1) + (currentSD * currentSD / currentN) * (currentSD * currentSD / currentN) / (currentN - 1))).toInt
            if (ndf == 0) {
              ndf = 1
            }
            ciLeft = diff - inverseStudentDistribution(ndf) * s
            ciRight = diff + inverseStudentDistribution(ndf) * s
          }

          if ((ciLeft > 0 && ciRight > 0) || (ciLeft < 0 && ciRight < 0)) {
            reduceConfidenceLevel()
          }
          else {
            ok = true
          }
        }

        if (!ok && slower) CIRegressionFailure(
          info.name,
          (currentMean, currentSD),
          List((previousMean, previousSD)),
          (ciLeft, ciRight))
        else CIRegressionSuccess(
          info.name,
          confidenceLevel,
          (currentMean, currentSD),
          List((previousMean, previousSD)),
          (ciLeft, ciRight))
      }
    }

    /** Statistically rigorously compares means of three or more samples using ANOVA.
      *
      * @param benchmark			The benchmark to be test
      * @param mode					The benchmarking mode
      * @param measurementResult	The just measured result
      * @param history			The list of previous results
      *
      * @return	The test result
      */
    private def testANOVA(info: BenchmarkInfo,
                          current: Series,
                          historian: History.Historian): RegressionResult = {

      val sum = historian.foldLeft(0: Long)((sum, p) => sum + p.sum) + current.sum
      val overall: Double = sum / ((historian.length + 1) * historian.head.length)

      val (currentMean, currentSD) = (mean(current), standardDeviation(current))
      val historyMeanAndSDs = historian map (s => (mean(s), standardDeviation(s)))

      var SSA: Double = 0
      var SSE: Double = 0
      historian foreach (alternative => {
        val alternativeMean = mean(alternative)
        SSA += (alternativeMean - overall) * (alternativeMean - overall) * alternative.length
        SSE += alternative.foldLeft(SSE) { (sse, value) => sse + (value - alternativeMean) * (value - alternativeMean) }
      })
      SSA += (currentMean - overall) * (currentMean - overall) * current.length
      current foreach (value => SSE += (value - currentMean) * (value - currentMean))

      val slower = historyMeanAndSDs forall (_._1 < currentMean)
      if (confidenceLevel == 100 && SSE == 0) {
        // Memory case
        if ((SSA != 0) && slower) {
          ANOVARegressionFailure(
            info.name,
            (currentMean, currentSD),
            historyMeanAndSDs,
            SSA,
            SSE,
            Double.PositiveInfinity,
            Double.NaN)
        }
        else {
          ANOVARegressionSuccess(
            info.name,
            100,
            (currentMean, currentSD),
            historyMeanAndSDs,
            SSA,
            SSE,
            Double.PositiveInfinity,
            Double.NaN)
        }
      }
      else {
        // Performance case

        val n1        = historian.length
        val n2        = (historian foldLeft 0)(_ + _.length) + current.length - historian.length - 1
        val FValue    = SSA * n2 / SSE / n1
        var F: Double = 0

        var ok = false
        reduceConfidenceLevel()

        while (isConfidenceLevelAcceptable && !ok) {
          F = inverseFDistribution(n1, n2)

          log.debug("[SSA] " + SSA + "\t[SSE] " + SSE + "\t[FValue] " + FValue + "\t[F(" + n1 + ", " + n2 + ")] " + F)

          if (FValue <= F) ok = true
          else reduceConfidenceLevel()
        }

        if (!ok && slower) ANOVARegressionFailure(
          info.name,
          (currentMean, currentSD),
          historyMeanAndSDs,
          SSA,
          SSE,
          FValue,
          F)
        else ANOVARegressionSuccess(
          info.name,
          confidenceLevel,
          (currentMean, currentSD),
          historyMeanAndSDs,
          SSA,
          SSE,
          FValue,
          F)
      }
    }

  }

  def apply(config: Config, log: Log, alpha: Double = 0): subStatistician

}

object Statistics extends Statistics {

  type subStatistician = Statistician

  trait Statistician {

    def reduceConfidenceLevel(): Int

    def isConfidenceLevelAcceptable: Boolean

    def resetConfidenceInterval(): Unit

    def confidenceInterval(series: Series): (Double, Double)

    def min(series: Series): Long

    def max(series: Series): Long

    def mean(series: Series): Double

    def standardDeviation(series: Series): Double

    /** Coefficient of variation
      */
    def CoV(series: Series): Double

    def significantLevel: Double

    def confidenceLevel: Int

    def testDifference(info: BenchmarkInfo,
                       current: Series,
                       historian: History.Historian): RegressionResult

  }

  def apply(config: Config, log: Log, alpha: Double = 0): subStatistician = new SimpleStatistics(config, log, alpha)

}
