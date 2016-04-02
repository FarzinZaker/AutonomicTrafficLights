package trafficLightSystem

/**
  * Created by root on 3/11/16.
  */
class Average {

  var sum = 0L
  var count = 0

  def +=(value: Long) = {
    sum += value
    count += 1
  }

  def average(): Long = {
    sum / count
  }
}
