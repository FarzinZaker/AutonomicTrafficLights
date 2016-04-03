package trafficLightSystem

import java.util.{UUID, Date}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by root on 2/27/16.
  */

object Car {

  def parseRealCar(data: Array[String]): RealCar = {
    val path = Path.parse(data)
    if (path.nonEmpty)
      new RealCar(path)
    else
      null
  }

  def parseTestCar(data: Array[String], adaptationGroup: UUID, adaptation: UUID): TestCar = {
    val path = Path.parse(data)
    if (path.nonEmpty)
      new TestCar(path, adaptationGroup, adaptation)
    else
      null
  }
}

class Car(path: Path) extends Transmittable(path) with TimeAware {

  var waitStack = mutable.Stack[Long]()

  def speed(): Long = {
    elapsedTime() / pathLength
  }

}

class RealCar(path: Path) extends Car(path) {}

object TestCar {
  def apply(car: Car, adaptationGroup: UUID, adaptation: UUID): TestCar = {
    new TestCar(car.remainingPath.clone(), adaptationGroup, adaptation)
  }
}

class TestCar(path: Path, adaptationGroup: UUID, adaptation: UUID) extends Car(path) {}