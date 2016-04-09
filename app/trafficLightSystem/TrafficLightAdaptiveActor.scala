package trafficLightSystem

import scala.concurrent.duration._

/**
  * Created by root on 2/27/16.
  */
class TrafficLightAdaptiveActor(carSpeed: Int = 5, routeCapacity: Int = 600) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  import context._

  def receive = {

    case neighbour: Neighbour => this.synchronized {
      neighbours(neighbour.direction) = sender()
    }

    case car: Car => this.synchronized {
      handleNewTransmittable(car)
    }

    case adaptationApplyCommand: AdaptationApplyCommand => this.synchronized {
      timings(adaptationApplyCommand.sourceDirection)(adaptationApplyCommand.destinationDirection) += adaptationApplyCommand.adaptationFactor
      endAdaptation()
    }

    //    case "CLEAR_UNDER_ADAPTATION" => endAdaptation()

    case route: Route => this.synchronized {
      doRouting(route)
    }

    case "GET_ACTOR_STATUS" => this.synchronized {
      sender ! this
    }

    case _ =>
  }

  def handleNewTransmittable(car: Car) = {
    if (adaptationRequired(car)) {

      startAdaptation()
      Thread.sleep(300)
      self ! new AdaptationApplyCommand(car.entranceDirection, car.nextTrafficLightDirection, 0.2)
//      context.system.scheduler.scheduleOnce(4.seconds, self, new AdaptationApplyCommand(car.entranceDirection, car.nextTrafficLightDirection, 0.2))
      //      context.system.scheduler.scheduleOnce(4.seconds, self, "CLEAR_UNDER_ADAPTATION")
      //      self ! "CLEAR_UNDER_ADAPTATION"
    }

    super.handleNewTransmittable(car)
  }


}
