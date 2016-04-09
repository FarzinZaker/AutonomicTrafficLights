package trafficLightSystem

/**
  * Created by root on 2/27/16.
  */
class TrafficLightActor extends TrafficLightActorBase() {

  def receive = {

    case neighbour: Neighbour => this.synchronized {
      neighbours(neighbour.direction) = sender()
    }

    case car: Car => this.synchronized {
      if (adaptationRequired(car)) {
        Thread.sleep(200)
      }
      handleNewTransmittable(car)
    }

    case route: Route =>  this.synchronized {
      doRouting(route)
    }

    case "GET_ACTOR_STATUS" =>  this.synchronized {
      sender ! this
    }

    case _ =>
  }

}
