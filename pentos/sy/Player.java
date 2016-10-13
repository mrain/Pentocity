package pentos.g1;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

	private Random gen = new Random();
	private Set<Cell> all_road_cells = new HashSet<Cell>();

	private static int INF = (int)1e9;

	private class Evaluation {
		public int peri_delta;
		public int num_seg;
		public int road_len;
		public int score;
		public int vertical_dis;

		Evaluation() {}

		boolean isBetter(Evaluation other) {
			if (peri_delta > other.peri_delta) return true;
			if (peri_delta < other.peri_delta) return false;

			if (num_seg > other.num_seg) return true;
			if (num_seg < other.num_seg) return false;

			if (score > other.score) return true;
			if (score < other.score) return false;

			if (vertical_dis > other.vertical_dis) return true;
			return false;
		}
	}

	public void init() { // function is called once at the beginning before play is called

	}

	public Move play(Building request, Land land) {
		ArrayList<Move> candidates = new ArrayList<Move>();
		ArrayList<Evaluation> scores = new ArrayList<Evaluation>();

		if (request.type == Building.Type.FACTORY) {
			// get candidates position
			for (int dx = 0; dx < land.side; ++dx)
				for (int dy = 0; dy < land.side; ++dy) {
					Cell pos = new Cell(dx, dy);
					Building[] rotations = request.rotations();
					for (int ri = 0; ri < rotations.length; ++ri) {
						Building b = rotations[ri];
						if (land.buildable(b, pos)) {
							candidates.add(new Move(true, request, pos, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
							scores.add(new Evaluation());
						}
					}
				}
			if (candidates.isEmpty()) {
				System.out.println("reject a FACTORY");
				return new Move(false);
			}

			// get candidates evaluation
			int min_vertical_dis = land.side;
			for (int i = 0; i < candidates.size(); ++i) {
				Move current = candidates.get(i);
				Set<Cell> bcells = new HashSet<Cell>();
				for (Cell x : current.request.rotations()[current.rotation]) {
					bcells.add(new Cell(x.i + current.location.i, x.j + current.location.j));
				}

				scores.get(i).vertical_dis = calVerticalDis(0, bcells, land);
				if (scores.get(i).vertical_dis > min_vertical_dis + 10) {
					current.accept = false;
					continue;
				}

				Set<Cell> shifted_cells = new HashSet<Cell>();
				shifted_cells.addAll(bcells);

				current.road = findRoad(bcells, land);
				if (current.road == null) {
					System.out.println("no road");
					current.accept = false;
					continue;
				}
				scores.get(i).road_len = current.road.size();
				
				min_vertical_dis = Math.min(min_vertical_dis, scores.get(i).vertical_dis);

				calPerimeter(scores.get(i), bcells, land); // calculate peri_delta & num_seg
				scores.get(i).score = request.size();
			}

			// pick one with highest evaluation
			int chosen = -1;
			for (int i = 1; i < candidates.size(); ++i) {
				if (candidates.get(i).accept && (chosen == -1 || scores.get(i).isBetter(scores.get(chosen)))) {
					chosen = i;
				}
			}
			if (chosen == -1) {
				System.out.println("reject a FACTORY");
				return new Move(false);
			}
			all_road_cells.addAll(candidates.get(chosen).road);
			return candidates.get(chosen);
		} else {
			// get candidates position
			for (int dx = land.side - 1; dx >= 0; --dx)
				for (int dy = 0; dy < land.side; ++dy) {
					Cell pos = new Cell(dx, dy);
					Building[] rotations = request.rotations();
					for (int ri = 0; ri < rotations.length; ++ri) {
						Building b = rotations[ri];
						if (land.buildable(b, pos)) {
							candidates.add(new Move(true, request, pos, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
							scores.add(new Evaluation());
						}
					}
				}
			if (candidates.isEmpty()) {
				System.out.println("reject a RESIDENCE");
				return new Move(false);
			}

			// get candidates evaluation
			int min_vertical_dis = land.side;
			for (int i = 0; i < candidates.size(); ++i) {
				Move current = candidates.get(i);
				Set<Cell> bcells = new HashSet<Cell>();
				for (Cell x : current.request.rotations()[current.rotation]) {
					bcells.add(new Cell(x.i + current.location.i, x.j + current.location.j));
				}

				scores.get(i).vertical_dis = calVerticalDis(land.side, bcells, land);
				if (scores.get(i).vertical_dis > min_vertical_dis + 10) {
					current.accept = false;
					continue;
				}

				Set<Cell> shifted_cells = new HashSet<Cell>();
				shifted_cells.addAll(bcells);

				current.road = findRoad(bcells, land);
				if (current.road == null) {
					System.out.println("no road");
					current.accept = false;
					continue;
				}
				scores.get(i).road_len = current.road.size();
				
				min_vertical_dis = Math.min(min_vertical_dis, scores.get(i).vertical_dis);

				calPerimeter(scores.get(i), bcells, land); // calculate peri_delta & num_seg

				shifted_cells.addAll(current.road);
				current.water = buildWaterOrPark(Cell.Type.WATER, bcells, shifted_cells, land);

				shifted_cells.addAll(current.water);
				current.park = buildWaterOrPark(Cell.Type.PARK, bcells, shifted_cells, land);

				scores.get(i).score = calScore(bcells, current, land);
			}

			// pick one with highest evaluation
			int chosen = -1;
			for (int i = 1; i < candidates.size(); ++i) {
				if (candidates.get(i).accept && (chosen == -1 || scores.get(i).isBetter(scores.get(chosen)))) {
					chosen = i;
				}
			}
			if (chosen == -1) {
				System.out.println("reject a RESIDENCE!!");
				return new Move(false);
			}
			all_road_cells.addAll(candidates.get(chosen).road);
			return candidates.get(chosen);
		}
	}

	private int calVerticalDis(int side, Set<Cell> bcells, Land land) {
		int min_dis = land.side;
		for (Cell cell : bcells) {
			min_dis = Math.min(min_dis, Math.abs(cell.i - side));
		}
		return min_dis;
	}

	private boolean atEdge(Cell cell, Land land) {
		if (cell.i == 0 || cell.i == land.side - 1 || cell.j == 0 || cell.j == land.side - 1) return true;
		return false;
	}

	private void calPerimeter(Evaluation score, Set<Cell> bcells, Land land) {
		score.peri_delta = 0;
		score.num_seg = 0;

		Queue<Cell> q = new LinkedList<Cell>();
		Set<Cell> compact_cells = new HashSet<Cell>();
		for (Cell b : bcells) {
			boolean flag = false;
			for (Cell nei : b.neighbors()) {
				if (!land.unoccupied(nei.i, nei.j) || (atEdge(b, land))) {
					compact_cells.add(b);
					if (q.isEmpty()) q.add(b);
					score.peri_delta++;
					continue;
				} else
				if (land.unoccupied(nei.i, nei.j) && !bcells.contains(nei)) {
					flag = true;
				}
			}
			if (flag) score.peri_delta--;
		}
		
		if (compact_cells.size() == 0) return;
		boolean[][] check = new boolean[land.side][land.side];
		for (int i = 0; i < land.side; ++i)
			for (int j = 0; j < land.side; ++j)
				check[i][j] = false;
		check[q.peek().i][q.peek().j] = true;

		int cnt = 1;
		while (!q.isEmpty()) {
			Cell cur = q.remove();
			for (Cell nei : cur.neighbors()) {
				if (compact_cells.contains(nei) && !check[nei.i][nei.j]) {
					check[nei.i][nei.j] = true;
					q.add(nei);
					++cnt;
				}
			}
		}
		score.num_seg = ((cnt == compact_cells.size()) ? (1) : (2));
	}

	private boolean neiborTo(Cell.Type type, Set<Cell> bcells, Land land) {
		for (Cell b : bcells) {
			for (Cell nei : b.neighbors()) {
				if (type == Cell.Type.WATER && land.isPond(nei)) return true;
				if (type == Cell.Type.PARK && land.isField(nei)) return true;
			}
		}
		return false;
	}

	private int calSimplePerimeter(Set<Cell> bcells, Set<Cell> field, Land land) {
		int cnt = 0;
		for (Cell b : bcells) {
			for (Cell nei : b.neighbors())
				if (!bcells.contains(nei) && !field.contains(nei) && !land.unoccupied(nei)) {
					++cnt;
					break;
				}
		}
		for (Cell f : field) {
			for (Cell nei : f.neighbors())
				if (!bcells.contains(nei) && !field.contains(nei) && !land.unoccupied(nei)) {
					++cnt;
					break;
				}
		}
		return cnt;
	}

	private Set<Cell> buildWaterOrPark(Cell.Type type, Set<Cell> bcells, Set<Cell> marked, Land land) {
		if (!neiborTo(type, bcells, land)) {
			Set<Cell> ans = new HashSet<Cell>();
			int max_peri = -1;
			for (int i = 0; i < 10; ++i) {
				Set<Cell> field = randomWalk(bcells, marked, land, 4);
				int peri = calSimplePerimeter(bcells, field, land);
				if (peri > max_peri) {
					max_peri = peri;
					ans.clear();
					ans.addAll(field);
				}
			}
			return ans;
		}
		return new HashSet<Cell>();
	}

	private int calScore(Set<Cell> bcells, Move move, Land land) {
		int scr = 0;
		for (Cell b : bcells) {
			++scr;
			for (Cell nei : b.neighbors()) {
				if (land.isPond(nei) || move.water.contains(nei)) {
					++scr;
					break;
				}
			}
			for (Cell nei : b.neighbors()) {
				if (land.isField(nei) || move.park.contains(nei)) {
					++scr;
					break;
				}
			}
		}
		return scr;
	}

	// current strategy is find shortest one
	private Set<Cell> findRoad(Set<Cell> bcells, Land land) {
		Set<Cell> output = new HashSet<Cell>();

		boolean[][] checked = new boolean[land.side][land.side];
		for (int i = 0; i < land.side; ++i)
			for (int j = 0; j < land.side; ++j)
				checked[i][j] = false;

		Queue<Cell> queue = new LinkedList<Cell>();

		// add border cells that don't have a road currently
		Cell source = new Cell(Integer.MAX_VALUE, Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
		for (int z=0; z<land.side; z++) {
			if (bcells.contains(new Cell(0,z)) || bcells.contains(new Cell(z,0)) || bcells.contains(new Cell(land.side-1,z)) || bcells.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
				return output;
			if (!checked[0][z] && land.unoccupied(0,z) && !bcells.contains(new Cell(0, z))) {
				checked[0][z] = true;
				queue.add(new Cell(0,z,source));
			}
			if (!checked[z][0] && land.unoccupied(z,0) && !bcells.contains(new Cell(z, 0))) {
				checked[z][0] = true;
				queue.add(new Cell(z,0,source));
			}
			if (!checked[z][land.side - 1] && land.unoccupied(z,land.side-1) && !bcells.contains(new Cell(z, land.side - 1))) {
				checked[z][land.side - 1] = true;
				queue.add(new Cell(z,land.side-1,source));
			}
			if (!checked[land.side - 1][z] && land.unoccupied(land.side-1,z) && !bcells.contains(new Cell(land.side - 1, z))) {
				checked[land.side - 1][z] = true;
				queue.add(new Cell(land.side-1,z,source));
			}
		}
		// add cells adjacent to current road cells
		for (Cell p : all_road_cells) {
			for (Cell q : p.neighbors()) {
				if (bcells.contains(q)) {
					return output;
				}
				if (!checked[q.i][q.j] && land.unoccupied(q) && !bcells.contains(q)) {
					checked[q.i][q.j] = true;
					queue.add(new Cell(q.i,q.j,source)); // use tail field of cell to keep track of previous road cell during the search
				}
			}
		}	

		//bfs
		while (!queue.isEmpty()) {
			Cell p = queue.remove();

			for (Cell x : p.neighbors()) {		
				if (bcells.contains(x)) { // trace back through search tree to find path
					Cell tail = p;
					while (!all_road_cells.contains(tail) && !tail.equals(source)) {
						output.add(new Cell(tail.i,tail.j));
						tail = tail.previous;
					}
					return output;
				}
				if (!checked[x.i][x.j] && land.unoccupied(x) && !bcells.contains(x)) {
					checked[x.i][x.j] = true;
					x.previous = p;
					queue.add(new Cell(x.i, x.j, p));	      
				} 
			}
		}
		return null;

		/*
		   if (output.isEmpty() && queue.isEmpty())
		   return null;
		   else
		   return output;
		   */
	}


	// walk n consecutive cells starting from a building. Used to build a random field or pond. 
	private Set<Cell> randomWalk(Set<Cell> b, Set<Cell> marked, Land land, int n) {
		ArrayList<Cell> adjCells = new ArrayList<Cell>();
		Set<Cell> output = new HashSet<Cell>();
		for (Cell p : b) {
			for (Cell q : p.neighbors()) {
				if (land.isField(q) || land.isPond(q))
					return new HashSet<Cell>();
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q))
					adjCells.add(q); 
			}
		}
		if (adjCells.isEmpty())
			return new HashSet<Cell>();
		Cell tail = adjCells.get(gen.nextInt(adjCells.size()));
		for (int ii=0; ii<n; ii++) {
			ArrayList<Cell> walk_cells = new ArrayList<Cell>();
			for (Cell p : tail.neighbors()) {
				if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p))
					walk_cells.add(p);		
			}
			if (walk_cells.isEmpty()) {
				//return output; //if you want to build it anyway
				return new HashSet<Cell>();
			}
			output.add(tail);	    
			tail = walk_cells.get(gen.nextInt(walk_cells.size()));
		}
		return output;
	}

}
