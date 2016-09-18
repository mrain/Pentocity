package pentos.sim;

import java.util.LinkedList;
import java.util.Queue;

public class Record {
	
	public int score, road, building, resort, empty, perimeter, extendable;
	// utility of water/park = score - building / resort;
	
	private final int[] dir_x = {0, 1, 0, -1};
	private final int[] dir_y = {1, 0, -1, 0};
	
	public Record(Land land, int score) {
		this.score = score;
		road = resort = empty = perimeter = extendable = 0;
		int n = land.side;
		// Simple cell counting
		for (int i = 0; i < n; ++ i)
			for (int j = 0; j < n; ++ j) {
				if (land.land[i][j].isPark()) ++ resort;
				if (land.land[i][j].isWater()) ++ resort;
				if (land.land[i][j].isEmpty()) ++ empty;
				if (land.land[i][j].isRoad()) ++ road;
				if (land.land[i][j].isFactory() || land.land[i][j].isType(Cell.Type.RESIDENCE))
					++ building;
			}
		
		// BFS for perimeter and extendable cells
		
		class Pair {
			public int x, y;
			public Pair(int x, int y) {
				this.x = x;
				this.y = y;
			}
		}
		
		boolean[][] vis = new boolean[n][n];
		Queue<Pair> queue = new LinkedList<Pair>();
		for (int i = 0; i < n; ++ i)
			for (int j = 0; j < n; ++ j) {
				if (vis[i][j]) continue;
				queue.add(new Pair(i, j));
				while (!queue.isEmpty()) {
					int x = queue.peek().x;
					int y = queue.poll().y;
					vis[x][y] = true;
					for (int k = 0; k < 4; ++ k) {
						int next_x = x + dir_x[k];
						int next_y = y + dir_y[k];
						if (next_x == n || next_y == n || next_x < 0 || next_y < 0 || vis[next_x][next_y]) continue;
						if (land.land[next_x][next_y].isEmpty()) {
							++ perimeter;
							if (!vis[next_x][next_y] && (land.land[x][y].isPark() || land.land[x][y].isWater())) {
								++ extendable;
								vis[next_x][next_y] = true;
							}
						}
					}
				}
			}
	}
}
