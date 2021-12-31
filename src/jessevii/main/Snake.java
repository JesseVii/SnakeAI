package jessevii.main;

import jessevii.main.SnakeNode.Position;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Snake extends JPanel {
	public static Snake instance;
	public static Direction direction = Direction.RIGHT;
	public static SnakeNode snakeHead;
	public static Position apple;
	public static int highScore, games, movesWithNoAppleEaten;

	/**
	 * Set if it will train it or use the pretrained model.
	 * Note: if this is enabled it will delete the previously saved model and overwrite it
	 * The model is saved when the program is closed
	 */
	public static boolean train = false;

	public static AI ai = new AI();
	
	public Snake() {
		instance = this;
		this.setLayout(null);
		this.setSize(Main.instance.getWidth(), Main.instance.getHeight());
		this.setVisible(true);
		this.setBackground(Color.BLACK);
		
		snakeHead = new SnakeNode();
		snakeHead.position = new Position(40, 40);
		generateApple();
		
		new Thread(() -> {
			while(true) {
				doAction();

				instance.repaint();
				if (!train) {
					Main.sleep(15);
				}
			}
		}).start();
	}
	
	public void doAction() {
		// Predict direction using AI
		INDArray inputs = ai.getInputs();
		int action = ai.getAction(inputs);
		direction = Direction.getDirectionForAction(action);
		ai.train(inputs, action);

		//Set last position and move the snake to the predicted direction
		snakeHead.lastPosition = new Position(snakeHead.position.x, snakeHead.position.y);
		if (direction == Direction.UP) {
			snakeHead.position.y -= snakeHead.size;
		} else if (direction == Direction.DOWN) {
			snakeHead.position.y += snakeHead.size;
		} else if (direction == Direction.RIGHT) {
			snakeHead.position.x += snakeHead.size;
		} else if (direction == Direction.LEFT) {
			snakeHead.position.x -= snakeHead.size;
		}

		//Eats apple if the head is inside of the apple
		if (isGoingToEatAppleAtLocation(snakeHead.position)) {
			generateApple();
			new SnakeNode(SnakeNode.list.get(SnakeNode.list.size() - 1));
			movesWithNoAppleEaten = 0;
		}

		List<SnakeNode> list = new ArrayList<>(SnakeNode.list);
		for (SnakeNode node : list) {
			if (node.parent != null) {
				node.lastPosition = new Position(node.position.x, node.position.y);
				node.position = node.parent.lastPosition;
			}

			if (!node.equals(snakeHead) && isGoingToDieAtLocation(node.position) || node.position.x < 0 || node.position.x > Main.instance.getWidth() || node.position.y < 0 || node.position.y > Main.instance.getHeight() || movesWithNoAppleEaten > 300) {
				games++;
				int score = SnakeNode.list.size();
				if (score > highScore) {
					highScore = score;
					if (train) {
						System.out.println("New highscore of " + score + " in game " + games);
					}
				}

				if (!train) {
					System.out.println("------------------------");
					System.out.println("Died with score of " + score);
					System.out.println("Last action: " + Direction.getDirectionForAction(action));
					System.out.println("Score for last action: " + ai.lastScore);
					System.out.println("Last inputs: " + ai.lastInputs);
				}

				SnakeNode.list.clear();
				snakeHead = new SnakeNode();
				snakeHead.position = new Position(40, 40);
				movesWithNoAppleEaten = 0;
				generateApple();
			}
		}

		movesWithNoAppleEaten++;
	}
	
	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D)graphics;

		List<SnakeNode> list = new ArrayList<>(SnakeNode.list);
		for (SnakeNode node : list) {
			g.setColor(Color.GREEN);
			g.fillRect(node.position.x, node.position.y, node.size, node.size);
		}
		
		g.setColor(Color.RED);
		g.fillRect(apple.x, apple.y, snakeHead.size, snakeHead.size);
	}
	
	/**
	 * Checks if the head is in the given position if it will die
	 */
	public static boolean isGoingToDieAtLocation(Position position) {
		return snakeHead.position.x == position.x && snakeHead.position.y == position.y
		|| position.x < 0 || position.x > Main.instance.getWidth() || position.y < 0 || position.y > Main.instance.getHeight() || movesWithNoAppleEaten > 300;
	}
	
	/**
	 * Checks if its going to eat the apple if the head is in the given position
	 */
	public static boolean isGoingToEatAppleAtLocation(Position position) {
		return apple.x == position.x && apple.y == position.y;
	}

	/**
	 * Generates a new apple randomly to the map
	 */
	public void generateApple() {
		int xAmount = Main.instance.getWidth() / (snakeHead.size + 1);
		int yAmount = Main.instance.getHeight() / (snakeHead.size + 1);
		
		apple = new Position(Main.random(0, xAmount) * snakeHead.size, Main.random(0, yAmount) * snakeHead.size);
		
		for (SnakeNode node : SnakeNode.list) {
			if (node.position.equals(apple)) {
				generateApple();
				return;
			}
		}
	}
	
	public enum Direction {
		UP(),
		DOWN(),
		RIGHT(),
		LEFT();
		
		public static Direction getDirectionForAction(int action) {
			return Direction.values()[action];
		}
	}
}
