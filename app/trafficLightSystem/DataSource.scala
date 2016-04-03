package trafficLightSystem

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

import akka.actor.ActorRef

import scala.io.Source

/**
  * Created by root on 3/28/16.
  */

object DataSource {

  val carDataFileName = "REAL_DATA.txt"
  val carDataFilePath = Paths.get(carDataFileName)
  var feedingRounds = 3
  var feedingRound = 1
}

class DataSource(rowsCount: Int, columnsCount: Int) {

  private val random = new scala.util.Random

  private def random(range: Range): Int = {
    var result : Int = -1
    while(result < 0 || result < range(0) || result > range(range.length - 1)) {
      val rand = random.nextGaussian()
      result = Math.round((rand + 2) * range.length / 4 + range(0)).toInt
    }
    result
  }

  //create data file
  if (!Files.exists(DataSource.carDataFilePath)) {
    Files.createFile(DataSource.carDataFilePath)
    val writer = new PrintWriter(new File(DataSource.carDataFileName))
    var period = random(5 to 20)
    for (i <- 0 until 2000) {
      var sourceRow = random(0 until rowsCount)
      var sourceColumn = random(0 until columnsCount)
      var destinationRow = random(0 until rowsCount)
      var destinationColumn = random(0 until columnsCount)

      while (Math.abs(destinationRow - sourceRow) + Math.abs(destinationColumn - sourceColumn) < 5) {
        sourceRow = random(0 until rowsCount)
        sourceColumn = random(0 until columnsCount)
        destinationRow = random(0 until rowsCount)
        destinationColumn = random(0 until columnsCount)
      }

      if (sourceRow != destinationRow || sourceColumn != destinationColumn) {

        writer.write(s"$sourceRow $sourceColumn $destinationRow $destinationColumn ${new Path(sourceRow, sourceColumn, destinationRow, destinationColumn, true).toList.collect { case dir: Direction.Value => Direction.get(dir).toString }.mkString(" ")}\r\n")

        period -= 1
        if (period == 0) {
          writer.write(s"sleep ${random(100 to 300)}\r\n")
          period = random(5 to 20)
        }
      }
    }
    writer.close()
  }

  def feed(trafficLightGrids: Array[TrafficLightGridBase]) = {

    new Thread(new Runnable {
      override def run(): Unit = {

        DataSource.feedingRound = 1
        while (DataSource.feedingRound <= DataSource.feedingRounds) {
          for (line <- Source.fromFile(DataSource.carDataFileName).getLines()) {
            val parts = line.toString.split(' ')
            if (parts(0) == "sleep") {
              Thread.sleep(parts(1).toLong)
            }
            else {
              if (Path.parse(parts).nonEmpty) {
                for (grid <- trafficLightGrids) {
                  if (grid != null)
                    grid.feed(Car.parseRealCar(parts))
                }
              }
            }
          }

          DataSource.feedingRound += 1
        }
      }
    }).start()
  }
}
