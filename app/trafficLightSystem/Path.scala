package trafficLightSystem

import trafficLightSystem.Direction._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by root on 2/27/16.
  */

object Path {

  def createReversePath(path: Path): Path = {
    val result = new Path(path.destinationRow, path.destinationColumn, path.sourceRow, path.sourceColumn)
    path.clone().reverse.foreach(
      (direction: Direction.Value) =>
        result.enqueue(Direction.opponent(direction))
    )
    result
  }
}

class Path(startRow: Int, startColumn: Int, endRow: Int, endColumn: Int, random: Boolean = false) extends mutable.Queue[Direction.Value] {

  var sourceRow = startRow
  var sourceColumn = startColumn
  var destinationRow = endRow
  var destinationColumn = endColumn
  var currentRow = startRow
  var currentColumn = startColumn

  def apply(startRow: Int, startColumn: Int, endRow: Int, endColumn: Int, directions: ListBuffer[Direction.Value]) = {
    directions.foreach(
      (direction: Direction.Value) => {
        enqueue(direction)
      }
    )
  }

  override def clone() : Path = {
    val path = new Path(startRow, startColumn, endRow, endColumn)
    for (elem <- this) {
      path.enqueue(elem)
    }
    path
  }

  def reachedTarget(): Boolean = {
    reachedTargetRow() && reachedTargetColumn()
  }

  private def reachedTargetRow(): Boolean = {
    currentRow == endRow
  }

  private def reachedTargetColumn(): Boolean = {
    currentColumn == endColumn
  }

  private def planVerticalMove() = {
    if (currentRow < endRow) {
      currentRow += 1
      enqueue(South)
    }
    else {
      currentRow -= 1
      enqueue(North)
    }
  }

  private def planHorizontalMove() = {
    if (currentColumn < endColumn) {
      currentColumn += 1
      enqueue(East)
    }
    else {
      currentColumn -= 1
      enqueue(West)
    }
  }

  def randomize() = {
    while (!reachedTarget())
      if (!reachedTargetRow()) {
        if (!reachedTargetColumn()) {
          //random choice
          if (math.random < 0.5) {
            planHorizontalMove()
          } else {
            planVerticalMove()
          }
        } else {
          planVerticalMove()
        }
      }
      else {
        if (!reachedTargetColumn()) {
          planHorizontalMove()
        }
      }

    currentRow = startRow
    currentColumn = startColumn
  }

  if (random)
    randomize()
}
