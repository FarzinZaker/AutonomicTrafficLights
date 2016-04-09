package trafficLightSystem

/**
  * Created by root on 2/27/16.
  */

class TrafficLightGrid(rowsCount: Int, columnsCount: Int) extends TrafficLightGridBase("TRAFFIC_LIGHT_SYSTEM", rowsCount, columnsCount) {

  override protected def createActorInstance: TrafficLightActorBase = new TrafficLightActor()

}
