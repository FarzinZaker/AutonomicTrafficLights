package trafficLightSystem

import java.util.UUID

import akka.actor.ActorRef

import scala.collection.mutable

/**
  * Created by root on 3/30/16.
  */
class TestActorInitData(val carSpeed: Int,
                        val routeCapacity: Int = 60,
                        val neighbours: mutable.HashMap[Direction.Value, ActorRef],
                        val currentTimings: mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Double]],
                        val adaptationGroup: UUID,
                        val adaptation: UUID,
                        val factor: Double = 1,
                        val adaptationPathSourceDirection: Direction.Value = Direction.None,
                        val adaptationPathDestinationDirection: Direction.Value = Direction.None) {

}
