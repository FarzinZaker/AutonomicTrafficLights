package trafficLightSystem

import java.util.Date
import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean, AtomicInteger}

import akka.actor.{ActorSelection, ActorRef, Actor}
import Direction._
import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by root on 4/3/16.
  */
object TrafficLightActorBase {
  var routeCounter: AtomicInteger = new AtomicInteger(0)

  def increaseRouteCount() = {
    routeCounter.incrementAndGet()
    //    println(s"route count:\t${routeCounter.get()}")
  }
}

abstract class TrafficLightActorBase(var transmittableSpeed: Int = 5, var routeCapacity: Int = 600) extends Actor {

  import context._

  protected val queues = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, mutable.Queue[Transmittable]]]()
  protected val waitTimes = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Average]]()
  protected val neighbours = mutable.HashMap[Direction.Value, ActorRef]()
  protected val timings = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Double]]()
  protected val testActors = mutable.HashMap[Long, mutable.HashMap[Double, ActorRef]]()
  protected var isUnderAdaptation = new AtomicBoolean(false)
  protected val adaptationTimes = new Average()
  protected var rowNumber: Int = 0
  protected var columnNumber: Int = 0
  protected var adaptationStartTime = new AtomicLong(0)

  //initialize local state variables
  Direction.values.foreach((sourceDirection: Direction.Value) => {
    timings(sourceDirection) = new mutable.HashMap[Direction.Value, Double]
    waitTimes(sourceDirection) = new mutable.HashMap[Direction.Value, Average]()
    queues(sourceDirection) = new mutable.HashMap[Direction.Value, mutable.Queue[Transmittable]]
    Direction.values.foreach((destinationDirection: Direction.Value) => {
      queues(sourceDirection)(destinationDirection) = new mutable.Queue[Transmittable]
      waitTimes(sourceDirection)(destinationDirection) = new Average
      if (sourceDirection != destinationDirection && sourceDirection != None && destinationDirection != None)
        timings(sourceDirection)(destinationDirection) = 1.0
      else
        timings(sourceDirection)(destinationDirection) = 0.0
    })
  })

  def handleNewTransmittable(transmittable: Transmittable) = {
    if (!transmittable.arrived()) {
      transmittable.setEnqueueTime()
      queues(transmittable.entranceDirection)(transmittable.nextTrafficLightDirection).enqueue(transmittable)
    }
    //    else
    //      log("Car Arrived")
  }

  def doRouting(route: Route) = {
//    while (isUnderAdaptation.get())
//      Thread.sleep(100)
    try {
      var totalTiming = 0.0
      Direction.values.foreach((sourceDirection: Direction.Value) => {
        Direction.values.foreach((destinationDirection: Direction.Value) => {
          totalTiming += timings(sourceDirection)(destinationDirection)
        })
      })
      val transmittableCount = math.ceil(timings(route.sourceDirection)(route.destinationDirection) * routeCapacity / (totalTiming * transmittableSpeed)).toInt
      for (i <- 0 until transmittableCount)
        if (queues(route.sourceDirection)(route.destinationDirection).nonEmpty) {
          val transmittable = queues(route.sourceDirection)(route.destinationDirection).dequeue()
          if (neighbours.contains(route.destinationDirection)) {
            waitTimes(route.sourceDirection)(route.destinationDirection) += transmittable.waitTime
            transmittable.waitStack.push(transmittable.waitTime)
            transmittable.elapseTime(transmittableSpeed)
            neighbours(route.destinationDirection) ! transmittable.move()
            TrafficLightActorBase.increaseRouteCount()
          }
          else
            queues(route.sourceDirection)(route.destinationDirection).enqueue(transmittable)
        }

      Direction.values.foreach((sourceDirection: Direction.Value) => {
        Direction.values.foreach((destinationDirection: Direction.Value) => {
          for (transmittable <- queues(sourceDirection)(destinationDirection)) {
            transmittable.elapseTime(transmittableCount * transmittableSpeed)
          }
        })
      })

      route.destinationDirection = Direction.next(route.destinationDirection)
      if (route.sourceDirection == route.destinationDirection) {
        route.sourceDirection = Direction.next(route.sourceDirection)
        route.destinationDirection = Direction.opponent(route.destinationDirection)
      }
      context.system.scheduler.scheduleOnce((transmittableCount * transmittableSpeed * 10).milliseconds, self, route)
    }
    catch {
      case ex: Exception =>
        println(ex)
    }
  }


  def getStatus: ActorStatus = {
    val status = new ActorStatus
    status.id = self.path.name.replace("TRAFFIC_LIGHT_", "")
    status.isUnderAdaptation = isUnderAdaptation.get()
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

    status.adaptationCount = if (isUnderAdaptation.get()) 1 else 0
    status.testActorCount = 0
    for (adaptationGroup <- testActors.keySet) {
      status.testActorCount += testActors(adaptationGroup).keySet.size
    }

    status.averageAdaptationTime = adaptationTimes.average()

    status
  }

  def adaptationRequired(car: Car): Boolean = {
    var totalQueueSize = 0L
    Direction.values.foreach((sourceDirection: Direction.Value) => {
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        totalQueueSize += queues(sourceDirection)(destinationDirection).size
      })
    })
    !isUnderAdaptation.get() &&
      queues(car.entranceDirection)(car.nextTrafficLightDirection).size < totalQueueSize &&
      queues(car.entranceDirection)(car.nextTrafficLightDirection).size > totalQueueSize / 6
  }

  def startAdaptation() = {
    isUnderAdaptation.set(true)
    adaptationStartTime.set(new Date().getTime)
  }

  def endAdaptation() = {
    isUnderAdaptation.set(false)
    adaptationTimes += (new Date().getTime - adaptationStartTime.get())
  }


  def row(): Int = {
    if (rowNumber == 0)
      rowNumber = self.path.name.split('_')(2).toInt
    rowNumber
  }

  def column(): Int = {
    if (columnNumber == 0)
      columnNumber = self.path.name.split('_')(3).toInt
    columnNumber
  }

  def log(message: Any) = {
    println(s"[${self.path.name}]\t$message")
  }
}
