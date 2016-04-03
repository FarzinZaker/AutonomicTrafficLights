package trafficLightSystem

/**
  * Created by root on 2/27/16.
  */

class TrafficLightAdaptiveWithAssuranceGrid(rowsCount: Int = 8, columnsCount: Int = 8) extends TrafficLightGridBase(rowsCount, columnsCount) {

  override protected def createActorInstance: TrafficLightActorBase = new TrafficLightAdaptiveWithAssuranceActor()

}
