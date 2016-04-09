package trafficLightSystem

/**
  * Created by root on 2/27/16.
  */

class TrafficLightAdaptiveGrid(rowsCount: Int, columnsCount: Int, adaptationFactor: Double) extends TrafficLightGridBase("ADAPTIVE_TRAFFIC_LIGHT_SYSTEM", rowsCount, columnsCount) {

  override protected def createActorInstance: TrafficLightActorBase = new TrafficLightAdaptiveActor(adaptationFactor)

}
