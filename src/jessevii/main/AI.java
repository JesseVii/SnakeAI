package jessevii.main;

import jessevii.main.Snake.Direction;
import jessevii.main.SnakeNode.Position;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;

public class AI {
	public MultiLayerNetwork model;
	public double lastDistance, totalScore;
	public String fileName = "trained_snake_model.zip";
	public INDArray lastInputs;
	public double lastScore;
	
	public AI() {
		model = getModel();
	}

	/**
	 * Gets the action that the AI chooses with the given inputs
	 */
	public int getAction(INDArray inputs) {
		INDArray output = model.output(inputs, false);
        float[] outputValues = output.data().asFloat();

		return getMaxValueIndex(outputValues);
	}
	
	public void train(INDArray inputs, int action) {
        INDArray output = model.output(inputs, true);
        double score = getScore(action);
		INDArray updatedOutput = output.putScalar(action, score);
		totalScore += score;
		
		//If total score gets to a high negative value then kill the snake
		//As it happens if it gets stuck on a loop and if the score goes below integer limit it will not work anymore.
		if (totalScore < -1000000) {
			Snake.snakeHead.position.x = -999999;
		}
		
        model.fit(inputs, updatedOutput);
	}

	/**
	 * Calculates a score for the action that the AI chose
	 * We dont know if the action was 100% correct either so it will calculate the score
	 * by looking if it moved closer to the apple which is good or ate the apple
	 * Or if it died because of this action then it was a bad move
	 * With this the AI will learn the basics of the game which is to eat apples and not die
	 */
	public double getScore(int action) {
		double score = 0;
		Position futurePosition = getFuturePositionForDirection(Direction.getDirectionForAction(action));
		double distance = getDistanceToApple(futurePosition.x, futurePosition.y);
		
		//If distance is closer to apple than last distance then its good
		//There are often times when its required to go to the opposite direction so it will not punish for it that much
		if (distance < lastDistance) {
			score += 1000;
		} else {
			score -= 1000;
		}
		
		//If it will die at the future position then its bad
		if (willDieAtPosition(futurePosition)) {
			score -= 25000 - SnakeNode.list.size() * 5;
		}
		
		//If it will eat apple at the future position then its good
		if (Snake.isGoingToEatAppleAtLocation(futurePosition)) {
			score += 50000;
		}
		
		lastDistance = distance;
		lastScore = score;
		return score;
	}

	/**
	 * Calculates a distance to the apple from the given position
	 */
	public double getDistanceToApple(int x, int y) {
		return Math.abs(x - Snake.apple.x) + Math.abs(y - Snake.apple.y);
	}

	/**
	 * Gets the inputs. This is what the AI will see
	 * With these it can see the distance to the apple and if it will die by moving to any of the 4 directions
	 */
	public INDArray getInputs() {
		INDArray inputs = Nd4j.create(1, 6);
		inputs.getRow(0).getColumn(0).assign(Snake.snakeHead.position.x - Snake.apple.x);
		inputs.getRow(0).getColumn(1).assign(Snake.snakeHead.position.y - Snake.apple.y);
		inputs.getRow(0).getColumn(2).assign(willDieAtPosition(getFuturePositionForDirection(Direction.UP)));
		inputs.getRow(0).getColumn(3).assign(willDieAtPosition(getFuturePositionForDirection(Direction.DOWN)));
		inputs.getRow(0).getColumn(4).assign(willDieAtPosition(getFuturePositionForDirection(Direction.RIGHT)));
		inputs.getRow(0).getColumn(5).assign(willDieAtPosition(getFuturePositionForDirection(Direction.LEFT)));
		lastInputs = inputs;
		
		return inputs;
	}

	/**
	 * Saves the model
	 */
	public void save() {
		try {
			model.save(new File(fileName));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the snake will die at the given position
	 */
	public boolean willDieAtPosition(Position position) {
		for (SnakeNode node : SnakeNode.list) {
			if (node.position.equals(position)) {
				return true;
			}
		}
		
		if (Snake.isGoingToDieAtLocation(position)) {
			return true;
		}
		
		return false;
	}

	/**
	 * Gets the future position for the given direction
	 */
	public Position getFuturePositionForDirection(Direction direction) {
		Position futurePosition = new Position(Snake.snakeHead.position.x, Snake.snakeHead.position.y);
		if (direction == Direction.UP) {
			futurePosition.y -= Snake.snakeHead.size;
		} else if (direction == Direction.DOWN) {
			futurePosition.y += Snake.snakeHead.size;
		} else if (direction == Direction.RIGHT) {
			futurePosition.x += Snake.snakeHead.size;
		} else if (direction == Direction.LEFT) {
			futurePosition.x -= Snake.snakeHead.size;
		}
		
		return futurePosition;
	}

	/**
	 * Loads the model from the file if not training or creates a new one
	 */
    public MultiLayerNetwork getModel() {
    	if (!Snake.train) {
    		try {
	    		MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(fileName);
	            model.init();
	            model.setListeners(new ScoreIterationListener(Integer.MAX_VALUE));
	            System.out.println("Loaded saved model");
	            return model;
    		} catch (Exception e) {
    			System.out.println("No saved model found");
    		}
    	}
    	
        final int numInputs = 6;
        int outputNum = 4;
        int hiddenLayers = 150;
        
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
        .seed(12345)
        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
        .weightInit(WeightInit.XAVIER)
        .updater(new Adam(0.001))
        .l2(0.001)
        .list()
        .layer(0, new DenseLayer.Builder()
                .nIn(numInputs)
                .nOut(hiddenLayers)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.RELU)
                .build())
        .layer(1, new DenseLayer.Builder()
                .nIn(hiddenLayers)
                .nOut(hiddenLayers)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.RELU)
                .build())
        .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(hiddenLayers)
                .nOut(outputNum)
                .weightInit(WeightInit.XAVIER)
                .activation(Activation.IDENTITY)
                .weightInit(WeightInit.XAVIER)
                .build())
        .backpropType(BackpropType.Standard)
        .build();
        
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new ScoreIterationListener(Integer.MAX_VALUE));
        System.out.println("Created new model");
        return model;
    }
    
    public int getMaxValueIndex(final float[] values) {
        int maxAt = 0;

        for (int i = 0; i < values.length; i++) {
            maxAt = values[i] > values[maxAt] ? i : maxAt;
        }

        return maxAt;
    }
}
