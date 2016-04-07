package trafficLightSystem

import java.util.UUID

import scala.collection.mutable

/**
  * Created by root on 4/5/16.
  */

object Token {
  def apply(car: Car, adaptationGroup: UUID, adaptationFactor: Double): Token = {
    new Token(car.remainingPath.clone(), adaptationGroup, adaptationFactor)
  }
}

class Token(path: Path, val adaptationGroup: UUID, val adaptationFactor: Double) extends Transmittable(path) {
}
