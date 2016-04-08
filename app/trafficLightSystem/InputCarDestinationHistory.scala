package trafficLightSystem

import java.util.concurrent.atomic.AtomicInteger

import org.encog.ml.data.versatile.VersatileDataSource

import scala.collection.mutable.ArrayBuffer

/**
  * Created by root on 4/7/16.
  */
class InputCarDestinationHistory extends VersatileDataSource {

  val carInputRowHistory = ArrayBuffer[Int]()
  val carInputColumnHistory = ArrayBuffer[Int]()

  def recordCarDestination(row: Int, column: Int) = {
    this.synchronized {
      carInputRowHistory += row
      carInputColumnHistory += column
    }
    //    println(carInputRowHistory)
    //    println(carInputColumnHistory)
  }

  val currentIndex = new AtomicInteger(0)

  override def columnIndex(s: String): Int = {
    if (s.toLowerCase.trim == "row")
      0
    else
      1
  }

  override def rewind(): Unit = {
    currentIndex.set(0)
  }

  override def readLine(): Array[String] = {
    Array(carInputRowHistory(currentIndex.getAndIncrement()).toString, carInputColumnHistory(currentIndex.getAndIncrement()).toString)
  }
}
