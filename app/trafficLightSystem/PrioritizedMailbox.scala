package trafficLightSystem

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

/**
  * Created by root on 3/10/16.
  */
class PrioritizedMailbox(settings: ActorSystem.Settings, cfg: Config)
  extends UnboundedPriorityMailbox(
    PriorityGenerator {
      case "GET_ACTOR_STATUS" =>
        0
      case status: ActorStatus =>
        0
      case adaptationMessage: AdaptationMessage =>
        1
      case partialTestResult: PartialTestResult =>
        1
      case testResult: TestResult =>
        1
      case token: Token =>
        1
//      case route: Route =>
//        1
      case _ =>
        10
    })
