package trafficLightSystem

import java.util.UUID

import akka.actor.ActorRef

import scala.collection.mutable

/**
  * Created by root on 3/2/16.
  */
class AdaptationMessage(val initiator: ActorRef, val adaptationGroupId: UUID, val adaptationFactors: Array[Double], val adaptationSourceDirection: Direction.Value, val adaptationDestinationDirection: Direction.Value) {

}
