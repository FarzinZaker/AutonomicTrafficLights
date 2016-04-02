package controllers

import play.api.mvc._
import trafficLightSystem.{DataSource, TrafficLightAdaptiveWithAssuranceGrid, TrafficLightAdaptiveGrid, TrafficLightGrid}

object Application extends Controller {

  var grid: TrafficLightGrid = null
  var adaptiveGrid: TrafficLightAdaptiveGrid = null
  var adaptiveWithAssuranceGrid: TrafficLightAdaptiveWithAssuranceGrid = null

  def index = Action {
    new DataSource(8, 8)
//    grid = new TrafficLightGrid
//    adaptiveGrid = new TrafficLightAdaptiveGrid
    adaptiveWithAssuranceGrid = new TrafficLightAdaptiveWithAssuranceGrid
    Ok(views.html.index("hello"))
  }

  def status = Action {
    if (grid != null)
      Ok(grid.getStatus)
    else
      Ok("[]")
  }

  def adaptiveStatus = Action {
    if (adaptiveGrid != null)
      Ok(adaptiveGrid.getStatus)
    else
      Ok("[]")
  }

  def adaptiveWithAssuranceStatus = Action {
    if (adaptiveWithAssuranceGrid != null)
      Ok(adaptiveWithAssuranceGrid.getStatus)
    else
      Ok("[]")
  }

}