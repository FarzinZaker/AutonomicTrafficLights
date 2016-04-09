package trafficLightSystem

import java.util.concurrent.atomic.{AtomicReference, AtomicLong, AtomicIntegerArray}
import scala.collection._
import akka.actor.{StopChild, PoisonPill, ActorRef, Props}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
  * Created by root on 2/27/16.
  */

class TrafficLightAdaptiveWithAssuranceActor(carSpeed: Int = 5, routeCapacity: Int = 600) extends TrafficLightActorBase(carSpeed, routeCapacity) with InputCarPrediction {

  import context._

  val adaptationArray = Array(0.2, 0.4, 0.8)

  val testResults = mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, mutable.HashMap[Double, Double]]]()

  var phaseSwitchMessageCounter = 0
  var currentAdaptationGroupId: Long = -1
  var routedCars = mutable.HashMap[Long, Int]()

  context.system.scheduler.schedule(0.seconds, 1000.milliseconds, self, "ELAPSE_TIMER")

  def receive = {

    case "ELAPSE_TIMER" => elapseTimer()

    case neighbour: Neighbour => this.synchronized {
      neighbours(neighbour.direction) = sender()
    }

    case car: Car => this.synchronized {
      if (car.isNew) {
        increaseCarsCount()
        recordCarDestination(car.sourceRow, car.destinationRow)
      }
      handleNewTransmittable(car)
    }

    case route: Route => this.synchronized {
      doRouting(route)
    }

    case adaptationMessage: AdaptationMessage => this.synchronized {
      createTestActors(adaptationMessage)
    }

    case "TEST_ACTORS_CREATED" => this.synchronized {
      handleTestActorCreation()
    }

    case adaptationStepCommand: AdaptationStepCommand => this.synchronized {
      handleAdaptationStepCommand(adaptationStepCommand)
    }

    case testResult: TestResult =>
      this.synchronized {
        if (!testResults.contains(testResult.adaptationPathSourceDirection))
          testResults += testResult.adaptationPathSourceDirection -> mutable.HashMap[Direction.Value, mutable.HashMap[Double, Double]]()
        if (!testResults(testResult.adaptationPathSourceDirection).contains(testResult.adaptationPathDestinationDirection))
          testResults(testResult.adaptationPathSourceDirection) += testResult.adaptationPathDestinationDirection -> mutable.HashMap[Double, Double]()
        if (!testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection).contains(testResult.adaptationFactor))
          testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection) += testResult.adaptationFactor -> testResult.value
        if (testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection).keySet.size == adaptationArray.size) {
          var selectedAdaptationFactor = 0.0
          var minWaitTime = 0.0
          for (factor <- testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection).keySet) {
            if (minWaitTime == 0 || testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection)(factor) < minWaitTime) {
              minWaitTime = testResults(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection)(factor)
              selectedAdaptationFactor = factor
            }
          }
          //          timings(testResult.adaptationPathSourceDirection)(testResult.adaptationPathDestinationDirection) += selectedAdaptationFactor
          testResults(testResult.adaptationPathSourceDirection) -= testResult.adaptationPathDestinationDirection
          finishTestActor(testResult.adaptationGroupId, testResult.adaptationFactor)

          context.system.scheduler.scheduleOnce((10000 - elapsedAdaptationTime).milliseconds, self, new AdaptationApplyCommand(testResult.adaptationPathSourceDirection, testResult.adaptationPathDestinationDirection, selectedAdaptationFactor))
          //          self ! new AdaptationApplyCommand(testResult.adaptationPathSourceDirection, testResult.adaptationPathDestinationDirection, selectedAdaptationFactor)
        }
      }

    case finishMessage: FinishMessage => finishTestActor(finishMessage.adaptationGroupId, finishMessage.adaptationFactor)


    case adaptationApplyCommand: AdaptationApplyCommand =>
      timings(adaptationApplyCommand.sourceDirection)(adaptationApplyCommand.destinationDirection) += adaptationApplyCommand.adaptationFactor
      endAdaptation()

    //    case "CLEAR_UNDER_ADAPTATION" => endAdaptation()

    case "GET_ACTOR_STATUS" => this.synchronized {
      sender ! this
    }

    case _ =>
      log(s"UNKNOWN")
  }

  def handleNewTransmittable(car: Car) = {

    if (!routedCars.contains(car.id))
      routedCars += car.id -> 1
    else {
      routedCars(car.id) += 1
      log(s"REPETITIVE:\t${car.id}\t${routedCars(car.id)}")
    }

    if (adaptationRequired(car)) {
      startAdaptation()
//      timings(car.entranceDirection)(car.nextTrafficLightDirection) += 0.4
      phaseSwitchMessageCounter = 0
      currentAdaptationGroupId = IdGenerator.get()
      broadcast(new AdaptationMessage(self, currentAdaptationGroupId, adaptationArray, car.entranceDirection, car.nextTrafficLightDirection))
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
      val testActorRef = context.actorOf(Props(new TrafficLightTestActor()).withDispatcher(s"${"ADAPTIVE_WITH_ASSURANCE_TRAFFIC_LIGHT_SYSTEM".toLowerCase()}-prio-test-dispatcher"), s"TEST_${self.path.name}_${adaptationMessage.adaptationGroupId}_$adaptationFactor")
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
      def predictedInputCars = predictNextCars(20, row(), column())
      //            log(predictedInputCars.map{_.size})
      for (testActor <- currentTestActors.values) {
        if (predictedInputCars.exists {
          _.nonEmpty
        })
          testActor ! new PredictionResult(predictedInputCars.clone())

        //        context.system.scheduler.scheduleOnce(1000.milliseconds, self, TokenRoute(Direction.South, Direction.East))
        testActor ! TokenRoute(Direction.South, Direction.East)
      }
    }
  }

  def finishTestActor(adaptationGroupId: Long, adaptationFactor: Double) = {
    context.stop(testActors(adaptationGroupId)(adaptationFactor))
    //    testActors(adaptationGroupId)(adaptationFactor) ! PoisonPill.getInstance
    testActors(adaptationGroupId) -= adaptationFactor
    if (testActors(adaptationGroupId).isEmpty)
      testActors -= adaptationGroupId
  }

  def broadcast(message: Object) = {
    context.actorSelection("../TRAFFIC_LIGHT_*") ! message
  }
}
