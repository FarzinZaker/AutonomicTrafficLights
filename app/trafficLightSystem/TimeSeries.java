package trafficLightSystem;

import org.encog.Encog;
import org.encog.ml.MLMethod;
import org.encog.ml.MLRegression;
import org.encog.ml.MLResettable;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.temporal.TemporalDataDescription;
import org.encog.ml.data.temporal.TemporalMLDataSet;
import org.encog.ml.data.temporal.TemporalPoint;
import org.encog.ml.factory.MLMethodFactory;
import org.encog.ml.factory.MLTrainFactory;
import org.encog.ml.train.MLTrain;
import org.encog.ml.train.strategy.RequiredImprovementStrategy;
import org.encog.neural.networks.training.propagation.manhattan.ManhattanPropagation;
import org.encog.util.arrayutil.NormalizationAction;
import org.encog.util.arrayutil.NormalizedField;
import org.encog.util.csv.ReadCSV;
import org.encog.util.simple.EncogUtility;

/**
 * Created by root on 4/7/16.
 */
public class TimeSeries {

    public static void predict(int[] input) {
//        TemporalMLDataSet result = new TemporalMLDataSet(30, 1);
//        TemporalDataDescription desc = new TemporalDataDescription(TemporalDataDescription.Type.RAW, true, false);
//        result.addDescription(desc);
//        for(int i = 0; i < input.length; i++) {
//            TemporalPoint point = new TemporalPoint(1);
//            point.setSequence(i + 1);
//            point.setData(0, input[i]);
//            result.getPoints().add(point);
//        }
//        result.generate();
//        return result;

        TemporalMLDataSet trainingData = createTraining(input);
        // Step 3. Create and train the model.
        // All sorts of models can be used here, see the XORFactory
        // example for more info.
        MLRegression model = trainModel(
                trainingData,
                MLMethodFactory.TYPE_FEEDFORWARD,
                "?:B->SIGMOID->25:B->SIGMOID->?",
                MLTrainFactory.TYPE_RPROP,
                "");

        // Now predict
        predict(input, model);

        Encog.getInstance().shutdown();

    }

    public static TemporalMLDataSet initDataSet() {
        // create a temporal data set
        TemporalMLDataSet dataSet = new TemporalMLDataSet(30, 1);

        // we are dealing with two columns.
        // The first is the sunspot number. This is both an input (used to
        // predict) and an output (we want to predict it), so true,true.
        TemporalDataDescription sunSpotNumberDesc = new TemporalDataDescription(TemporalDataDescription.Type.RAW, true, true);

        // The second is the standard deviation for the month. This is an
        // input (used to predict) only, so true,false.
        TemporalDataDescription standardDevDesc = new TemporalDataDescription(TemporalDataDescription.Type.RAW, true, false);
        dataSet.addDescription(sunSpotNumberDesc);
        dataSet.addDescription(standardDevDesc);
        return dataSet;
    }

    public static TemporalMLDataSet createTraining(int[] input) {
        TemporalMLDataSet trainingData = initDataSet();
        for (int i = 0; i < input.length; i++) {
            int sequenceNumber = i + 1;

            TemporalPoint point = new TemporalPoint(trainingData.getDescriptions().size());
            point.setSequence(sequenceNumber);
            point.setData(0, input[i]);
            trainingData.getPoints().add(point);
        }

        // generate the time-boxed data
        trainingData.generate();
        return trainingData;
    }

    public static MLRegression trainModel(
            MLDataSet trainingData,
            String methodName,
            String methodArchitecture,
            String trainerName,
            String trainerArgs) {

        // first, create the machine learning method (the model)
        MLMethodFactory methodFactory = new MLMethodFactory();
        MLMethod method = methodFactory.create(methodName, methodArchitecture, trainingData.getInputSize(), trainingData.getIdealSize());

        // second, create the trainer
        MLTrainFactory trainFactory = new MLTrainFactory();
        MLTrain train = trainFactory.create(method,trainingData,trainerName,trainerArgs);
        // reset if improve is less than 1% over 5 cycles
        if( method instanceof MLResettable && !(train instanceof ManhattanPropagation) ) {
            train.addStrategy(new RequiredImprovementStrategy(500));
        }

        // third train the model
        EncogUtility.trainToError(train, 0.5);

        return (MLRegression)train.getMethod();
    }

    public static TemporalMLDataSet predict(int[] input, MLRegression model) {
        // You can also use the TemporalMLDataSet for prediction.  We will not use "generate"
        // as we do not want to generate an entire training set.  Rather we pass it each sun spot
        // ssn and dev and it will produce the input to the model, once there is enough data.
        TemporalMLDataSet trainingData = initDataSet();
        for (int i = 0; i < input.length; i++) {

            // do we have enough data for a prediction yet?
            if( trainingData.getPoints().size()>=trainingData.getInputWindowSize() ) {
                // Make sure to use index 1, because the temporal data set is always one ahead
                // of the time slice its encoding.  So for RAW data we are really encoding 0.
                MLData modelInput = trainingData.generateInputNeuralData(1);
                MLData modelOutput = model.compute(modelInput);
                double ssn = modelOutput.getData(0);
                System.out.println(i + ":Predicted=" + ssn + ",Actual=" + input[i] );

                // Remove the earliest training element.  Unlike when we produced training data,
                // we do not want to build up a large data set.  We just add enough data points to produce
                // input to the model.
                trainingData.getPoints().remove(0);
            }

            // we need a sequence number to sort the data. Here we just use
            // year * 100 + month, which produces output like "201301" for
            // January, 2013.
//            int sequenceNumber = (year * 100) + month;
//
//            TemporalPoint point = new TemporalPoint(trainingData.getDescriptions().size());
//            point.setSequence(sequenceNumber);
//            point.setData(0, normSSN.normalize(sunSpotNum) );
//            point.setData(1, normDEV.normalize(dev) );
//            trainingData.getPoints().add(point);
        }

        // generate the time-boxed data
        trainingData.generate();
        return trainingData;
    }
}
