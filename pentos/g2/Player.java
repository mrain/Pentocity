package pentos.g2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

public class Player implements pentos.sim.Player {
	private Random gen = new Random();
	final int ITERATION_COUNT = 200;
	final int side = 50;
	private boolean[][] isDisconnected;
	private Set<Cell> road_cells = new HashSet<Cell>();
	static int count = 0;

	private static int INF  = (int)1e9;
	private static int SLOT_GAP = 15;

	private class Slot {
		public int start = -1;
		public int len = -1;
		public int cursor = 0;
		public int road_pos = -1;
		public int bottom = -1;

		public Slot(int _start, int _len, int _road_pos, Land land) {
			start = _start;
			len = _len;
			cursor = land.side - 1;
			road_pos = _road_pos;
			bottom = start - len;
			if (road_pos == bottom) --bottom;
		}

		public SlotChoice fit(Building request, Land land) {
			SlotChoice res = new SlotChoice();
			res.height = -1;

			int[] states = getPosition(request, cursor, start, len);
			if (states[0] < 0 || states[1] < 0) return res;

			Building b = request.rotations()[states[2]];
			int[] scale = getScale(b);
			Cell pos = new Cell(states[0], states[1]);

			if (land.buildable(b, pos) && forRoad(cursor - scale[0] + 1, cursor, road_pos, land)) {
				res.height = land.side - 1 - cursor + scale[0];
				res.pos = pos;
				res.rotation = states[2];
			}

			return res;
		}

		public HashSet<Cell> build(SlotChoice choice, Land land) {
			HashSet<Cell> road = new HashSet<Cell>();
			if (road_pos < 0 || road_pos >= land.side) {
				cursor = land.side - choice.height;
				return road;
			}

			for (int i = land.side - choice.height; i <= cursor; ++i) {
				if (land.unoccupied(i, road_pos)) road.add(new Cell(i, road_pos));
			}
			cursor = land.side - choice.height;

			return road;
		}

	}

	private ArrayList<Slot> slot = new ArrayList<Slot>();

