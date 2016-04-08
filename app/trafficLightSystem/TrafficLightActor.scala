package trafficLightSystem

/**
  * Created by root on 2/27/16.
  */
class TrafficLightActor(carSpeed: Int = 5, routeCapacity: Int = 600) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  def receive = {

    case neighbour: Neighbour => neighbours(neighbour.direction) = sender()

    case car: Car =>
      handleNewTransmittable(car)
//      Thread.sleep(100)

    case route: Route => doRouting(route)

    case "GET_ACTOR_STATUS" => sender ! this

    case _ =>
  }

}
