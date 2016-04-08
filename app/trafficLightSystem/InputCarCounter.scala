package trafficLightSystem

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import org.encog.ml.data.versatile.VersatileDataSource

import scala.collection.mutable.ArrayBuffer

/**
  * Created by root on 4/7/16.
  */
class InputCarCounter extends VersatileDataSource {

  val carInputCountHistory = ArrayBuffer[AtomicInteger]()

  def elapseTimer() = {
    carInputCountHistory.synchronized {
      carInputCountHistory += new AtomicInteger(0)
    }
  }

  def increaseCarsCount() = {
    carInputCountHistory.synchronized {
      carInputCountHistory.last.incrementAndGet()
//      if (carInputCountHistory.size == 50) {
//        println()
//        println()
//        println("==================================")
////        TimeSeries.predict(carInputCountHistory.map {
////          _.get()
////        }.toArray)
//        new TimeSeriesSVM(carInputCountHistory.map {
//                    _.get().toDouble
//                  }).predict()
//        println("==================================")
//        println()
//        println()
//      }
      //      println(carInputCountHistory)
    }
  }

  val currentIndex = new AtomicInteger(0)

  override def columnIndex(s: String): Int = {
    0
  }

  override def rewind(): Unit = {
    currentIndex.set(0)
  }

  override def readLine(): Array[String] = {
    Array(carInputCountHistory(currentIndex.getAndIncrement()).toString)
  }

  def predict(): Int = {
    TimeSeries.predict(carInputCountHistory.map {
      _.get()
    }.toArray)
    0
  }
}
