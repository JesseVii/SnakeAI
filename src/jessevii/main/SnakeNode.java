package jessevii.main;

import java.util.ArrayList;
import java.util.List;

public class SnakeNode {
	public Position position, lastPosition;
	public int size = 20;
	public SnakeNode parent;
	public static List<SnakeNode> list = new ArrayList<>();
		
	public SnakeNode() {
		list.add(this);
	}
	
	public SnakeNode(SnakeNode parent) {
		this.parent = parent;
		this.position = parent.lastPosition;
		list.add(this);
	}
	
	public static class Position {
		public int x, y;
		
		public Position(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public String toString() {
			return "X: " + x + " Y: " + y;
		}
		
		@Override
		public boolean equals(Object other) {
			Position otherPosition = (Position)other;
			return otherPosition.x == x && otherPosition.y == y;
		}
	}
}
