package trafficLightSystem

import java.util.concurrent.atomic.AtomicLong

/**
  * Created by root on 4/9/16.
  */

object IdGenerator {
  private val currentId = new AtomicLong(0)

  def get(): Long = {
    currentId.incrementAndGet()
  }
}

class IdGenerator {

}
