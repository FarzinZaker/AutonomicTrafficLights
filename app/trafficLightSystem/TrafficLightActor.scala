package trafficLightSystem

/**
  * Created by root on 2/27/16.
  */
class TrafficLightActor(carSpeed: Int = 5, routeCapacity: Int = 60) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  def receive = {

    case neighbour: Neighbour => neighbours(neighbour.direction) = sender()

    case car: RealCar => handleNewCar(car)

    case route: Route => self ! doRouting(route)

    case "GET_ACTOR_STATUS" => sender ! getStatus

    case _ =>
  }

}
