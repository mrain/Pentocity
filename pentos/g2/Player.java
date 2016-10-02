package pentos.g2;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();

	private static int INF = (int)1e9;
	private static double EXTRA_ROAD_LENGTH = 30;
	private static double decay = 0.95;
	private static int EXTRA_ROAD_NUM = 100;
	private static int TOTAL_EXTRA_ROAD_SIZE = 90;
	private static int gap = 30;
	private static int PERIOD = 12;

	private static int req_counter = 0;

	public void init() { // function is called once at the beginning before play is called

	}

	public Move play(Building request, Land land) {
		++req_counter;
		System.out.println(req_counter);

		if (req_counter % PERIOD == 0) {
			EXTRA_ROAD_LENGTH *= decay;
			TOTAL_EXTRA_ROAD_SIZE *= decay;
			gap *= decay;
		}

		if (request.type == Building.Type.FACTORY) {
			ArrayList<Move> candidates = new ArrayList<Move>();
			for (int dx = 0; dx < land.side; ++dx)
				for (int dy = 0; dy < land.side; ++dy) {
					Cell pos = new Cell(dx, dy);
					Building[] rotations = request.rotations();
					for (int ri = 0; ri < rotations.length; ++ri) {
						Building b = rotations[ri];
						if (land.buildable(b, pos)) {
							candidates.add(new Move(true, request, pos, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
						}
					}
				}
			if (candidates.isEmpty()) return new Move(false);

			int[] level = new int[candidates.size()];

			int minRoadLen = INF;
			for (int i = 0; i < candidates.size(); ++i) {
				Move move = candidates.get(i);
				Set<Cell> shiftedCells = new HashSet<Cell>();
				for (Cell x : move.request.rotations()[move.rotation]) {
					shiftedCells.add(new Cell(x.i + move.location.i, x.j + move.location.j));
				}

				Set<Cell> roadCells = findShortestRoad(shiftedCells, land, minRoadLen);
				if (roadCells == null) {
					move.accept = false;
				} else {
					minRoadLen = Math.min(minRoadLen, roadCells.size());

					move.road = roadCells;

					level[i] = 0;
					if (atEdge(shiftedCells, land)) {
						level[i] += 2;
					}
					if (fitInCorner(shiftedCells, roadCells, land)) {
						level[i] += 1;
					}
				}
			}

			int minRoad = INF;
			int maxLevel = 0;
			int ind = -1;
			for (int i = 0; i < candidates.size(); ++i) {
				Move move = candidates.get(i);
				if (!move.accept) continue;
				if (level[i] > maxLevel || (level[i] == maxLevel && move.road.size() < minRoad)) {
					maxLevel = level[i];
					minRoad = move.road.size();
					ind = i;
				}
			}
			if (ind == -1) System.out.println("reject a building");
			if (ind == -1) return new Move(false);

			road_cells.addAll(candidates.get(ind).road);

			return candidates.get(ind);
		} else {
			ArrayList<Move> candidates = new ArrayList<Move>();
			for (int dx = land.side - 1; dx >= 0; --dx)
				for (int dy = land.side - 1; dy >= 0; --dy) {
					Cell pos = new Cell(dx, dy);
					Building[] rotations = request.rotations();
					for (int ri = 0; ri < rotations.length; ++ri) {
						Building b = rotations[ri];
						if (land.buildable(b, pos)) {
							candidates.add(new Move(true, request, pos, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
						}
					}
				}
			if (candidates.isEmpty()) return new Move(false);

			int[] score = new int[candidates.size()];
			int[] exposure = new int[candidates.size()];

			int minRoadLen = INF;

			for (int i = 0; i < candidates.size(); ++i) {
				Move move = candidates.get(i);
				Set<Cell> shiftedCells = new HashSet<Cell>();
				for (Cell x : move.request.rotations()[move.rotation]) {
					shiftedCells.add(new Cell(x.i + move.location.i, x.j + move.location.j));
				}

				Set<Cell> roadCells = findShortestRoad(shiftedCells, land, minRoadLen);
				if (roadCells == null) {
					move.accept = false;
				} else {
					minRoadLen = Math.min(minRoadLen, roadCells.size());

					move.road = roadCells;

					Set<Cell> markedForConstruction = new HashSet<Cell>();
					markedForConstruction.addAll(roadCells);

					int coin = gen.nextInt(2);
					if (coin == 0) move.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
					markedForConstruction.addAll(move.water);

					coin = gen.nextInt(2);
					if (coin == 0) move.park = randomWalk(shiftedCells, markedForConstruction, land, 4);

					score[i] = 0;
					for (Cell cell : shiftedCells) {
						for (Cell p : cell.neighbors()) {
							if (move.water.contains(p) || land.isPond(p)) {
								++score[i];
								break;
							}
						}
						for (Cell p : cell.neighbors()) {
							if (move.park.contains(p) || land.isField(p)) {
								++score[i];
								break;
							}
						}
					}

					exposure[i] = calExposure(shiftedCells, roadCells, land);
				}
			}

			int min_exposure = INF;
			int min_road = INF;
			int max_score = 0;
			int ind = -1;
			for (int i = 0; i < candidates.size(); ++i) {
				Move move = candidates.get(i);
				if (!move.accept) continue;
				if (betterResidence(exposure[i], move.road.size(), score[i], min_exposure, min_road, max_score)) {
					min_exposure = exposure[i];
					min_road = move.road.size();
					max_score = score[i];
					ind = i;
				}
			}
			if (ind == -1) System.out.println("reject a residence");
			if (ind == -1) return new Move(false);

			road_cells.addAll(candidates.get(ind).road);

			return candidates.get(ind);
		}
	}

	private boolean betterResidence(int exposure1, int roadsize1, int score1, int exposure2, int roadsize2, int score2) {
		if (exposure1 < exposure2) return true;
		if (exposure1 > exposure2) return false;
		if (score1 > score2) return true;
		if (score1 < score2) return false;
		return (roadsize1 < roadsize2);
	}

	private int calExposure(Set<Cell> b, Set<Cell> road, Land land) {
		int cnt = 0;

		boolean[][] check = new boolean[land.side][land.side];
		for (int i = 0; i < land.side; ++i)
			for (int j = 0; j < land.side; ++j)
				check[i][j] = false;

		for (Cell cell : b) {
			cnt += onNumEdge(cell, land);
			for (Cell p : cell.neighbors()) {
				if ((!check[p.i][p.j]) && (!b.contains(p)) && (!road.contains(p)) && land.unoccupied(p)) {
					check[p.i][p.j] = true;
					++cnt;
				}
			}
		}
		for (Cell cell : road) {
			for (Cell p : cell.neighbors()) {
				if ((!check[p.i][p.j]) && (!b.contains(p)) && (!road.contains(p)) && land.unoccupied(p)) {
					check[p.i][p.j] = true;
					++cnt;
				}
			}
		}
		return cnt;
	}

	private int onNumEdge(Cell cell, Land land) {
		int cnt = 0;
		if (cell.i == 0 || cell.i == land.side - 1) ++cnt;
		if (cell.j == 0 || cell.j == land.side - 1) ++cnt;
		if (cell.i == cell.j) ++cnt;
		if (cell.i - cell.j == land.side - 1 || cell.j - cell.i == land.side - 1) ++cnt;
		return cnt;
	}

	private boolean outside(int x, int y, int side) {
		if (x < 0 || x >= side || y < 0 || y >= side) return true;
		return false;
	}

	private boolean atEdge(Set<Cell> b, Land land) {
		for (Cell cell : b) {
			if (cell.i == 0 || cell.i == land.side - 1 || cell.j == 0 || cell.j == land.side - 1)
				return true;
		}
		return false;
	}

	private boolean fitInCorner(Set<Cell> b, Set<Cell> road, Land land) {
		for (Cell cell : b) {
			boolean flag = true;
			for (Cell p : cell.neighbors()) {
				if ((!b.contains(p)) && (!road.contains(p)) && land.unoccupied(p)) {
					flag = false;
					break;
				}
			}
			if (flag) return true;
		}
		return false;
	}

	private boolean nearBuilding(Cell p, Land land) {
		for (Cell nei : p.neighbors()) {
			if (nei.isType(Cell.Type.RESIDENCE) || nei.isType(Cell.Type.FACTORY))
				return true;
		}
		return false;
	}

	private int nearSpace(Cell p, Set<Cell> b, boolean[][] checked, Land land) {
		int cnt = 0;
		for (Cell nei : p.neighbors()) {
			if (land.unoccupied(p) && !b.contains(p) && !checked[p.i][p.j])
				++cnt;
		}
		return cnt;
	}

	private int nearRoad(Cell p, boolean[][] checked) {
		int cnt = 0;
		for (Cell nei : p.neighbors()) {
			if (checked[nei.i][nei.j] || road_cells.contains(nei))
				++cnt;
		}
		return cnt;
	}

	private Set<Cell> drawExtraRoad(Queue<Cell> start, boolean[][] checked, Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();

		Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); 
		output.addAll(start);
		Queue<Cell> queue = new LinkedList<Cell>();
		int cnt = INF;
		for (Cell cell : output) {
			if (cnt <= gap) {
				++cnt;
				continue;
			}
			int tmp = gen.nextInt(start.size());
			if (tmp < EXTRA_ROAD_NUM) {
				queue.add(new Cell(cell.i, cell.j, source));
				cnt = 0;
			} else ++cnt;
		}
		output.clear();

		Queue<Integer> step = new LinkedList<Integer>();
		for (int i = 0; i < queue.size(); ++i) step.add(1);

		while (!queue.isEmpty()) {
			Cell cur = queue.remove();
			int cur_step = step.remove();
			if (cur_step == (int)EXTRA_ROAD_LENGTH) {
				Cell tail = cur;
				while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
					if (!output.contains(tail)) output.add(new Cell(tail.i, tail.j));
					tail = tail.previous;
				}
				if (output.size() > TOTAL_EXTRA_ROAD_SIZE) return output;
				continue;
			}

			Cell chosen = null;
			for (Cell p : cur.neighbors()) {
				if (b.contains(p) || !land.unoccupied(p) || nearRoad(p, checked) >= 2) continue;
				if (chosen == null) chosen = p;
				if (nearSpace(p, b, checked, land) > 1) {
					chosen = p;
				}
				if (/*nearBuilding(p, land) && */nearSpace(p, b, checked, land) > 2) {
					chosen = p;
					break;
				}
			}
			if (chosen == null) continue;
			checked[chosen.i][chosen.j] = true;
			queue.add(new Cell(chosen.i, chosen.j, cur));
			step.add(cur_step + 1);
		}
		return output;
	}

	// build shortest sequence of road cells to connect to a set of cells b
	private Set<Cell> findShortestRoad(Set<Cell> b, Land land, int minRoadLen) {
		boolean no_need = false;
		Set<Cell> output = new HashSet<Cell>();

		boolean[][] checked = new boolean[land.side][land.side];
		for (int i = 0; i < land.side; ++i)
			for (int j = 0; j < land.side; ++j)
				checked[i][j] = false;

		Queue<Cell> queue = new LinkedList<Cell>();
		Queue<Integer> step = new LinkedList<Integer>();

		// add border cells that don't have a road currently
		Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
		for (int z=0; z<land.side; z++) {
			if (b.contains(new Cell(0,z)) || b.contains(new Cell(z,0)) || b.contains(new Cell(land.side-1,z)) || b.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
				no_need = true;
			if (!checked[0][z] && land.unoccupied(0,z) && !b.contains(new Cell(0, z))) {
				checked[0][z] = true;
				queue.add(new Cell(0,z,source));
				step.add(1);
			}
			if (!checked[z][0] && land.unoccupied(z,0) && !b.contains(new Cell(z, 0))) {
				checked[z][0] = true;
				queue.add(new Cell(z,0,source));
				step.add(1);
			}
			if (!checked[z][land.side - 1] && land.unoccupied(z,land.side-1) && !b.contains(new Cell(z, land.side - 1))) {
				checked[z][land.side - 1] = true;
				queue.add(new Cell(z,land.side-1,source));
				step.add(1);
			}
			if (!checked[land.side - 1][z] && land.unoccupied(land.side-1,z) && !b.contains(new Cell(land.side - 1, z))) {
				checked[land.side - 1][z] = true;
				queue.add(new Cell(land.side-1,z,source));
				step.add(1);
			}
		}
		// add cells adjacent to current road cells
		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (b.contains(q)) {
					no_need = true;
				}
				if (!checked[q.i][q.j] && land.unoccupied(q) && !b.contains(q)) {
					checked[q.i][q.j] = true;
					queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
					step.add(1);
				}
			}
		}	

		if (req_counter % PERIOD == 0) {
			Set<Cell> extra_road = drawExtraRoad(queue, checked, b, land);
			output.addAll(extra_road);

			for (Cell p : extra_road) checked[p.i][p.j] = true;
			for (Cell p : extra_road) {
				for (Cell q : p.neighbors()) {
					if (b.contains(q)) {
						return output;
					}
					if (!checked[q.i][q.j] && land.unoccupied(q) && !b.contains(q)) {
						checked[q.i][q.j] = true;
						queue.add(new Cell(q.i, q.j, source));
						step.add(1);
					}
				}
			}
		}
		if (no_need) return output;
 
		//bfs
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			int len = step.remove();
			if (len > Math.max(minRoadLen * 2, 5)) return null;

			for (Cell x : p.neighbors()) {		
				if (b.contains(x)) { // trace back through search tree to find path
					Cell tail = p;
					while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
						output.add(new Cell(tail.i,tail.j));
						tail = tail.previous;
					}
					//if (!output.isEmpty())
						return output;
				}
				else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
					checked[x.i][x.j] = true;
					x.previous = p;
					queue.add(new Cell(x.i, x.j, p));	      
					step.add(len + 1);
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
