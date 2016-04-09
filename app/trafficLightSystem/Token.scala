package trafficLightSystem


import scala.collection.mutable

/**
  * Created by root on 4/5/16.
  */

object Token {
  def apply(car: Car, adaptationGroup: Long, adaptationFactor: Double): Token = {
    new Token(car.remainingPath.clone(), adaptationGroup, adaptationFactor)
  }
}

class Token(path: Path, val adaptationGroup: Long, val adaptationFactor: Double) extends Transmittable(path) {
}
