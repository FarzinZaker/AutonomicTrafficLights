package trafficLightSystem


import akka.actor.ActorRef

/**
  * Created by root on 4/4/16.
  */
class PartialTestResult(val initiator: ActorRef, val adaptationGroupId: Long, val adaptationFactor: Double, val value: Double) {

}
