package jessevii.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

public class Main extends JFrame {
	public static Main instance;
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new Main());
	}
	
	public Main() {
		instance = this;
		this.setLayout(null);
		this.setTitle("Snake");
		this.setSize(600, 600);
		this.setBackground(Color.BLACK);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
		        if (Snake.train) {
					//Save the model if training is enabled and the program is closed
		        	Snake.ai.save();
		        	System.out.println("Saved model");
		        }
		    }
		});

		this.setVisible(true);
		this.add(new Snake());
	}

	/**
	 * Sleeps the given milliseconds on this thread
	 */
	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
	}

	/**
	 * Generates a random number between minimum and maximum
	 */
	public static int random(int min, int max) {
		return new Random().nextInt(max - min) + min;
	}
}
