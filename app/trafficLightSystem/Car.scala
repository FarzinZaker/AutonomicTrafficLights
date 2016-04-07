package trafficLightSystem

import java.util.{UUID, Date}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by root on 2/27/16.
  */

object Car {

  def parse(data: Array[String]): Car = {
    val path = Path.parse(data)
    if (path.nonEmpty)
      new Car(path)
    else
      null
  }

}

class Car(path: Path) extends Transmittable(path) {

}