	private boolean forRoad(int x1, int x2, int y, Land land) {
		if (y == -1 || y >= land.side) return true;
		for (int i = x1; i <= x2; ++i)
			if (!road_cells.contains(new Cell(i, y)) && !land.unoccupied(i, y)) return false;
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
				res[0] = res[1] = 0;
				for (Cell c : bcells) {
					res[0] = Math.max(res[0], c.i);
					res[1] = Math.max(res[1], c.j);
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

		int start = land.side - 1;
		if (numSlot > 0) start = slot.get(numSlot - 1).bottom;
		int[] pos = getPosition(request, land.side - 1, start, scale[1]); // position.x, position.y, rotation
		if (pos[0] >= 0 && pos[1] >= 0 && land.buildable(request.rotations()[pos[2]], new Cell(pos[0], pos[1]))) {
			slot.add(new Slot(start, scale[1], (((numSlot % 2) == 1)?(start - scale[1]):(start + 1)), land));
			return true;
		} else {
			pos = getPosition(request, land.side - 1, start, scale[0]);
			if (pos[0] < 0 || pos[1] < 0 || !land.buildable(request.rotations()[pos[2]], new Cell(pos[0], pos[1]))) {
				return false;
			} else {
				slot.add(new Slot(start, scale[0], (((numSlot % 2) == 1)?(start - scale[0]):(start + 1)), land));
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


		isDisconnected = new boolean[side][side];
	}

	public Move getBestMove(Building request, Land land) {
		//find first free location row by row
		if(request.type == Building.Type.RESIDENCE) {
			Building[] rotations = request.rotations();
			int best_i = land.side + 1,
			    best_j = land.side + 1;
			int best_perimeter = -1;
			Move best_move = null;


			best_i = land.side + 1;
			best_j = land.side + 1;

			for (int i = 0; i < land.side; ++i) 
				for (int j = 0; j < land.side; ++j) {
					Cell p = new Cell(i,j);
					for (int ri = 0; ri < rotations.length; ++ri)
						if(land.buildable(rotations[ri], p)) {
							Move temp = new Move(true, 
									request, 
									p, 
									ri, 
									new HashSet<Cell>(), 
									new HashSet<Cell>(), 
									new HashSet<Cell>());
							boolean disconnected = false;
							Set<Cell> shiftedCells = new HashSet<Cell>();
							for (Cell x : temp.request.rotations()[temp.rotation]) {
								shiftedCells.add(
										new Cell(x.i+temp.location.i,
											x.j+temp.location.j));
								disconnected |= 
									isDisconnected[x.i + temp.location.i][x.j + temp.location.j];
							}

							int perimeter = 0;
							for(Cell x : shiftedCells) {
								for(Cell y : x.neighbors()) {
									if(!land.unoccupied(y)) {
										++perimeter;
									}
								}
								if(x.i == 0 || x.i == land.side - 1) ++ perimeter;
								if(x.j == 0 || x.j == land.side - 1) ++ perimeter;						
							}
							// builda road to connect this building to perimeter

							if(!disconnected && ((perimeter > best_perimeter)
										||(perimeter==best_perimeter && (i  + j) < best_i +  best_j)
										|| (perimeter==best_perimeter && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j))) {
								Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
								if(roadCells != null) {
									best_move = temp;
									best_i = i;
									best_j = j;
									best_perimeter = perimeter;
								} else {
									for(Cell x : shiftedCells) {
										isDisconnected[x.i][x.j] = true;
									}
								}
										}				
						}
				}

			return best_move;
			//find closest free location to end
		} else {
			return playFactoryBySlot(request, land);
			//return playFactory(request, land);
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
			HashSet<Cell> road = slot.get(slot.size() - 1).build(res, land);
			System.out.println();
			road_cells.addAll(road);
			return new Move(true, request, res.pos, res.rotation, road, new HashSet<Cell>(), new HashSet<Cell>());
		}

		if (candidates.size() == 0) {
			if (newSlot(request, land) == false) return playFactory(request, land);
			SlotChoice res = slot.get(slot.size() - 1).fit(request, land);
			HashSet<Cell> road = slot.get(slot.size() - 1).build(res, land);
			road_cells.addAll(road);
			return new Move(true, request, res.pos, res.rotation, road, new HashSet<Cell>(), new HashSet<Cell>());
		}

		candidates.get(chosen).road = slot.get(choices.get(chosen).slot_id).build(choices.get(chosen), land);
		road_cells.addAll(candidates.get(chosen).road);
		return candidates.get(chosen);
	}



	private Move playFactory(Building request, Land land) {
		Building[] rotations = request.rotations();
		int best_i = land.side + 1,
		    best_j = land.side + 1;
		int best_perimeter = -1;
		Move best_move = null;


		best_i = -1;
		best_j = -1;
		best_perimeter = -1;
		for (int i = land.side - 1; i >= 0; --i) 
			for ( int j = land.side - 1; j >= 0; --j) {
				Cell p = new Cell(i,j);
				for (int ri = 0; ri < rotations.length; ++ri)
					if(land.buildable(rotations[ri], p)) {
						Move temp = new Move(true, 
								request, 
								p, 
								ri, 
								new HashSet<Cell>(), 
								new HashSet<Cell>(), 
								new HashSet<Cell>());

						boolean disconnected = false;
						Set<Cell> shiftedCells = new HashSet<Cell>();
						for (Cell x : temp.request.rotations()[temp.rotation]) {
							shiftedCells.add(
									new Cell(x.i+temp.location.i,
										x.j+temp.location.j));
							disconnected |= 
								isDisconnected[x.i + temp.location.i][x.j + temp.location.j];
						}

						int perimeter = 0;
						for(Cell x : shiftedCells) {
							for(Cell y : x.neighbors()) {
								if(!land.unoccupied(y)) {
									++perimeter;
								}
							}
							if(x.i == 0 || x.i == land.side - 1) ++ perimeter;
							if(x.j == 0 || x.j == land.side - 1) ++ perimeter;						
						}
						// builda road to connect this building to perimeter
						if(!disconnected && (perimeter > best_perimeter 
									|| (perimeter==best_perimeter && (i  + j) >  best_i + best_j))
								||(perimeter==best_perimeter && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j)) {
							Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
							if(roadCells != null) {
								best_move = temp;
								best_i = i;
								best_j = j;
								best_perimeter = perimeter;
							} else {
								for(Cell x : shiftedCells) {
									isDisconnected[x.i][x.j] = true;
								}
							}
								}
					}
			}
		return best_move;
	}

	private static int num = 0;
	static Set<Cell> prev_water=null;
	public Move play(Building request, Land land) {
		Move best_move = getBestMove(request, land);
		if (request.type == Building.Type.FACTORY && best_move != null && best_move.road.size() > 0) return best_move;

		//no move
		if (best_move == null) {
			return new Move(false);
		}
		// get coordinates of building placement (position plus local building cell coordinates)
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : best_move.request.rotations()[best_move.rotation]) {
			shiftedCells.add(
					new Cell(x.i+best_move.location.i,
						x.j+best_move.location.j));
		}
		// builda road to connect this building to perimeter
		Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
		if (roadCells != null) {
			best_move.road = roadCells;
			road_cells.addAll(roadCells);
			//int x = gen.nextInt();
			if(request.type == request.type.RESIDENCE) {

				optimize_water_and_park(roadCells, land, best_move, shiftedCells);
			}

			return best_move;
		}
		else {// reject placement if building cannot be connected by road
			//Kailash: This should never happen now.
			return new Move(false);    
		}
	}

	void optimize_water_and_park(Set<Cell> roadCells, Land land, Move best_move, Set<Cell> shiftedCells) {
		Set<Cell> markedForConstruction = new HashSet<Cell>();
		markedForConstruction.addAll(roadCells);
		Set<Set<Cell>> park_options = getStraightParksAndPonds(land, best_move);

		Set<Cell> best_park = getBestRandomParkOrWater(shiftedCells, markedForConstruction, land, 1);


		best_park = getActualBestParkOrWater(park_options, land, best_park);
		best_move.park = best_park;
		markedForConstruction.addAll(best_move.park);

		if(park_options.contains(best_park)) {
			park_options.remove(best_park);
		}

		Set<Cell> best_water = getBestRandomParkOrWater(shiftedCells, markedForConstruction, land, 2);
		best_water = getActualBestParkOrWater(park_options, land, best_water);
		best_move.water = best_water;
	}

	private Set<Cell> getActualBestParkOrWater(Set<Set<Cell>> park_options, Land land, Set<Cell> best_park) {
		int best_perimeter = getPerimeter(land, best_park);
		for(Set<Cell> park_option: park_options) {
			int perimeter = getPerimeter(land, park_option);
			int size = park_option.size();
			if(size < best_park.size() || (size == best_park.size() && perimeter < best_perimeter)) {
				best_perimeter = perimeter;
				best_park = park_option;
			}
		}
		return best_park;
	}

	private Set<Set<Cell>> getStraightParksAndPonds(Land land, Move best_move) {
		Set<Set<Cell>> park_options = new HashSet<Set<Cell>>();
		for(Cell x : best_move.request.rotations()[best_move.rotation]) {
			int[] dx = {0,0,1,-1};
			int[] dy = {1,-1,0,0};
			for (Cell y : x.neighbors()) {
				int i = y.i;
				int j = y.j;
				for(int k=0;k<4;++k) {
					Set<Cell> option = new HashSet<Cell>();
					boolean empty = true;
					for(int cellnum = 0;cellnum<4;++cellnum) {
						if(!land.unoccupied(i + dx[k]*cellnum,j + dy[k]*cellnum)) {
							empty = false;
						} else {
							option.add(new Cell(i+dx[k]*cellnum, j + dy[k]*cellnum));
						}
					}
					if(empty) {
						park_options.add(option);
					}
				}
			}
		}
		return park_options;
	}

	private int getPerimeter(Land land, Set<Cell> cells) {
		int perimeter = 0;
		for(Cell x : cells) {
			for(Cell y: x.neighbors()) {
				if(!land.unoccupied(y)) {
					++perimeter;
				}
			}
		}
		return perimeter;
	}

	private Set<Cell> getBestRandomParkOrWater(Set<Cell> shiftedCells, Set<Cell> markedForConstruction, Land land, int type) {
		Set<Cell> best_park = new HashSet<Cell>();
		int best_perimeter = 100;
		int best_size = 100;
		for(int i = 0; i < ITERATION_COUNT; ++i) {
			int perimeter = 0;
			int size = 0;
			Set<Cell> park_option = randomWalk(shiftedCells, markedForConstruction, land, 4, type);
			size = park_option.size()>0?park_option.size():110;
			perimeter = getPerimeter(land, park_option);
			if(size < best_size || (size == best_size && perimeter < best_perimeter)) {
				best_perimeter = perimeter;
				best_park = park_option;
				best_size = size;
			}
		}
		return best_park;
	}

	// build shortest sequence of road cells to connect to a set of cells b
	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();
		// add border cells that don't have a road currently
		Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
		for(Cell x : b) {
			if(x.i==0 || x.i==land.side-1 || x.j==0 || x.j==land.side-1) return new HashSet<Cell>();
			for(Cell y : x.neighbors()) {
				if(road_cells.contains(y)) {
					return new HashSet<Cell>();
				}
			}
		}
		for (int z=0; z<land.side; z++) {
			if (b.contains(new Cell(0,z)) || b.contains(new Cell(z,0)) || b.contains(new Cell(land.side-1,z)) || b.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
				return output;
			if (land.unoccupied(0,z))
				queue.add(new Cell(0,z,source));
			if (land.unoccupied(z,0))
				queue.add(new Cell(z,0,source));
			if (land.unoccupied(z,land.side-1))
				queue.add(new Cell(z,land.side-1,source));
			if (land.unoccupied(land.side-1,z))
				queue.add(new Cell(land.side-1,z,source));
		}
		// add cells adjacent to current road cells

		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q)) 
					queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
			}
		}	
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			checked[p.i][p.j] = true;
			for (Cell x : p.neighbors()) {		
				if (b.contains(x)) { // trace back through search tree to find path
					Cell tail = p;
					while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
						output.add(new Cell(tail.i,tail.j));
						tail = tail.previous;
					}
					if (!output.isEmpty())
						return output;
				}
				else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
					x.previous = p;
					checked[x.i][x.j] = true;
					queue.add(x);
				} 

			}
		}
		if (output.isEmpty() && queue.isEmpty())
			return null;
		else
			return output;
	}

	// walk n consecutive cells starting from a building. Used to build a random field or pond. 
	private Set<Cell> randomWalk(Set<Cell> b, Set<Cell> marked, Land land, int n, int type) {
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
				if((land.isField(p) && type == 1) || (land.isPond(p) && type == 2)) {
					//	System.out.println("hi");
					output.add(tail);
					return output;
				}
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
