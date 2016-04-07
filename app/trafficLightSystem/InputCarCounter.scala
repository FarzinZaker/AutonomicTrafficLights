package trafficLightSystem

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by root on 4/7/16.
  */
trait InputCarCounter {

  val carInputCountHistory = ArrayBuffer[AtomicInteger]()

  def elapseTimer() = {
    carInputCountHistory.synchronized {
      carInputCountHistory += new AtomicInteger(0)
    }
  }

  def increaseCarsCount() = {
    carInputCountHistory.synchronized{
      carInputCountHistory.last.incrementAndGet()
    }
  }
}
