package trafficLightSystem

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import org.encog.ml.data.versatile.VersatileDataSource
import org.encog.util.EngineArray

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue
import org.encog.util.EngineArray

/**
  * Created by root on 4/7/16.
  */
trait InputCarPrediction {

  val carInputCountHistory = ArrayBuffer[AtomicInteger]()
  val carInputRowHistory = ArrayBuffer[Int]()
  val carInputColumnHistory = ArrayBuffer[Int]()

  def elapseTimer() = {
    carInputCountHistory.synchronized {
      carInputCountHistory += new AtomicInteger(0)
    }
  }

  def increaseCarsCount() = {
    carInputCountHistory.synchronized {
      carInputCountHistory.last.incrementAndGet()
    }
  }

  def recordCarDestination(row: Int, column: Int) = {
    this.synchronized {
      carInputRowHistory += row
      carInputColumnHistory += column
    }
  }

  def predictNextCars(count: Int, currentRow: Int, currentColumn: Int): mutable.Queue[mutable.Queue[Car]] = {
    val result = mutable.Queue[mutable.Queue[Car]]()

    //copy data
    val carInputCountHistoryCopy: ArrayBuffer[Double] = carInputCountHistory.map {
      _.get().toDouble
    }
    val carInputRowHistoryCopy: ArrayBuffer[Double] = carInputRowHistory.map {
      _.toDouble
    }.clone()
    val carInputColumnHistoryCopy: ArrayBuffer[Double] = carInputColumnHistory.map {
      _.toDouble
    }.clone()

    //loops
    for (i <- 0 until count) {
      val carCount = new TimeSeriesSVM(carInputCountHistoryCopy).predict()
      carInputCountHistoryCopy += carCount
      val currentCarList = mutable.Queue[Car]()
      for (j <- 0 until carCount) {
        var carRow = -1
        var carColumn = -1
        while (carRow < 0 || carRow > 7 || carColumn < 0 || carColumn > 7 || (carRow == currentRow && carColumn == currentColumn)) {
          carRow = new TimeSeriesSVM(carInputRowHistoryCopy).predict()
          carInputRowHistoryCopy += carRow
          carColumn = new TimeSeriesSVM(carInputColumnHistoryCopy).predict()
          carInputColumnHistoryCopy += carColumn
        }
        currentCarList.enqueue(new Car(new Path(currentRow, currentColumn, carRow, carColumn, true)))
      }
      result.enqueue(currentCarList)
    }
    result
  }
}
