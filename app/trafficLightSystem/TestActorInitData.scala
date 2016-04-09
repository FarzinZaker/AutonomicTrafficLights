package trafficLightSystem


import akka.actor.ActorRef

import scala.collection.mutable

/**
  * Created by root on 3/30/16.
  */
class TestActorInitData(val parent: ActorRef,
                        val initiator: ActorRef,
                        val neighbours: mutable.HashMap[Direction.Value, ActorRef],
                        val currentTimings: mutable.HashMap[Direction.Value, mutable.HashMap[Direction.Value, Double]],
                        val adaptationGroup: Long,
                        val factor: Double,
                        val applyAdaptation: Boolean = false,
                        val adaptationPathSourceDirection: Direction.Value = Direction.None,
                        val adaptationPathDestinationDirection: Direction.Value = Direction.None) {

}
