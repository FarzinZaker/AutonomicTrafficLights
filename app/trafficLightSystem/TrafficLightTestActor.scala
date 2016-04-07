package trafficLightSystem

import java.util.{Date, UUID}

import akka.actor.{ActorSelection, ActorLogging, ActorRef, Actor}
import trafficLightSystem.Direction._
import scala.concurrent.duration._

import scala.collection.mutable

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
class TrafficLightTestActor(carSpeed: Int = 5, routeCapacity: Int = 60) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  //  TrafficLightTestActor.currentInstances()

  var parent: ActorRef = null
  var initiator: ActorRef = null
  var adaptationGroupId: UUID = null
  var adaptationPathSourceDirection: Direction.Value = null
  var adaptationPathDestinationDirection: Direction.Value = null
  var adaptationFactor: Double = 1.0
  var routingsDone = 0
  var resultSet = Set[Double]()

  def isLeader: Boolean = adaptationFactor != 1

  def receive = {
    case initData: TestActorInitData => this.synchronized {
      init(initData)
    }

    case token: Token => this.synchronized {
      handleNewTransmittable(token)
    }

    case tokenRoute: TokenRoute =>
      this.synchronized {
        routingsDone += 1
        if (routingsDone <= 20)
          doRouting(tokenRoute)
        else {
//                    var waitTime: Double = 0.0
//
//                    Direction.values.foreach((sourceDirection: Direction.Value) => {
//                      if (sourceDirection != None)
//                        Direction.values.foreach((destinationDirection: Direction.Value) => {
//                          if (destinationDirection != None && sourceDirection != destinationDirection &&
//                            waitTimes.contains(sourceDirection) && waitTimes(sourceDirection).contains(destinationDirection))
//                            waitTime += waitTimes(sourceDirection)(destinationDirection).average()
//                        })
//                    })
//                    parent ! new PartialTestResult(initiator, adaptationGroupId, adaptationFactor, waitTime)
        }
      }

    case partialTestResult: PartialTestResult =>
      this.synchronized {
        resultSet += partialTestResult.value
        if (resultSet.size >= 64)
          initiator ! new TestResult(adaptationPathSourceDirection, adaptationPathDestinationDirection, adaptationFactor, resultSet.sum)
      }

    case _ => log(s"UNKNOWN")
  }

  def getTargetActor(actor: ActorRef): ActorSelection = {
    context.actorSelection(s"/user/${actor.path.name}/TEST_${actor.path.name}_${adaptationGroupId}_$adaptationFactor")
    //    context.actorSelection( actor.path.toString())
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

  def doRouting(tokenRoute: TokenRoute) = {
    var totalTiming = 0.0
    Direction.values.foreach((sourceDirection: Direction.Value) => {
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        totalTiming += timings(sourceDirection)(destinationDirection)
      })
    })
    val transmittableCount = math.ceil(timings(tokenRoute.sourceDirection)(tokenRoute.destinationDirection) * routeCapacity / (totalTiming * transmittableSpeed)).toInt
    for (i <- 0 until transmittableCount)
      if (queues(tokenRoute.sourceDirection)(tokenRoute.destinationDirection).nonEmpty) {
        val transmittable = queues(tokenRoute.sourceDirection)(tokenRoute.destinationDirection).dequeue()
        transmittable.elapseTime(transmittableSpeed)
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
          transmittable.elapseTime(transmittableCount * transmittableSpeed)
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

}
