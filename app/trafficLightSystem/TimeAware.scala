package trafficLightSystem

import java.util.concurrent.atomic.AtomicLong

/**
  * Created by root on 3/26/16.
  */
trait TimeAware {

  protected var currentTimeInternal = new AtomicLong(0)
  //  protected var creationTimeInternal = 0L
  protected var enqueueTimeInternal = new AtomicLong(0)

  def elapseTime(time: Long) = {
    currentTimeInternal.addAndGet(time)
  }

  def elapseTime(time: Double) = {
    currentTimeInternal.addAndGet(Math.round(time))
  }

  def elapsedTime(): Long = {
    currentTimeInternal.get() // - creationTimeInternal
  }

  def setEnqueueTime() = {
    enqueueTimeInternal.set(currentTimeInternal.get())
  }

  def waitTime: Long = {
    currentTimeInternal.get() - enqueueTimeInternal.get()
  }
}
