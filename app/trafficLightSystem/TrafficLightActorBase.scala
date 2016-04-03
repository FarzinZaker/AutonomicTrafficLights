package trafficLightSystem

import akka.actor.{ActorRef, Actor}
import Direction._
import scala.collection.mutable

/**
  * Created by root on 4/3/16.
  */
abstract class TrafficLightActorBase(carSpeed: Int = 5, routeCapacity: Int = 60) extends Actor {


  protected val queues = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, mutable.Queue[Car]]]()
  protected val waitTimes = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Average]]()
  protected val neighbours = mutable.HashMap[Direction.Value, ActorRef]()
  protected val timings = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Double]]()
  protected val testActors = mutable.ArrayBuffer[ActorRef]()
  protected var isUnderAdaptation: Boolean = false

  //initialize local state variables
  Direction.values.foreach((sourceDirection: Direction.Value) => {
    timings(sourceDirection) = new mutable.HashMap[Direction.Value, Double]
    waitTimes(sourceDirection) = new mutable.HashMap[Direction.Value, Average]()
    queues(sourceDirection) = new mutable.HashMap[Direction.Value, mutable.Queue[Car]]
    Direction.values.foreach((destinationDirection: Direction.Value) => {
      queues(sourceDirection)(destinationDirection) = new mutable.Queue[Car]
      waitTimes(sourceDirection)(destinationDirection) = new Average
      if (sourceDirection != destinationDirection && sourceDirection != None && destinationDirection != None)
        timings(sourceDirection)(destinationDirection) = 1.0
      else
        timings(sourceDirection)(destinationDirection) = 0.0
    })
  })

  def handleNewCar(car: RealCar) = {
    if (!car.arrived()) {
      car.setEnqueueTime()
      queues(car.entranceDirection)(car.nextTrafficLightDirection).enqueue(car)
    }
  }

  def doRouting(route: Route): Route = {
    var totalTiming = 0.0
    Direction.values.foreach((sourceDirection: Direction.Value) => {
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        totalTiming += timings(sourceDirection)(destinationDirection)
      })
    })
    val carsCount = math.ceil(timings(route.sourceDirection)(route.destinationDirection) * routeCapacity / totalTiming).toInt
    for (i <- 0 until carsCount)
      if (queues(route.sourceDirection)(route.destinationDirection).nonEmpty) {
        val car = queues(route.sourceDirection)(route.destinationDirection).dequeue()
        car.elapseTime(carSpeed)
        if (neighbours.contains(route.destinationDirection)) {
          waitTimes(route.sourceDirection)(route.destinationDirection) += car.waitTime
          car.waitStack.push(car.waitTime)
          neighbours(route.destinationDirection) ! car.move()
        }
        else
          queues(route.sourceDirection)(route.destinationDirection).enqueue(car)
      }

    Direction.values.foreach((sourceDirection: Direction.Value) => {
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        for (car <- queues(sourceDirection)(destinationDirection)) {
          car.elapseTime(carsCount * carSpeed)
        }
      })
    })

    route.destinationDirection = Direction.next(route.destinationDirection)
    if (route.sourceDirection == route.destinationDirection) {
      route.sourceDirection = Direction.next(route.sourceDirection)
      route.destinationDirection = Direction.opponent(route.destinationDirection)
    }
    Thread.sleep(Math.round(carsCount * carSpeed))
    route
  }


  def getStatus: ActorStatus = {
    val status = new ActorStatus
    status.id = self.path.name.replace("TRAFFIC_LIGHT_", "")
    status.isUnderAdaptation = isUnderAdaptation
    if (neighbours.contains(North))
      status.neighbourList(North) = neighbours(North).path.name.replace("TRAFFIC_LIGHT_", "")
    if (neighbours.contains(East))
      status.neighbourList(East) = neighbours(East).path.name.replace("TRAFFIC_LIGHT_", "")
    if (neighbours.contains(South))
      status.neighbourList(South) = neighbours(South).path.name.replace("TRAFFIC_LIGHT_", "")
    if (neighbours.contains(West))
      status.neighbourList(West) = neighbours(West).path.name.replace("TRAFFIC_LIGHT_", "")
    status.queueSizeList(North) = queues(North)(East).size + queues(North)(South).size + queues(North)(West).size
    status.queueSizeList(East) = queues(East)(South).size + queues(East)(West).size + queues(East)(North).size
    status.queueSizeList(South) = queues(South)(West).size + queues(South)(North).size + queues(South)(East).size
    status.queueSizeList(West) = queues(West)(North).size + queues(West)(East).size + queues(West)(South).size
    val northCount = waitTimes(North)(East).count + waitTimes(North)(South).count + waitTimes(North)(West).count
    if (northCount > 0)
      status.averageWaitTimeList(North) = (waitTimes(North)(East).sum + waitTimes(North)(South).sum + waitTimes(North)(West).sum) / northCount
    val eastCount = waitTimes(East)(South).count + waitTimes(East)(West).count + waitTimes(East)(North).count
    if (eastCount > 0)
      status.averageWaitTimeList(East) = (waitTimes(East)(South).sum + waitTimes(East)(West).sum + waitTimes(East)(North).sum) / eastCount
    val southCount = waitTimes(South)(West).count + waitTimes(South)(North).count + waitTimes(South)(East).count
    if (southCount > 0)
      status.averageWaitTimeList(South) = (waitTimes(South)(West).sum + waitTimes(South)(North).sum + waitTimes(South)(East).sum) / southCount
    val westCount = waitTimes(West)(North).count + waitTimes(West)(East).count + waitTimes(West)(South).count
    if (westCount > 0)
      status.averageWaitTimeList(West) = (waitTimes(West)(North).sum + waitTimes(West)(East).sum + waitTimes(South)(West).sum) / westCount

    status.adaptationCount = if(isUnderAdaptation) 1 else 0
    status.testActorCount = testActors.size

    status
  }
}
