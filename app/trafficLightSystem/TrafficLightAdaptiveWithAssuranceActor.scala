package trafficLightSystem

import java.util.UUID
import scala.collection._
import akka.actor.{ActorRef, Props}

/**
  * Created by root on 2/27/16.
  */

class TrafficLightAdaptiveWithAssuranceActor(carSpeed: Int = 5, routeCapacity: Int = 600) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  def testResults = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, mutable.HashMap[Double, Double]]]()

  var phaseSwitchMessageCounter = 0
  var currentAdaptationGroupId: UUID = null
  var routedCars = mutable.HashMap[UUID, Int]()

  def receive = {

    case neighbour: Neighbour => this.synchronized{ neighbours(neighbour.direction) = sender() }

    case car: Car => this.synchronized{ handleNewTransmittable(car) }

    case route: Route => this.synchronized{ doRouting(route) }

    case adaptationMessage: AdaptationMessage => this.synchronized{ createTestActors(adaptationMessage) }

    case "TEST_ACTORS_CREATED" => this.synchronized{ handleTestActorCreation() }

    case adaptationStepCommand: AdaptationStepCommand => this.synchronized{ handleAdaptationStepCommand(adaptationStepCommand) }

    case partialTestResult: PartialTestResult => this.synchronized{ handlePartialTestResult(partialTestResult) }

    case testResult: TestResult =>
      this.synchronized {
        if (!testResults.contains(testResult.adaptationPathSourceDirection))
          testResults += testResult.adaptationPathSourceDirection -> mutable.HashMap[Direction.Value, mutable.HashMap[Double, Double]]()
        if (!testResults(testResult.adaptationPathSourceDirection).contains(testResult.adaptationPathDestinationDirection))
          testResults(testResult.adaptationPathSourceDirection) += testResult.adaptationPathDestinationDirection -> mutable.HashMap[Double, Double]()
        if (!testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection).contains(testResult.adaptationFactor))
          testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection) += testResult.adaptationFactor -> testResult.value
        if (testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection).keySet.size >= 3) {
          var selectedAdaptationFactor = 1.0
          var minWaitTime = 0.0
          for (factor <- testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection).keySet) {
            if (minWaitTime == 0 || testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection)(factor) < minWaitTime) {
              minWaitTime = testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection)(factor)
              selectedAdaptationFactor = factor
            }
          }
          println(s"adaptation factor: $selectedAdaptationFactor")
          timings(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection) += selectedAdaptationFactor
          testResults(testResult.adaptationPathSourceDirection) -= testResult.adaptationPathDestinationDirection
          isUnderAdaptation = false
        }
      }

    case "GET_ACTOR_STATUS" => this.synchronized{ sender ! getStatus }

    case _ => log(s"UNKNOWN")
  }

  def handleNewTransmittable(car: Car) = {

    if (!routedCars.contains(car.id))
      routedCars += car.id -> 1
    else {
      routedCars(car.id) += 1
      log(s"REPETITIVE:\t${car.id}\t${routedCars(car.id)}")
    }
    var totalQueueSize = 0L
    Direction.values.foreach((sourceDirection: Direction.Value) => {
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        totalQueueSize += queues(sourceDirection)(destinationDirection).size
      })
    })

    if (!isUnderAdaptation &&
      queues(car.entranceDirection)(car.nextTrafficLightDirection).size < totalQueueSize &&
      queues(car.entranceDirection)(car.nextTrafficLightDirection).size > totalQueueSize / 6) {
      isUnderAdaptation = true
      phaseSwitchMessageCounter = 0
      currentAdaptationGroupId = UUID.randomUUID()
      broadcast(new AdaptationMessage(self, currentAdaptationGroupId, Array(0.2, 0.3, 0.4), car.entranceDirection, car.nextTrafficLightDirection))
    }

    super.handleNewTransmittable(car)
  }

  def createTestActors(adaptationMessage: AdaptationMessage) = {
    for (adaptationFactor <- adaptationMessage.adaptationFactors) {
      var testActorData: TestActorInitData = null
      if (adaptationMessage.initiator == self)
        testActorData = new TestActorInitData(self, adaptationMessage.initiator, neighbours, timings, adaptationMessage.adaptationGroupId, adaptationFactor, true, adaptationMessage.adaptationSourceDirection, adaptationMessage.adaptationDestinationDirection)
      else
        testActorData = new TestActorInitData(self, adaptationMessage.initiator, neighbours, timings, adaptationMessage.adaptationGroupId, adaptationFactor)
      val testActorRef = context.actorOf(Props(new TrafficLightTestActor()).withDispatcher(s"${"ADAPTIVE_WITH_ASSURANCE_TRAFFIC_LIGHT_SYSTEM".toLowerCase()}-prio-dispatcher"), s"TEST_${self.path.name}_${adaptationMessage.adaptationGroupId}_$adaptationFactor")
      if (!testActors.contains(adaptationMessage.adaptationGroupId))
        testActors += adaptationMessage.adaptationGroupId -> mutable.HashMap()
      if (!testActors(adaptationMessage.adaptationGroupId).contains(adaptationFactor))
        testActors(adaptationMessage.adaptationGroupId) += adaptationFactor -> testActorRef
      testActorRef ! testActorData
      Direction.values.foreach((sourceDirection: Direction.Value) => {
        Direction.values.foreach((destinationDirection: Direction.Value) => {
          queues(sourceDirection)(destinationDirection).foreach((car: Transmittable) => {
            testActorRef ! Token(car.asInstanceOf[Car], adaptationMessage.adaptationGroupId, adaptationFactor)
          })
        })
      })
    }

    adaptationMessage.initiator ! "TEST_ACTORS_CREATED"
  }

  def handleTestActorCreation() = {
    phaseSwitchMessageCounter += 1
    if (phaseSwitchMessageCounter == 64) {
      broadcast(new AdaptationStepCommand("START_ROUTING", currentAdaptationGroupId))
    }
  }

  def handleAdaptationStepCommand(adaptationStepCommand: AdaptationStepCommand) = {
    val currentTestActors = testActors(adaptationStepCommand.adaptationGroupId)
    if (adaptationStepCommand.command == "START_ROUTING") {
      for (testActor <- currentTestActors.values) {
        testActor ! TokenRoute(Direction.South, Direction.East)
      }
    }
    //    log("routing completed")
  }

  def handlePartialTestResult(partialTestResult: PartialTestResult) = {
    //    testActors(partialTestResult.adaptationGroupId)(partialTestResult.adaptationFactor).tell(partialTestResult, sender())
  }

  def broadcast(message: Object) = {
    context.actorSelection("../TRAFFIC_LIGHT_*") ! message
  }
}
