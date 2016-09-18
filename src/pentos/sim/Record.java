package pentos.sim;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Record {
	
	public int score, road, building, resort, empty, giant, perimeter, giant_perimeter, extendable, giant_extendable;
	// utility of water/park = score - building / resort;
	
	private final int[] dir_x = {0, 1, 0, -1};
	private final int[] dir_y = {1, 0, -1, 0};
	
	public Record(Land land, int score) {
		this.score = score;
		road = resort = empty = perimeter = extendable = giant = giant_perimeter = 0;
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
				if (vis[i][j] || land.land[i][j].isEmpty()) continue;
				queue.add(new Pair(i, j));
				vis[i][j] = true;
				while (!queue.isEmpty()) {
					int x = queue.peek().x;
					int y = queue.poll().y;
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
						} else {
							vis[next_x][next_y] = true;
							queue.add(new Pair(next_x, next_y));
						}
					}
				}
			}
		
		// BFS for the giant empty area
		for (int i = 0; i < n; ++ i)
			for (int j = 0; j < n; ++ j)
				vis[i][j] = false;
		queue.clear();
		for (int i = 0; i < n; ++ i)
			for (int j = 0; j < n; ++ j) {
				if (vis[i][j] || !land.land[i][j].isEmpty()) continue;
				int tmp_count = 0, tmp_perimeter = 0, tmp_extendable = 0;
				queue.add(new Pair(i, j));
				vis[i][j] = true; 
				while (!queue.isEmpty()) {
					int x = queue.peek().x;
					int y = queue.poll().y;
					++ tmp_count;
					boolean extend_mark = false;
					for (int k = 0; k < 4; ++ k) {
						int next_x = x + dir_x[k];
						int next_y = y + dir_y[k];
						if (next_x == n || next_y == n || next_x < 0 || next_y < 0 || vis[next_x][next_y]) continue;
						if (!land.land[next_x][next_y].isEmpty()) {
							++ tmp_perimeter;
							if (!extend_mark && (land.land[next_x][next_y].isPark() || land.land[next_x][next_y].isWater())) {
								extend_mark = true;
								++ tmp_extendable;
							}
						} else {
							queue.add(new Pair(next_x, next_y));
							vis[next_x][next_y] = true;
						}
							
					}
				}
				if (tmp_count > giant) {
					giant = tmp_count;
					giant_perimeter = tmp_perimeter;
					giant_extendable = tmp_extendable;
				}
			}
	}
}
