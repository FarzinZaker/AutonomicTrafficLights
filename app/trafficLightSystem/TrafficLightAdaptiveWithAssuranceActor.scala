package trafficLightSystem

import akka.actor.Props

/**
  * Created by root on 2/27/16.
  */
class TrafficLightAdaptiveWithAssuranceActor(carSpeed: Int = 5, routeCapacity: Int = 60) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  def receive = {

    case neighbour: Neighbour => neighbours(neighbour.direction) = sender()

    case car: RealCar => handleNewCar(car)

    case car: TestCar =>
      for (testActor <- testActors) {
        testActor.tell(car, sender())
      }

    case route: Route => self ! doRouting(route)

    case adaptationMessage: AdaptationMessage => {
      var testActorData: TestActorInitData = null
      if (adaptationMessage.initiator == self)
        testActorData = new TestActorInitData(carSpeed, routeCapacity, neighbours, timings, adaptationMessage.adaptationGroupId, adaptationMessage.adaptationId, adaptationMessage.adaptationFactor, adaptationMessage.adaptationSourceDirection, adaptationMessage.adaptationDestinationDirection)
      else
        testActorData = new TestActorInitData(carSpeed, routeCapacity, neighbours, timings, adaptationMessage.adaptationGroupId, adaptationMessage.adaptationId)
      val testActorRef = context.actorOf(Props(new TrafficLightTestActor()).withDispatcher("prio-dispatcher"), s"TEST_${self.path.name}_${adaptationMessage.adaptationGroupId}_${adaptationMessage.adaptationId}")
            testActors += testActorRef
            testActorRef ! testActorData
      //      Direction.values.foreach((sourceDirection: Direction.Value) => {
      //        Direction.values.foreach((destinationDirection: Direction.Value) => {
      //          queues(sourceDirection)(destinationDirection).foreach((car:Car) => {
      //            testActorRef ! car
      //          })
      //        })
      //      })
    }

    case "GET_ACTOR_STATUS" => sender ! getStatus

    case _ =>
  }

  override def handleNewCar(car: RealCar) = {
    super.handleNewCar(car)
//    if (!car.arrived()) {
//      car.setEnqueueTime()
//      queues(car.entranceDirection)(car.nextTrafficLightDirection).enqueue(car)
//
//      var totalQueueSize = 0L
//      Direction.values.foreach((sourceDirection: Direction.Value) => {
//        Direction.values.foreach((destinationDirection: Direction.Value) => {
//          totalQueueSize += queues(sourceDirection)(destinationDirection).size
//        })
//      })
//      if (!isUnderAdaptation && queues(car.entranceDirection)(car.nextTrafficLightDirection).size > totalQueueSize / 2) {
//        isUnderAdaptation = true
//        def adaptationGroup = UUID.randomUUID()
//        for (factor <- 2 to 4) {
//          context.actorSelection("../TRAFFIC_LIGHT_*") ! new AdaptationMessage(self, adaptationGroup, factor, car.entranceDirection, car.nextTrafficLightDirection)
//        }
//      }
//    }
  }
}
