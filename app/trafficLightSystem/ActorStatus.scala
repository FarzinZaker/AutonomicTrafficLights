package trafficLightSystem

import play.api.libs.json._

import scala.collection.mutable
import trafficLightSystem.Direction._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by root on 3/3/16.
  */
class ActorStatus {

  var id = ""

  var test = false
  var isUnderAdaptation = false

  var queueSizeList: mutable.Map[Direction.Value, Int] = mutable.HashMap[Direction.Value, Int](
    North -> 0,
    East -> 0,
    South -> 0,
    West -> 0
  )

  var neighbourList: mutable.Map[Direction.Value, String] = mutable.HashMap[Direction.Value, String](
    North -> "",
    East -> "",
    South -> "",
    West -> ""
  )

  var averageWaitTimeList: mutable.Map[Direction.Value, Long] = mutable.HashMap[Direction.Value, Long](
    North -> 0,
    East -> 0,
    South -> 0,
    West -> 0
  )

  var adaptationCount = 0
  var testActorCount = 0

  def getQueueSize: Int = {
    queueSizeList(North) + queueSizeList(East) + queueSizeList(South) + queueSizeList(West)
  }

  def toJson: mutable.Map[String, ArrayBuffer[JsObject]] = {
    val position = id.split("_")
    val x = position(0).toInt
    val y = position(1).toInt
    val nodes = ArrayBuffer.empty[JsObject]
    val links = ArrayBuffer.empty[JsObject]

    try {
      nodes += JsObject(Seq(
        "key" -> JsString(id),
        "text" -> JsString(s"$y:$x"),
        "loc" -> JsString(s"${x * 200} ${y * 120}"),
        "color" -> (if (isUnderAdaptation) JsString("#748ba7") else JsString("#eeeeee")),
        "textColor" -> (if (isUnderAdaptation) JsString("#ffffff") else JsString("#000000")),
        "tests" -> JsString(s"TEST: $testActorCount"),
        "adaptations" -> JsString(s"ADPT: $adaptationCount")
      ))

      //      "testActorList" -> JsArray(testActorList.collect {
      //        case actorStatus: ActorStatus => actorStatus.toJson
      //      } toSeq)


      //North
      links += JsObject(Seq(
        "from" -> JsString(s"${x}_${y - 1}"),
        "to" -> JsString(id),
        "avgWaitTime" -> JsNumber(averageWaitTimeList(North)),
        "queueSize" -> JsNumber(queueSizeList(North)),
        "text" -> JsString(s"${queueSizeList(North)}:${averageWaitTimeList(North)} ▼"),
        "color" -> JsString("gray"),
        "curviness" -> JsNumber(20),
        "trl" -> JsString("-35 15")
      ))

      //East
      links += JsObject(Seq(
        "from" -> JsString(s"${x + 1}_$y"),
        "to" -> JsString(id),
        "avgWaitTime" -> JsNumber(averageWaitTimeList(East)),
        "queueSize" -> JsNumber(queueSizeList(East)),
        "text" -> JsString(s"◄ ${queueSizeList(East)}:${averageWaitTimeList(East)}"),
        "color" -> JsString("gray"),
        "curviness" -> JsNumber(20),
        "trl" -> JsString("0 -15")
      ))

      //South
      links += JsObject(Seq(
        "from" -> JsString(s"${x}_${y + 1}"),
        "to" -> JsString(id),
        "avgWaitTime" -> JsNumber(averageWaitTimeList(South)),
        "queueSize" -> JsNumber(queueSizeList(South)),
        "text" -> JsString(s"▲ ${queueSizeList(South)}:${averageWaitTimeList(South)}"),
        "color" -> JsString("gray"),
        "curviness" -> JsNumber(-20),
        "trl" -> JsString("35 -15")
      ))

      //West
      links += JsObject(Seq(
        "from" -> JsString(s"${x - 1}_$y"),
        "to" -> JsString(id),
        "avgWaitTime" -> JsNumber(averageWaitTimeList(West)),
        "queueSize" -> JsNumber(queueSizeList(West)),
        "text" -> JsString(s"${queueSizeList(West)}:${averageWaitTimeList(West)} ►"),
        "color" -> JsString("gray"),
        "curviness" -> JsNumber(-20),
        "trl" -> JsString("0 15")
      ))
    }
    catch {
      case ex: Exception =>
        println(ex.getMessage)
    }
    mutable.HashMap[String, ArrayBuffer[JsObject]](
      "nodes" -> nodes,
      "links" -> links
    )
  }

}
