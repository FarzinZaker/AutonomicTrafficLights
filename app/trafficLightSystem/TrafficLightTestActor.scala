package trafficLightSystem

import java.util.{Date, UUID}

import akka.actor.{ActorRef, Actor}
import trafficLightSystem.Direction._

import scala.collection.mutable

/**
  * Created by root on 3/11/16.
  */
class TrafficLightTestActor extends Actor {

  private var carSpeed: Int = 0
  private var routeCapacity: Int = 60
  private var adaptationGroup: UUID = null
  private var adaptation: UUID = null
  private var factor: Double = 1
  private var adaptationPathSourceDirection: Direction.Value = Direction.None
  private var adaptationPathDestinationDirection: Direction.Value = Direction.None

  private val queues = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, mutable.Queue[Car]]]()
  private val waitTimes = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Average]]()
  private val neighbours = mutable.HashMap[Direction.Value, ActorRef]()
  private val timings = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Long]]()

  val isLeader: Boolean = factor != 1

  def init(initData: TestActorInitData) = {

    carSpeed = initData.carSpeed
    routeCapacity = initData.routeCapacity
    adaptationGroup = initData.adaptationGroup
    adaptation = initData.adaptation
    factor = initData.factor
    adaptationPathSourceDirection = initData.adaptationPathSourceDirection
    adaptationPathDestinationDirection = initData.adaptationPathDestinationDirection

    //initialize local state variables
    Direction.values.foreach((sourceDirection: Direction.Value) => {
      if (initData.neighbours.contains(sourceDirection))
        neighbours(sourceDirection) = initData.neighbours(sourceDirection)
      timings(sourceDirection) = new mutable.HashMap[Direction.Value, Long]
      waitTimes(sourceDirection) = new mutable.HashMap[Direction.Value, Average]()
      queues(sourceDirection) = new mutable.HashMap[Direction.Value, mutable.Queue[Car]]
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        queues(sourceDirection)(destinationDirection) = new mutable.Queue[Car]
        waitTimes(sourceDirection)(destinationDirection) = new Average
        timings(sourceDirection)(destinationDirection) = initData.currentTimings(sourceDirection)(destinationDirection)
      })
    })
    timings(adaptationPathSourceDirection)(adaptationPathDestinationDirection) =
      Math.round(timings(adaptationPathSourceDirection)(adaptationPathDestinationDirection) * factor)
  }

  def receive = {
    case initData: TestActorInitData =>
      init(initData)
//      println(initData)

    case car: RealCar =>
    //      handleNewCar(TestCar(car, adaptationGroup, adaptation))

    case car: TestCar =>
      handleNewCar(car)

    case route: Route =>
      self ! doRouting(route)

    case "GET_ACTOR_STATUS" =>
      sender ! getStatus
  }

  /**
    * gets new car and enqueues it if needed
    *
    * @param car received car
    */
  def handleNewCar(car: TestCar) = {
    if (!car.arrived()) {

      car.setEnqueueTime()
      queues(car.entranceDirection)(car.nextTrafficLightDirection).enqueue(car)
    }
  }

  /**
    * route cars
    *
    * @param route current route
    * @return next route
    */
  def doRouting(route: Route): Route = {
//
//    var totalTiming = 0L
//    Direction.values.foreach((sourceDirection: Direction.Value) => {
//      Direction.values.foreach((destinationDirection: Direction.Value) => {
//        totalTiming += timings(sourceDirection)(destinationDirection)
//      })
//    })
//    val carsCount = Math.ceil(timings(route.sourceDirection)(route.destinationDirection).toFloat * routeCapacity / totalTiming).toInt
//    for (i <- 0 until carsCount)
//      if (queues(route.sourceDirection)(route.destinationDirection).nonEmpty) {
//        val car = queues(route.sourceDirection)(route.destinationDirection).dequeue()
//        car.elapseTime(carSpeed)
//        if (neighbours.contains(route.destinationDirection)) {
//          waitTimes(route.sourceDirection)(route.destinationDirection) += car.waitTime
//          car.waitStack.push(car.waitTime)
//          neighbours(route.destinationDirection) ! car.move()
//        }
//        else
//          queues(route.sourceDirection)(route.destinationDirection).enqueue(car)
//      }
//    for (car <- queues(route.sourceDirection)(route.destinationDirection)) {
//      car.elapseTime(carsCount * carSpeed)
//    }
//    route.destinationDirection = Direction.next(route.destinationDirection)
//    if (route.sourceDirection == route.destinationDirection) {
//      route.sourceDirection = Direction.next(route.sourceDirection)
//      route.destinationDirection = Direction.opponent(route.destinationDirection)
//    }
//
//    try {
//      Thread.sleep(Math.round(carsCount * carSpeed))
//    } catch {
//      case ex: Exception =>
//        println(ex)
//    }
    route
  }

  /**
    * collect actor status
    *
    * @return Actor Status
    */
  def getStatus: ActorStatus = {
    val status = new ActorStatus
//    status.id = self.path.name.replace("TRAFFIC_LIGHT_", "")
//    status.isUnderAdaptation = false
//    if (neighbours.contains(North))
//      status.neighbourList(North) = neighbours(North).path.name.replace("TRAFFIC_LIGHT_", "")
//    if (neighbours.contains(East))
//      status.neighbourList(East) = neighbours(East).path.name.replace("TRAFFIC_LIGHT_", "")
//    if (neighbours.contains(South))
//      status.neighbourList(South) = neighbours(South).path.name.replace("TRAFFIC_LIGHT_", "")
//    if (neighbours.contains(West))
//      status.neighbourList(West) = neighbours(West).path.name.replace("TRAFFIC_LIGHT_", "")
//    status.queueSizeList(North) = queues(North)(East).size + queues(North)(South).size + queues(North)(West).size
//    status.queueSizeList(East) = queues(East)(South).size + queues(East)(West).size + queues(East)(North).size
//    status.queueSizeList(South) = queues(South)(West).size + queues(South)(North).size + queues(South)(East).size
//    status.queueSizeList(West) = queues(West)(North).size + queues(West)(East).size + queues(West)(South).size
//    val northCount = waitTimes(North)(East).count + waitTimes(North)(South).count + waitTimes(North)(West).count
//    if (northCount > 0)
//      status.averageWaitTimeList(North) = (waitTimes(North)(East).sum + waitTimes(North)(South).sum + waitTimes(North)(West).sum) / northCount
//    val eastCount = waitTimes(East)(South).count + waitTimes(East)(West).count + waitTimes(East)(North).count
//    if (eastCount > 0)
//      status.averageWaitTimeList(East) = (waitTimes(East)(South).sum + waitTimes(East)(West).sum + waitTimes(East)(North).sum) / eastCount
//    val southCount = waitTimes(South)(West).count + waitTimes(South)(North).count + waitTimes(South)(East).count
//    if (southCount > 0)
//      status.averageWaitTimeList(South) = (waitTimes(South)(West).sum + waitTimes(South)(North).sum + waitTimes(South)(East).sum) / southCount
//    val westCount = waitTimes(West)(North).count + waitTimes(West)(East).count + waitTimes(West)(South).count
//    if (westCount > 0)
//      status.averageWaitTimeList(West) = (waitTimes(West)(North).sum + waitTimes(West)(East).sum + waitTimes(South)(West).sum) / westCount
//
//    status.adaptationCount = 0
//    status.testActorCount = 0

    status
  }

}
