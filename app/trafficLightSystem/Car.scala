package trafficLightSystem

import java.util.concurrent.atomic.AtomicBoolean

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

  private val _isNew = new AtomicBoolean(true)

  def isNew : Boolean = {
    _isNew.get()
  }

  override def move(): Transmittable = {
    _isNew.set(false)
    super.move()
  }

}