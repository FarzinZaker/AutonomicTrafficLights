package trafficLightSystem

/**
  * Created by root on 3/26/16.
  */
trait TimeAware {

  protected var currentTimeInternal = 0L
//  protected var creationTimeInternal = 0L
  protected var enqueueTimeInternal = 0L

  def elapseTime(time: Long) = {
    currentTimeInternal += time * 1000
  }

  def elapseTime(time: Double) = {
    currentTimeInternal += Math.round(time * 1000)
  }

  def elapsedTime(): Long = {
    currentTimeInternal// - creationTimeInternal
  }

  def setEnqueueTime() = {
    enqueueTimeInternal = currentTimeInternal
  }

  def waitTime: Long = {
    currentTimeInternal - enqueueTimeInternal
  }
}
