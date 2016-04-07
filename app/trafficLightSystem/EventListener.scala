package trafficLightSystem

import akka.actor.{DeadLetter, Actor}

/**
  * Created by root on 4/7/16.
  */
class EventListener extends Actor {
  def receive = {
    case d: DeadLetter =>
      println(d)
  }
}
