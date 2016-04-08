package trafficLightSystem

import scala.concurrent.duration._

/**
  * Created by root on 2/27/16.
  */
class TrafficLightAdaptiveActor(carSpeed: Int = 5, routeCapacity: Int = 600) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  import context._

  def receive = {

    case neighbour: Neighbour => neighbours(neighbour.direction) = sender()

    case car: Car =>
      handleNewTransmittable(car)
//      Thread.sleep(100)

    case "CLEAR_UNDER_ADAPTATION" => isUnderAdaptation.set(false)

    case route: Route => doRouting(route)

    case "GET_ACTOR_STATUS" => sender ! this

    case _ =>
  }

  def handleNewTransmittable(car: Car) = {

    var totalQueueSize = 0L
    Direction.values.foreach((sourceDirection: Direction.Value) => {
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        totalQueueSize += queues(sourceDirection)(destinationDirection).size
      })
    })

    if (queues(car.entranceDirection)(car.nextTrafficLightDirection).size < totalQueueSize &&
      queues(car.entranceDirection)(car.nextTrafficLightDirection).size > totalQueueSize / 6) {
      isUnderAdaptation.set(true)
      timings(car.entranceDirection)(car.nextTrafficLightDirection) += 0.2

      context.system.scheduler.scheduleOnce(5.seconds, self, "CLEAR_UNDER_ADAPTATION")
    }

    super.handleNewTransmittable(car)
  }


}
