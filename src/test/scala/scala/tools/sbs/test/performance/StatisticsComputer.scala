package scala.tools.sbs
package test
package performance
package regression

import scala.collection.mutable.ArrayBuffer
import scala.tools.sbs.performance.History
import scala.tools.sbs.performance.Statistics
import scala.tools.sbs.performance.Series

import org.scalatest.Spec

class StatisticsComputer extends Spec with PerformanceTest {

  val computer = Statistics(testConfig, testLog)

  describe("StatisticComputer") {

    //    it("PinpointForShow.bridge()V at line 25 and Iterator_flatten$.main()V at line 26") {
    //      val previous = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          648,
    //          650,
    //          648,
    //          647,
    //          646,
    //          648,
    //          647,
    //          649,
    //          646,
    //          649,
    //          648,
    //          648,
    //          647),
    //        99)
    //
    //      val current = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          667,
    //          668,
    //          667,
    //          667,
    //          667,
    //          669,
    //          668,
    //          666,
    //          669,
    //          668,
    //          684,
    //          667,
    //          670),
    //        99)
    //
    //      val history = HistoryFactory(DummyBenchmark, SteadyState)
    //      history add previous
    //      val ret = computer.testDifference(DummyBenchmark, current, history)
    //      ret.toReport foreach println
    //    }

    //    it("PinpointForShow.bridge()V") {
    //      val previous = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          328,
    //          327,
    //          328,
    //          329,
    //          328,
    //          328,
    //          328,
    //          328,
    //          329,
    //          329,
    //          328,
    //          328,
    //          327),
    //        95)
    //
    //      val current = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          359,
    //          359,
    //          358,
    //          358,
    //          358,
    //          358,
    //          358,
    //          365,
    //          372,
    //          358,
    //          364,
    //          358,
    //          358),
    //        95)
    //
    //      val history = HistoryFactory(DummyBenchmark, SteadyState)
    //      history add previous
    //      val ret = computer.testDifference(DummyBenchmark, current, history)
    //      ret.toReport foreach println
    //    }

    //    it("PinpointForShow.foo()V at line 31 and ListBuffer_size$.run()V at line 32") {
    //      val previous = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          328,
    //          327,
    //          328,
    //          328,
    //          328,
    //          328,
    //          328,
    //          328,
    //          328,
    //          328,
    //          327,
    //          329,
    //          340),
    //        99)
    //
    //      val current = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          363,
    //          362,
    //          363,
    //          362,
    //          363,
    //          362,
    //          362,
    //          364,
    //          365,
    //          362,
    //          363,
    //          363,
    //          363),
    //        99)
    //
    //      val history = HistoryFactory(DummyBenchmark, SteadyState)
    //      history add previous
    //      val ret = computer.testDifference(DummyBenchmark, current, history)
    //      ret.toReport foreach println
    //    }

    //    it("PinpointForShow.foo()V") {
    //      val previous = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          50,
    //          51,
    //          50,
    //          50,
    //          50,
    //          50,
    //          51,
    //          51,
    //          50,
    //          51,
    //          50,
    //          50,
    //          50),
    //        99)
    //
    //      val current = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          50,
    //          50,
    //          51,
    //          51,
    //          50,
    //          50,
    //          50,
    //          51,
    //          51,
    //          50,
    //          50,
    //          51,
    //          51),
    //        99)
    //
    //      val history = HistoryFactory(DummyBenchmark, SteadyState)
    //      history add previous
    //      val ret = computer.testDifference(DummyBenchmark, current, history)
    //      ret.toReport foreach println
    //    }

    //    it("PinpointForShow.failure()LListBuffer_size$; at line 32 and ListBuffer_size$.run()V at line 32") {
    //      val previous = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          280,
    //          279,
    //          281,
    //          284,
    //          277,
    //          278,
    //          278,
    //          279,
    //          278,
    //          278,
    //          281,
    //          278,
    //          279),
    //        99)
    //
    //      val current = new Series(
    //        testConfig,
    //        testLog,
    //        ArrayBuffer[Long](
    //          315,
    //          316,
    //          313,
    //          313,
    //          312,
    //          314,
    //          313,
    //          313,
    //          316,
    //          314,
    //          313,
    //          312,
    //          313),
    //        99)
    //
    //      val history = HistoryFactory(DummyBenchmark, SteadyState)
    //      history add previous
    //      val ret = computer.testDifference(DummyBenchmark, current, history)
    //      ret.toReport foreach println
    //    }

    it("ListBuffer_size$.run()V") {
      val previous = new Series(
        testConfig,
        testLog,
        ArrayBuffer[Long](
          278,
          278,
          279,
          278,
          279,
          278,
          288,
          281,
          278,
          279,
          279,
          278,
          278),
        99)

      val current = new Series(
        testConfig,
        testLog,
        ArrayBuffer[Long](
          320,
          308,
          309,
          310,
          309,
          310,
          311,
          327,
          318,
          315,
          313,
          310,
          310),
        99)

      val history = History()
      history add previous
      val ret = computer.testDifference(DummyBenchmark.info, current, history)
      ret.toReport foreach println
    }

  }

}

