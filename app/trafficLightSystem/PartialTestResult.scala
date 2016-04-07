package trafficLightSystem

import java.util.UUID

import akka.actor.ActorRef

/**
  * Created by root on 4/4/16.
  */
class PartialTestResult(val initiator: ActorRef, val adaptationGroupId: UUID, val adaptationFactor: Double, val value: Double) {

}
