package trafficLightSystem

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

/**
  * Created by root on 3/28/16.
  */

object DataSource {

  val carDataFileName = "REAL_DATA.txt"
  val carDataFilePath = Paths.get(carDataFileName)
}

class DataSource(rowsCount: Int, columnsCount: Int) {

  private val random = new scala.util.Random

  private def random(range: Range): Int = {
    range(random.nextInt(range.length))
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
}
