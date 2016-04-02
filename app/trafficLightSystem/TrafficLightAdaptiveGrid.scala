package trafficLightSystem

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString}
import trafficLightSystem.Direction._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

/**
  * Created by root on 2/27/16.
  */

class TrafficLightAdaptiveGrid {

  val rowsCount: Int = 8
  val columnsCount: Int = 8
  var feedingRounds = 3


  val carList = new ArrayBuffer[Car]()

  def getArrivedCarsList: ArrayBuffer[Long] = {
    val result = new ArrayBuffer[Long]()
    for (car <- carList) {
      if (car.arrived())
        result += car.speed()
    }
    result
  }

  private val grid = Array.ofDim[ActorRef](rowsCount, columnsCount)
  private val gridActors = Array.ofDim[TrafficLightAdaptiveActor](rowsCount, columnsCount)


  val system = ActorSystem("TRAFFIC_LIGHT_ADAPTIVE_SYSTEM", com.typesafe.config.ConfigFactory.parseString(
    """ prio-dispatcher {
            type = "Dispatcher"
            mailbox-type = "%s"
          }""".format(classOf[PrioritizedMailbox].getName)))
  //  val system = ActorSystem("TRAFFIC_LIGHT_SYSTEM")

  //init
  for {
    i <- 0 until rowsCount
    j <- 0 until columnsCount
  } {
    grid(i)(j) = system.actorOf(Props(new TrafficLightAdaptiveActor()).withDispatcher("prio-dispatcher"), s"TRAFFIC_LIGHT_${i + 1}_${j + 1}")
  }

  //set neighbours
  for {
    i <- 0 until rowsCount
    j <- 0 until columnsCount
  } {
    if (i > 0)
      grid(i)(j).tell(Neighbour(North), grid(i - 1)(j))
    if (i < rowsCount - 1)
      grid(i)(j).tell(Neighbour(South), grid(i + 1)(j))
    if (j > 0)
      grid(i)(j).tell(Neighbour(West), grid(i)(j - 1))
    if (j < columnsCount - 1)
      grid(i)(j).tell(Neighbour(East), grid(i)(j + 1))

    grid(i)(j).tell(Route(Direction.South, Direction.East), ActorRef.noSender)

  }

  var feedingRound = 1

  new Thread(new Runnable {
    override def run(): Unit = {

      while (feedingRound <= feedingRounds) {
        //feed data
        for (line <- Source.fromFile(DataSource.carDataFileName).getLines()) {
          val parts = line.toString.split(' ')
          if (parts(0) == "sleep") {
            Thread.sleep(parts(1).toLong)
          }
          else {
            val car = Car.parseRealCar(parts)
            if (car != null) {
              carList += car
              grid(car.path.sourceRow)(car.path.sourceColumn).tell(car, ActorRef.noSender)
              //Thread sleep 100
            }
          }
        }
        feedingRound += 1
      }
    }
  }).start()

  def getStatus: JsObject = {


    val actorStatusList = new ArrayBuffer[ActorStatus]()

    for {
      i <- 0 until rowsCount
      j <- 0 until columnsCount
    } {
      actorStatusList += null
    }

    implicit val timeout = Timeout(60 seconds)

    //    (0 until rowsCount).par.foreach {
    //      i =>
    //        (0 until columnsCount).par.foreach {
    //          j =>
    //            val future = grid(i)(j) ? "GET_ACTOR_STATUS"
    //            actorStatusList(i * j + j) = Await.result(future, timeout.duration).asInstanceOf[ActorStatus]
    //        }
    //    }

    val threads = Array.ofDim[Thread](rowsCount, columnsCount)
    for {
      i <- 0 until rowsCount
      j <- 0 until columnsCount
    } {
      threads(i)(j) = new Thread(new Runnable {
        override def run(): Unit = {
          val future = grid(i)(j) ? "GET_ACTOR_STATUS"
          actorStatusList += Await.result(future, timeout.duration).asInstanceOf[ActorStatus]
        }
      })
      threads(i)(j).start()
    }
    for {
      i <- 0 until rowsCount
      j <- 0 until columnsCount
    } {
      threads(i)(j).join()
    }


    val nodes = new ArrayBuffer[JsObject]
    for (i <- 1 to rowsCount) {
      nodes += JsObject(Seq(
        "key" -> JsString(s"${i}_0"),
        "text" -> JsString(s"N:$i"),
        "color" -> JsString("white"),
        "textColor" -> JsString("#000000"),
        "loc" -> JsString(s"${i * 200 - 1} ${0 * 120}")
      ))
      nodes += JsObject(Seq(
        "key" -> JsString(s"${i}_${columnsCount + 1}"),
        "text" -> JsString(s"S:$i"),
        "color" -> JsString("white"),
        "textColor" -> JsString("#000000"),
        "loc" -> JsString(s"${i * 200 - 1} ${(columnsCount + 1) * 120}")
      ))
    }
    for (j <- 1 to columnsCount) {
      nodes += JsObject(Seq(
        "key" -> JsString(s"0_$j"),
        "text" -> JsString(s"W:$j"),
        "color" -> JsString("white"),
        "textColor" -> JsString("#000000"),
        "loc" -> JsString(s"${0 * 200} ${j * 120}")
      ))
      nodes += JsObject(Seq(
        "key" -> JsString(s"${rowsCount + 1}_$j"),
        "text" -> JsString(s"E:$j"),
        "color" -> JsString("white"),
        "textColor" -> JsString("#000000"),
        "loc" -> JsString(s"${(rowsCount + 1) * 200} ${j * 120}")
      ))
    }

    var links = new ArrayBuffer[JsObject]
    actorStatusList.foreach((status: ActorStatus) => {
      if (status != null) {
        val json = status.toJson
        json("nodes").foreach((node: JsObject) =>
          nodes += node
        )
        json("links").foreach((link: JsObject) =>
          links += link
        )
      }
    })

    var enqueuedCars = 0
    var totalAverageWaitTime = 0L
    links.foreach((link: JsObject) => {
      totalAverageWaitTime += link.value("avgWaitTime").asInstanceOf[JsNumber].value.toLongExact
      enqueuedCars += link.value("queueSize").asInstanceOf[JsNumber].value.toInt
    })
    val avgWaitTime = totalAverageWaitTime / links.size
    links = links.collect {
      case link: JsObject =>
        if (link.value("avgWaitTime").asInstanceOf[JsNumber].value.toLongExact > avgWaitTime * 3)
          link.+("color", new JsString("#801515"))
        else link
    }

    val arrivedCars = getArrivedCarsList
    JsObject(Seq(
      "nodes" -> JsArray(nodes toSeq),
      "links" -> JsArray(links toSeq),
      "status" -> JsString(if (feedingRound > feedingRounds) "FEEDING COMPLETED" else s"FEEDING ROUND: $feedingRound"),
      "enqueuedCars" -> JsString(if (enqueuedCars < 1) "NO WAITING CAR" else s"WAITING CARS: $enqueuedCars"),
      "cars" -> JsObject(Seq(
        "count" -> JsNumber(arrivedCars.size),
        "avgArivalTime" -> JsNumber(if (arrivedCars.nonEmpty) arrivedCars.sum / arrivedCars.size else 0)
      ))
    ))
  }
}
