package trafficLightSystem

/**
  * Created by root on 2/27/16.
  */
class TrafficLightAdaptiveActor(carSpeed: Int = 5, routeCapacity: Int = 60) extends TrafficLightActorBase(carSpeed, routeCapacity) {

  def receive = {

    case neighbour: Neighbour => neighbours(neighbour.direction) = sender()

    case car: RealCar => handleNewCar(car)

    case route: Route => self ! doRouting(route)

    case "GET_ACTOR_STATUS" => sender ! getStatus

    case _ =>
  }

  override def handleNewCar(car: RealCar) = {

    var totalQueueSize = 0L
    Direction.values.foreach((sourceDirection: Direction.Value) => {
      Direction.values.foreach((destinationDirection: Direction.Value) => {
        totalQueueSize += queues(sourceDirection)(destinationDirection).size
      })
    })

    if (queues(car.entranceDirection)(car.nextTrafficLightDirection).size < totalQueueSize &&
      queues(car.entranceDirection)(car.nextTrafficLightDirection).size > totalQueueSize / 6) {
      isUnderAdaptation = true
      timings(car.entranceDirection)(car.nextTrafficLightDirection) += 0.2
      //        new Thread(new Runnable {
      //          override def run(): Unit = {
      //            Thread.sleep(5000)
      isUnderAdaptation = false
      //          }
      //        }).start()
      //      }
    }

    super.handleNewCar(car)
  }


}
