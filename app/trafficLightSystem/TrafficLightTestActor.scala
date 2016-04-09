package trafficLightSystem

import java.util.concurrent.atomic.AtomicInteger
import java.util.Date

import akka.actor.{ActorSelection, ActorLogging, ActorRef, Actor}
import trafficLightSystem.Direction._
import scala.concurrent.duration._

import scala.collection.mutable
import scala.concurrent.stm.skel.AtomicArray

object TrafficLightTestActor {
  var instanceCounter = 0

  def currentInstances() = {
    instanceCounter += 1
    println(s"test actors: $instanceCounter")
  }
}

/**
  * Created by root on 3/11/16.
  */
class TrafficLightTestActor extends TrafficLightActorBase {

  import context._

  //  TrafficLightTestActor.currentInstances()
  var parent: ActorRef = null
  var initiator: ActorRef = null
  var adaptationGroupId: Long = -1
  var adaptationPathSourceDirection: Direction.Value = null
  var adaptationPathDestinationDirection: Direction.Value = null
  var adaptationFactor: Double = 1.0
  var routingsDone = new AtomicInteger(0)
  var resultSet = mutable.ArrayBuffer[Double]()

  def isLeader: Boolean = adaptationFactor != 1

  def receive = {
    case initData: TestActorInitData => this.synchronized {
      init(initData)
    }

    case token: Token => this.synchronized {
      handleNewTransmittable(token)
    }

    case predictionResult: PredictionResult => this.synchronized {
      handlePredictedInputCars(predictionResult)
    }

    case tokenRoute: TokenRoute => this.synchronized {
      doRouting(tokenRoute)
    }

    case partialTestResult: PartialTestResult => this.synchronized {
      handlePartialTestResult(partialTestResult)
    }

    case _ =>
    //      log(s"UNKNOWN")
  }

  def getTargetActor(actor: ActorRef): ActorSelection = {
    context.actorSelection(s"/user/${
      actor.path.name
    }/TEST_${
      actor.path.name
    }_${
      adaptationGroupId
    }_$adaptationFactor")
  }

  def init(initData: TestActorInitData) = {

    parent = initData.parent
    initiator = initData.initiator
    adaptationGroupId = initData.adaptationGroup
    adaptationFactor = initData.factor
    adaptationPathSourceDirection = initData.adaptationPathSourceDirection
    adaptationPathDestinationDirection = initData.adaptationPathDestinationDirection

    Direction.values.foreach((sourceDirection: Direction.Value) => {
      if (initData.neighbours.contains(sourceDirection))
        neighbours(sourceDirection) = initData.neighbours(sourceDirection)
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        timings(sourceDirection)(destinationDirection) = initData.currentTimings(sourceDirection)(destinationDirection)
      })
    })

    if (initData.applyAdaptation)
      timings(adaptationPathSourceDirection)(adaptationPathDestinationDirection) += adaptationFactor
  }

  def handlePredictedInputCars(predictionResult: PredictionResult) = {
    if (predictionResult.cars.nonEmpty) {
      val currentCarList = predictionResult.cars.dequeue()
      for (car <- currentCarList) {
        self ! Token(car.asInstanceOf[Car], adaptationGroupId, adaptationFactor)
      }
      //      context.system.scheduler.scheduleOnce(10.milliseconds, self, inputCars)
      self ! predictionResult
    }
  }

  def doRouting(tokenRoute: TokenRoute) = {

    if (routingsDone.getAndIncrement() < 20) {
      var totalTiming = 0.0
      Direction.values.foreach((sourceDirection: Direction.Value) => {
        Direction.values.foreach((destinationDirection: Direction.Value) => {
          totalTiming += timings(sourceDirection)(destinationDirection)
        })
      })

      val transmittableCount = math.ceil(timings(tokenRoute.sourceDirection)(tokenRoute.destinationDirection) * routeCapacity / (totalTiming * carSpeed)).toInt
      for (i <- 0 until transmittableCount)
        if (queues(tokenRoute.sourceDirection)(tokenRoute.destinationDirection).nonEmpty) {
          val transmittable = queues(tokenRoute.sourceDirection)(tokenRoute.destinationDirection).dequeue()
          transmittable.elapseTime(carSpeed)
          if (neighbours.contains(tokenRoute.destinationDirection)) {
            waitTimes(tokenRoute.sourceDirection)(tokenRoute.destinationDirection) += transmittable.waitTime
            transmittable.waitStack.push(transmittable.waitTime)
            getTargetActor(neighbours(tokenRoute.destinationDirection)) ! transmittable.move()
          }
          else
            queues(tokenRoute.sourceDirection)(tokenRoute.destinationDirection).enqueue(transmittable)
        }

      Direction.values.foreach((sourceDirection: Direction.Value) => {
        Direction.values.foreach((destinationDirection: Direction.Value) => {
          for (transmittable <- queues(sourceDirection)(destinationDirection)) {
            transmittable.elapseTime(transmittableCount * carSpeed)
          }
        })
      })

      tokenRoute.destinationDirection = Direction.next(tokenRoute.destinationDirection)
      if (tokenRoute.sourceDirection == tokenRoute.destinationDirection) {
        tokenRoute.sourceDirection = Direction.next(tokenRoute.sourceDirection)
        tokenRoute.destinationDirection = Direction.opponent(tokenRoute.destinationDirection)
      }
      self ! tokenRoute
    }
    else {
      if (initiator != null && parent != null) {
        var waitTime: Double = 0.0

        Direction.values.foreach((sourceDirection: Direction.Value) => {
          if (sourceDirection != None)
            Direction.values.foreach((destinationDirection: Direction.Value) => {
              if (destinationDirection != None && sourceDirection != destinationDirection &&
                waitTimes.contains(sourceDirection) && waitTimes(sourceDirection).contains(destinationDirection))
                waitTime += waitTimes(sourceDirection)(destinationDirection).average()
            })
        })
        getTargetActor(initiator) ! new PartialTestResult(initiator, adaptationGroupId, adaptationFactor, waitTime)

        if (initiator != parent)
        //                    context.system.scheduler.scheduleOnce(5.seconds, parent, new FinishMessage(adaptationGroupId, adaptationFactor))
          parent ! new FinishMessage(adaptationGroupId, adaptationFactor)
      }
      else
        context.system.scheduler.scheduleOnce(10.milliseconds, self, tokenRoute)

    }
  }

  def handlePartialTestResult(partialTestResult: PartialTestResult) = {
    resultSet.synchronized {
      resultSet += partialTestResult.value
    }
    if (resultSet.size == 64) {
      parent ! new TestResult(adaptationGroupId, adaptationPathSourceDirection, adaptationPathDestinationDirection, adaptationFactor, resultSet.sum)
    }
  }

  override def row(): Int = {
    if (rowNumber == 0)
      rowNumber = self.path.name.split('_')(3).toInt
    rowNumber
  }

  override def column(): Int = {
    if (columnNumber == 0)
      columnNumber = self.path.name.split('_')(4).toInt
    columnNumber
  }

}
