/*
 * ScrutinyRegressionResult
 * 
 * Version
 * 
 * Created on November 1st, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import scala.tools.sbs.performance.CIRegressionFailure
import scala.tools.sbs.performance.CIRegressionSuccess
import scala.tools.sbs.performance.ImmeasurableFailure
import scala.tools.sbs.performance.MeasurementFailure

trait ScrutinyRegressionResult extends ScrutinyResult

class ScrutinyCIRegressionSuccess(_benchmarkName: String,
                                  _confidenceLevel: Int,
                                  _current: (Double, Double),
                                  _previous: List[(Double, Double)],
                                  _CI: (Double, Double))
  extends CIRegressionSuccess(
    _benchmarkName,
    _confidenceLevel,
    _current,
    _previous,
    _CI)
  with ScrutinyRegressionResult
  with ScrutinySuccess {

  def this(CISuccess: CIRegressionSuccess) =
    this(
      CISuccess.benchmarkName,
      CISuccess.confidenceLevel,
      CISuccess.current,
      CISuccess.previous,
      CISuccess.CI)

}

object ScrutinyCIRegressionSuccess {

  def apply(benchmarkName: String,
            confidenceLevel: Int,
            current: (Double, Double),
            previous: List[(Double, Double)],
            CI: (Double, Double)): ScrutinyCIRegressionSuccess =
    new ScrutinyCIRegressionSuccess(benchmarkName, confidenceLevel, current, previous, CI)

  def apply(CISuccess: CIRegressionSuccess): ScrutinyCIRegressionSuccess =
    new ScrutinyCIRegressionSuccess(CISuccess)

  def unapply(srs: ScrutinyCIRegressionSuccess) =
    if (true) Some(srs.benchmarkName, srs.confidenceLevel, srs.current, srs.previous, srs.CI)
    else None // Force return type to Option[], 'cause it's too long to be explicitly written :(

}

trait ScrutinyRegressionFailure extends ScrutinyFailure with ScrutinyRegressionResult

class ScrutinyCIRegressionFailure(_benchmarkName: String,
                                  _current: (Double, Double),
                                  _previous: List[(Double, Double)],
                                  _CI: (Double, Double))
  extends CIRegressionFailure(
    _benchmarkName,
    _current,
    _previous,
    _CI)
  with ScrutinyRegressionFailure {

  def this(CIFailure: CIRegressionFailure) =
    this(
      CIFailure.benchmarkName,
      CIFailure.current,
      CIFailure.previous,
      CIFailure.CI)

}

object ScrutinyCIRegressionFailure {

  def apply(benchmarkName: String,
            current: (Double, Double),
            previous: List[(Double, Double)],
            meansAndSD: List[(Double, Double)],
            CI: (Double, Double)): ScrutinyCIRegressionFailure =
    new ScrutinyCIRegressionFailure(benchmarkName, current, previous, CI)

  def apply(CISuccess: CIRegressionFailure): ScrutinyCIRegressionFailure =
    new ScrutinyCIRegressionFailure(CISuccess)

  def unapply(srs: ScrutinyCIRegressionFailure) =
    if (true) Some(srs.benchmarkName, srs.current, srs.previous, srs.CI)
    else None // Force return type to Option[], 'cause it's too long to be explicitly written :(

}

class ScrutinyImmeasurableFailure(_benchmarkName: String,
                                  _failure: MeasurementFailure)
  extends ImmeasurableFailure(
    _benchmarkName,
    _failure: MeasurementFailure)
  with ScrutinyRegressionFailure {

  def this(imf: ImmeasurableFailure) = this(imf.benchmarkName, imf.failure)

}

object ScrutinyImmeasurableFailure {

  def apply(benchmarkName: String, failure: MeasurementFailure): ScrutinyImmeasurableFailure =
    new ScrutinyImmeasurableFailure(benchmarkName, failure)

  def apply(imf: ImmeasurableFailure): ScrutinyImmeasurableFailure =
    new ScrutinyImmeasurableFailure(imf)

  def unpply(sif: ScrutinyImmeasurableFailure): Option[(String, MeasurementFailure)] =
    Some(sif.benchmarkName, sif.failure)

}
