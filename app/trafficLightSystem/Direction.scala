package trafficLightSystem


/**
  * Created by root on 2/27/16.
  */
object Direction extends Enumeration {
  var North, East, South, West, None = Value

  def next(value: Direction.Value): Direction.Value = {
    get((get(value) + 1) % 4)
  }

  def opponent(value: Direction.Value): Direction.Value = {
    get((get(value) + 2) % 4)
  }

  def get(index: Int): Direction.Value = {
    index match {
      case 0 => North
      case 1 => East
      case 2 => South
      case 3 => West
    }
  }

  def get(value: Direction.Value): Int = {
    if (value == North)
      0
    else if (value == East)
      1
    else if (value == South)
      2
    else
      3
  }
}
