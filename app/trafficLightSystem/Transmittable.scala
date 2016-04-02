package trafficLightSystem

import Direction._

/**
  * Created by root on 3/2/16.
  */
abstract class Transmittable(val path: Path) {

  var entranceDirection = Direction.opponent(path.head)
  var remainingPath = path.clone()
  var currentRow = path.sourceRow
  val sourceRow = path.sourceRow
  val sourceColumn = path.sourceColumn
  val destinationRow = path.destinationRow
  val destinationColumn = path.destinationColumn
  var currentColumn = path.sourceColumn
  var passedPath = new Path(sourceRow, sourceColumn, destinationRow, destinationColumn)
  var nextTrafficLightDirection: Direction.Value = remainingPath.head
  var pathLength: Int = path.size

  def resetRemainingPath() = {
    remainingPath = path.clone()
  }

  def move(): Transmittable = {
    if (nextTrafficLightDirection == North)
      currentRow -= 1
    if (nextTrafficLightDirection == North)
      currentColumn += 1
    if (nextTrafficLightDirection == North)
      currentRow += 1
    if (nextTrafficLightDirection == North)
      currentColumn -= 1

    entranceDirection = Direction.opponent(nextTrafficLightDirection)
    passedPath.enqueue(remainingPath.dequeue())
    if (remainingPath.nonEmpty)
      nextTrafficLightDirection = remainingPath.head

    this
  }

  def arrived(): Boolean = {
    remainingPath.isEmpty
  }
}
