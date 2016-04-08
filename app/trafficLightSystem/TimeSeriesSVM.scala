package trafficLightSystem

import java.text.NumberFormat

import org.encog.Encog
import org.encog.ml.data.basic.BasicMLData
import org.encog.ml.data.{MLData, MLDataSet}
import org.encog.ml.svm.SVM
import org.encog.ml.svm.training.SVMTrain
import org.encog.util.EngineArray
import org.encog.util.arrayutil.{TemporalWindowArray, NormalizeArray}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by root on 4/8/16.
  */
class TimeSeriesSVM(val data: ArrayBuffer[Double]) {

  private val WINDOW_SIZE: Int = Math.round(data.length * 8 / 10)
  private var normalizedSunspots: Array[Double] = null
  private var closedLoopSunspots: Array[Double] = null
  private var network: SVM = null
  private var training: MLDataSet = null

  private def init() = {
    copyData()
    network = createNetwork
    training = generateTraining
    train(network, training)
  }

  private def shutdown() = {
//    Encog.getInstance().shutdown()
  }

  private def copyData() {
    normalizedSunspots = EngineArray.arrayCopy(data.toArray)
    closedLoopSunspots = EngineArray.arrayCopy(data.toArray)
  }

  private def generateTraining: MLDataSet = {
    val temp: TemporalWindowArray = new TemporalWindowArray(WINDOW_SIZE, 1)
    temp.analyze(normalizedSunspots)
    temp.process(normalizedSunspots)
  }

  private def createNetwork: SVM = {
    val network: SVM = new SVM(WINDOW_SIZE, true)
    network
  }

  private def train(network: SVM, training: MLDataSet) {
    val train: SVMTrain = new SVMTrain(network, training)
    train.iteration()
  }

  private def internalPredict(): Int = {
    val input: MLData = new BasicMLData(WINDOW_SIZE)
    for (i <- 0 until input.size()) {
      input.setData(i, normalizedSunspots(data.length - WINDOW_SIZE + i))
    }
    val output: MLData = network.compute(input)
    output.getData(0).toInt
  }

  def predict(): Int = {
    init()
    val result = internalPredict()
    shutdown()
    result
  }

  def predict(count: Int): ArrayBuffer[Int] = {
    val result = ArrayBuffer[Int]()
    init()
    result += internalPredict()
    for (index <- 1 until count) {
      data += result.last
      init()
      result += internalPredict()
    }
    shutdown()
    result
  }
}
