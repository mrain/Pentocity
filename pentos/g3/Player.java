package pentos.g3;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

	private Random gen = new Random();
	private Set<Cell> all_road_cells = new HashSet<Cell>();

	private enum Type {FACTORY_SLOT, CUSTOM_PEREMETER};

	private static int INF = (int)1e9;
	private static int VERTICAL_GAP = 6;

	private final Type strategy = Type.FACTORY_SLOT;
	// FACTORY_SLOT's parameters
	private static int SLOT_GAP = 15;

	private class Slot {
		public int start = -1;
		public int len = -1;
		public int cursor = 0;
		public int road_pos = -1;
		public int bottom = -1;

		public Slot(int _start, int _len, int _road_pos) {
			start = _start;
			len = _len;
			cursor = 0;
			road_pos = _road_pos;
			bottom = start + len;
			if (road_pos == bottom) ++bottom;
		}

		public SlotChoice fit(Building request, Land land) {
			SlotChoice res = new SlotChoice();
			res.height = -1;

			int[] states = getPosition(request, cursor, start, len);
			if (states[0] == -1) return res;

			Building b = request.rotations()[states[2]];
			int[] scale = getScale(b);
			Cell pos = new Cell(states[0], states[1]);

			if (land.buildable(b, pos) && forRoad(cursor, cursor + scale[0] - 1, road_pos, land)) {
				res.height = cursor + scale[0];
				res.pos = pos;
				res.rotation = states[2];
			}

			return res;
		}

		public HashSet<Cell> build(SlotChoice choice, Land land) {
			HashSet<Cell> road_cells = new HashSet<Cell>();
			if (road_pos < 0 || road_pos >= land.side) {
				cursor = choice.height;
				return road_cells;
			}
			
			for (int i = cursor; i < choice.height; ++i) {
				if (land.unoccupied(i, road_pos)) road_cells.add(new Cell(i, road_pos));
			}
			cursor = choice.height;

			return road_cells;
		}

	}

	private ArrayList<Slot> slot = new ArrayList<Slot>();

	private boolean forRoad(int x1, int x2, int y, Land land) {
		if (y == -1) return true;
		for (int i = x1; i <= x2; ++i)
			if (!all_road_cells.contains(new Cell(i, y)) && !land.unoccupied(i, y)) return false;
		return true;
	}

	private int[] getScale(Building bcells) {
		int x1 = INF, x2 = 0, y1 = INF, y2 = 0;
		for (Cell c : bcells) {
			x1 = Math.min(x1, c.i);
			x2 = Math.max(x2, c.i);
			y1 = Math.min(y1, c.j);
			y2 = Math.max(y2, c.j);
		}
		
		int[] res = new int[2];
		res[0] = x2 - x1 + 1; res[1] = y2 - y1 + 1;
		return res;
	}

	private int[] getPosition(Building bcells, int x, int y, int dy) {
		int[] res = new int[3];
		res[0] = res[1] = res[2] = -1;

		Building[] rotation = bcells.rotations();
		for (int i = 0; i < rotation.length; ++i) {
			int[] scale = getScale(rotation[i]);
			if (scale[1] == dy) {
				res[0] = res[1] = INF;
				for (Cell c : bcells) {
					res[0] = Math.min(res[0], c.i);
					res[1] = Math.min(res[1], c.j);
				}
				res[0] = x - res[0]; res[1] = y - res[1];
				res[2] = i;
				return res;
			}
		}
		return res;
	}

	private boolean newSlot(Building request, Land land) {
		int[] scale = getScale(request);
		if (scale[0] < scale[1]) {
			int c = scale[0];
			scale[0] = scale[1]; scale[1] = c;
		}
		int numSlot = slot.size();

		int start = 0;
		if (numSlot > 0) start = slot.get(numSlot - 1).bottom;
		int[] pos = getPosition(request, 0, start, scale[1]); // position.x, position.y, rotation
		if (pos[0] != -1 && land.buildable(request.rotations()[pos[2]], new Cell(pos[0], pos[1]))) {
			slot.add(new Slot(start, scale[1], (((numSlot % 2) == 1)?(start + scale[1]):(start - 1))));
			return true;
		} else {
			pos = getPosition(request, 0, start, scale[0]);
			if (pos[0] == -1 || !land.buildable(request.rotations()[pos[2]], new Cell(pos[0], pos[1]))) {
				return false;
			} else {
				slot.add(new Slot(start, scale[0], (((numSlot % 2) == 1)?(start + scale[0]):(start-1))));
				return true;
			}
		}
	}


	private class SlotChoice {
		int slot_id;
		int height;
		Cell pos;
		int rotation;

		SlotChoice() {}
	}


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
		if (request.type == Building.Type.FACTORY) {
			if (strategy == Type.FACTORY_SLOT) return playFactoryBySlot(request, land);
			else if (strategy == Type.CUSTOM_PEREMETER) return playFactory(request, land);
			else return playFactory(request, land);
		} else {
			return playResidence(request, land);
		}
	}

	private Move playFactoryBySlot(Building request, Land land) {
		ArrayList<Move> candidates = new ArrayList<Move>();
		ArrayList<SlotChoice> choices = new ArrayList<SlotChoice>();

		int min_height = INF;
		int chosen = 0;
		for (int i = 0; i < slot.size(); ++i) {
			SlotChoice res = slot.get(i).fit(request, land);
			res.slot_id = i;
			if (res.height != -1) {
				candidates.add(new Move(true, request, res.pos, res.rotation, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
				choices.add(res);

				if (res.height < min_height) {
					min_height = res.height;
					chosen = candidates.size() - 1;
				}
			}
		}
		if (min_height > SLOT_GAP && newSlot(request, land)) {
			SlotChoice res = slot.get(slot.size() - 1).fit(request, land);
			HashSet<Cell> road_cells = slot.get(slot.size() - 1).build(res, land);
			all_road_cells.addAll(road_cells);
			return new Move(true, request, res.pos, res.rotation, road_cells, new HashSet<Cell>(), new HashSet<Cell>());
		}

		if (candidates.size() == 0) {
			if (newSlot(request, land) == false) return playFactory(request, land);
			SlotChoice res = slot.get(slot.size() - 1).fit(request, land);
			HashSet<Cell> road_cells = slot.get(slot.size() - 1).build(res, land);
			all_road_cells.addAll(road_cells);
			return new Move(true, request, res.pos, res.rotation, road_cells, new HashSet<Cell>(), new HashSet<Cell>());
		}

		candidates.get(chosen).road = slot.get(choices.get(chosen).slot_id).build(choices.get(chosen), land);
		all_road_cells.addAll(candidates.get(chosen).road);
		return candidates.get(chosen);
	}

	private Move playFactory(Building request, Land land) {
		ArrayList<Move> candidates = getFacCandidates(request, land);
		if (candidates.isEmpty()) {
			System.out.println("reject a FACTORY");
			return new Move(false);
		}

		ArrayList<Evaluation> scores = getFacEvaluation(candidates, request, land);

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

	}

	private Move playResidence(Building request, Land land) {
		ArrayList<Move> candidates = getResCandidates(request, land);
		if (candidates.isEmpty()) {
			System.out.println("reject a RESIDENCE: no valid position candidate");
			return new Move(false);
		}

		ArrayList<Evaluation> scores = getResEvaluation(candidates, request, land);
		
		// pick one with highest evaluation
		int chosen = -1;
		for (int i = 1; i < candidates.size(); ++i) {
			if (candidates.get(i).accept && (chosen == -1 || scores.get(i).isBetter(scores.get(chosen)))) {
				chosen = i;
			}
		}
		if (chosen == -1) {
			System.out.println("reject a RESIDENCE!! : no accept candidate");
			return new Move(false);
		}
		all_road_cells.addAll(candidates.get(chosen).road);
		return candidates.get(chosen);
	}

	private ArrayList<Move> getFacCandidates(Building request, Land land) {
		ArrayList<Move> candidates = new ArrayList<Move>();
		// get candidates position
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
		return candidates;
	}

	private ArrayList<Evaluation> getFacEvaluation(ArrayList<Move> candidates, Building request, Land land) {
		ArrayList<Evaluation> scores = new ArrayList<Evaluation>();

		int min_vertical_dis = land.side;
		for (int i = 0; i < candidates.size(); ++i) {
			Evaluation sc = new Evaluation();

			Move current = candidates.get(i);
			Set<Cell> bcells = new HashSet<Cell>();
			for (Cell x : current.request.rotations()[current.rotation]) {
				bcells.add(new Cell(x.i + current.location.i, x.j + current.location.j));
			}

			sc.vertical_dis = calVerticalDis(0, bcells, land);
			if (sc.vertical_dis > min_vertical_dis + VERTICAL_GAP) {
				current.accept = false;
				scores.add(sc);
				continue;
			}

			Set<Cell> shifted_cells = new HashSet<Cell>();
			shifted_cells.addAll(bcells);

			current.road = findRoad(bcells, land);
			if (current.road == null) {
				System.out.println("no road");
				current.accept = false;
				scores.add(sc);
				continue;
			}
			sc.road_len = current.road.size();

			min_vertical_dis = Math.min(min_vertical_dis, sc.vertical_dis);

			calPerimeter(sc, bcells, land); // calculate peri_delta & num_seg
			sc.score = request.size();
			scores.add(sc);
		}
		return scores;
	}

	private ArrayList<Move> getResCandidates(Building request, Land land) {
		ArrayList<Move> candidates = new ArrayList<Move>();
		// get candidates position
		for (int dx = land.side - 1; dx >= 0; --dx)
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
		return candidates;
	}


	private ArrayList<Evaluation> getResEvaluation(ArrayList<Move> candidates, Building request, Land land) {
		ArrayList<Evaluation> scores = new ArrayList<Evaluation>();

		int min_vertical_dis = land.side;
		for (int i = 0; i < candidates.size(); ++i) {
			Evaluation sc = new Evaluation();

			Move current = candidates.get(i);
			Set<Cell> bcells = new HashSet<Cell>();
			for (Cell x : current.request.rotations()[current.rotation]) {
				bcells.add(new Cell(x.i + current.location.i, x.j + current.location.j));
			}

			sc.vertical_dis = calVerticalDis(land.side, bcells, land);
			if (sc.vertical_dis > min_vertical_dis + VERTICAL_GAP) {
				current.accept = false;
				scores.add(sc);
				continue;
			}

			Set<Cell> shifted_cells = new HashSet<Cell>();
			shifted_cells.addAll(bcells);

			current.road = findRoad(bcells, land);
			if (current.road == null) {
				System.out.println("no road");
				current.accept = false;
				scores.add(sc);
				continue;
			}
			sc.road_len = current.road.size();

			min_vertical_dis = Math.min(min_vertical_dis, sc.vertical_dis);

			calPerimeter(sc, bcells, land); // calculate peri_delta & num_seg

			shifted_cells.addAll(current.road);
			current.water = buildWaterOrPark(Cell.Type.WATER, bcells, shifted_cells, land);

			shifted_cells.addAll(current.water);
			current.park = buildWaterOrPark(Cell.Type.PARK, bcells, shifted_cells, land);

			sc.score = calScore(bcells, current, land);
			scores.add(sc);
		}
		return scores;
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
