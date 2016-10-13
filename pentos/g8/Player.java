package pentos.g0;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();
	private ArrayList<ArrayList<Move>> optimized = new ArrayList<ArrayList<Move>>();
	private ArrayList<Building> buildingTypes = new ArrayList<Building>();

	private ArrayList<Set<Cell>> parks = new ArrayList<Set<Cell>>();
	private ArrayList<Set<Cell>> allRoads = new ArrayList<Set<Cell>>();
	private ArrayList<Set<Cell>> allPonds = new ArrayList<Set<Cell>>();


	// The boundary of optimized region
	private static final int OPT_TOP = 29;
	private static final int OPT_BUT = 49;
	private static final int OPT_RIGHT = 20;
	private static final int OPT_LEFT = 0;
	private boolean pond = true;

	
	// function is called once at the beginning before play is called
	public void init() { 
		addParks();
		addWater();
		addRoads();
		
		addShape1();
		addShape2();
		addShape3();
		addShape4();
		addShape5();
		addShape6();
		addShape7();
		addShape8();
		addShape9();
		addShape10();
		addShape11();
		addShape12();
		addShape13();
	}

	public Move play(Building request, Land land) {

		// find all valid building locations and orientations
		ArrayList<Move> moves = new ArrayList<Move>();
		ArrayList<Move> moves_opt = new ArrayList<Move>();
		ArrayList<Integer> largestArea = new ArrayList<Integer>();

		Move move_opt = getOptMove(request, land);

		if (move_opt != null) {
			return move_opt;
		}

		for (int i = 0; i < land.side; i++)
			for (int j = 0; j < land.side; j++) {
				Cell p = new Cell(i, j);
				Building[] rotations = request.rotations();
				for (int ri = 0; ri < rotations.length; ri++) {
					Building b = rotations[ri];
					if (land.buildable(b, p)) {
						Move m = new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(),
							new HashSet<Cell>());
						if (!isInOptimizedArea(m)) {
							moves.add(m);
						} else {
							moves_opt.add(m);
						}

					}
				}
			}

			Move move_normal = getNormalMove(moves, land, request, largestArea);
			if (move_normal != null) {
				return move_normal;
			}

			System.out.println("Putting it in optimized region");
			Move move_normal_in_optimized = getNormalMove(moves_opt, land, request, largestArea);
			if (move_normal_in_optimized != null) {
				return move_normal_in_optimized;
			}

			return new Move(false);
		}

		private boolean isInOptimizedArea(Move chosen) {
			int x = chosen.location.i;
			int y = chosen.location.j;
		// Inside optimized rectangle
			if (x > Player.OPT_TOP && x <= Player.OPT_BUT && y >= OPT_LEFT && y < OPT_RIGHT) {
				return true;
			}
			return false;
		}

		private boolean isSameMove(Move chosen, Move opt) {
			if (!isSameCell(chosen.location, opt.location)) { return false; }
			Building[] rotations_chosen = chosen.request.rotations();
			Building[] rotations_opt = opt.request.rotations();
			Building y = rotations_opt[opt.rotation];
			Building x = rotations_chosen[chosen.rotation];
			return isSameBuilding(x, y);
		}

		private boolean isSameCell(Cell a, Cell b) {
			return (a.i == b.i) && (a.j == b.j);
		}

		private boolean isSameBuilding(Building a, Building b) {
			return a.toString().equals(b.toString());
		}

		private Set<Cell> shiftCells(Move move) {
			Set<Cell> res = new HashSet<Cell>();
			Building[] rotations = move.request.rotations();
			Building b = rotations[move.rotation];
			for (Cell c : b) {
				res.add(new Cell(c.i + move.location.i, c.j + move.location.j));
			}
			return res;
		}

		private Move getNormalMove(List<Move> moves, Land land, Building request, List<Integer> areas) {

			PriorityQueue<int[]> pq = getCompactMoveIndex(moves, land, request);

			Set<Cell> shiftedCells = new HashSet<Cell>();
			Set<Cell> roadCells = new HashSet<Cell>();

			Move chosen = new Move(false);
			while (!pq.isEmpty()) {
				int[] pair = pq.poll();
				int index = pair[1];

				/*if (request.type == Building.Type.FACTORY) {
					System.out.println("FACTORY score" + pair[0]);
				}*/
				chosen = moves.get(index);
				shiftedCells = shiftCells(chosen);
				roadCells = findShortestRoad(shiftedCells, land);
				if (roadCells != null) { 
					break; 
				}
			}

			if (roadCells != null) {
				chosen.road = roadCells;
				road_cells.addAll(roadCells);

				if (request.type == Building.Type.RESIDENCE) { // for residences, build random ponds and fields connected to it
					Set<Cell> markedForConstruction = new HashSet<Cell>();
					markedForConstruction.addAll(roadCells);
					chosen.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
					markedForConstruction.addAll(chosen.water);
					chosen.park = randomWalk(shiftedCells, markedForConstruction, land, 4);
				}
if(chosen.request == null){
	return null;
}

				if(!land.buildable(chosen.request.rotations()[chosen.rotation], chosen.location)){
						return null;
				}
				return chosen;	
			} 
			
			return null;
		}


	private Move getNormalMove2(List<Move> moves, Land land, Building request, List<Integer> areas) {
		while (!moves.isEmpty()) {// reject if no valid placements

			int max = 0;
			for (int i = 1; i < areas.size(); i++) {
				if (areas.get(i) > areas.get(max))
					max = i;
			}

			int chosen_index = request.getType() == Building.Type.RESIDENCE ? max : moves.size() - 1;
			Move chosen = moves.get(chosen_index);
			// get coordinates of building placement (position plus local
			// building cell coordinates)
			Set<Cell> shiftedCells = new HashSet<Cell>();
			for (Cell x : chosen.request.rotations()[chosen.rotation])
				shiftedCells.add(new Cell(x.i + chosen.location.i, x.j + chosen.location.j));
			// build a road to connect this building to perimeter

			Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
			if (roadCells != null) {
				chosen.road = roadCells;
				road_cells.addAll(roadCells);
				if (request.type == Building.Type.RESIDENCE) { // for
																// residences,
																// build random
																// ponds and
																// fields
																// connected to
																// it
					Set<Cell> markedForConstruction = new HashSet<Cell>();
					markedForConstruction.addAll(roadCells);
					// find buildable region
					// put down 1
					Set<Cell> amenity = randomWalk(shiftedCells, markedForConstruction, land, 4);
					if (pond)
						chosen.water = amenity;
					else
						chosen.park = amenity;
					markedForConstruction.addAll(chosen.water);
					markedForConstruction.addAll(chosen.park);

				}
				return chosen;
			} else // reject placement if building cannot be connected by road
			{
				moves.remove(chosen_index);
			}
		}
		return null;
	}


	private boolean connectedToRoad(Move chosen){
		
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : chosen.request.rotations()[chosen.rotation])
			shiftedCells.add(new Cell(x.i + chosen.location.i, x.j + chosen.location.j));

		for (Cell p : shiftedCells) {
			for (Cell q : p.neighbors()) {
				if(road_cells.contains(q)){
					return true;
				}
			}
		}
		return false;		
	}

	private int getWeight(Set<Cell> shiftedCells, Land land, Building request) {
		int weight = 0; 
		for (Cell shiftedCell : shiftedCells) {
			for (Cell neighbor : shiftedCell.neighbors()) {
				if (!shiftedCells.contains(neighbor) && land.unoccupied(neighbor)) { 
					weight += 10; 
				}

				/*if (!shiftedCells.contains(neighbor) && !land.unoccupied(neighbor)) { 
					weight -= 20; 
				}*/

				
				if (request.type == Building.Type.RESIDENCE) {
					if (land.isPond(neighbor) || land.isField(neighbor)) {
						weight -= 3;
					}
				}

				if (request.type == Building.Type.FACTORY) {

					if (land.isPond(neighbor) || land.isField(neighbor)) {
						weight += 100000;
					}
				}
			}
		}
		return weight;
	}

	private PriorityQueue<int[]> getCompactMoveIndex(List<Move> moves, Land land, Building request) {
		PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);

		for (int i = 0 ; i < moves.size() ; ++i) {
			int index = i;
			index = request.type == Building.Type.FACTORY ? index : moves.size() - index -1 ;
			Set<Cell> shiftedCells = shiftCells(moves.get(index));
			int weight = getWeight(shiftedCells, land, request);
			int[] pair = {weight, index};
			pq.offer(pair);

		}
		return pq;
	}


	private int getBiggestSpace(Move move, Land land){
		int maxArea = 0;
		int width = 2;
		int height = 2;
		Building space = createSpace(width, height); //create a 1 cell factory
		boolean bigger = true;
		while (bigger){
			boolean found = false;
			for (int i = 0; i < land.side; i++){
				for (int j = 0; j < land.side; j++) {
					if (!found){
						Cell p = new Cell(i, j);
						Building[] rotations = space.rotations();
						for (int ri = 0; ri < rotations.length; ri++) {
							Building b = rotations[ri];
							if (land.buildable(b, p)) {
								found = true;
								int temp = width * height;
								if (maxArea < temp)
									maxArea = temp;
							} 
						}

					}
				}

			}
			if (found && width < land.side && height < land.side ){
				width++;
				height++;
				space = createSpace(width, height); //create a 1 cell factory

			}
			else{
				bigger = false;
			}
		}
		return maxArea;
	}

	public Building createSpace(int w, int h){

		Set<Cell> space = new HashSet<Cell>();
		for (int i=0; i<w; i++) {
			for (int j=0; j<h; j++) {
				space.add(new Cell(i,j));
			}
		}
		return new Building(space.toArray(new Cell[space.size()]), Building.Type.FACTORY);
	}

	private Move getOptMove(Building request, Land land) {

		for (int i = 0; i < optimized.size(); i++) {
			if (request.equals(buildingTypes.get(i))) {
				if (!optimized.get(i).isEmpty()) {
					
					Move m = optimized.get(i).get(0);
					optimized.get(i).remove(0);
					
					int nextToParkOrWater = nextToWaterOrPark(m, land);
					
					if(nextToParkOrWater == 3){
						m.park = new HashSet<Cell>();
						m.water = new HashSet<Cell>();
					}else if(nextToParkOrWater == 2){
						m.water = new HashSet<Cell>();
					}else if(nextToParkOrWater == 1){
						m.park = new HashSet<Cell>();
					}
					
					Building hardCoded = m.request;
					
					m.rotation = getCorrectRotation(request, hardCoded, m.rotation);
					m.request = request;

					if(connectedToRoad(m)){
						m.road = new HashSet<Cell>();
					}else{
						road_cells.addAll(m.road);
					}
					if(!land.buildable(m.request.rotations()[m.rotation], m.location)){
						return null;
					}
					
					return m;
				}
			}
		}

		return null;
	}

	private int nextToWaterOrPark(Move chosen, Land land) {
		boolean park = false;
		boolean water = false;
		
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : chosen.request.rotations()[chosen.rotation])
			shiftedCells.add(new Cell(x.i + chosen.location.i, x.j + chosen.location.j));

		for (Cell p : shiftedCells) {
			for (Cell q : p.neighbors()) {
				if (land.isField(q)) {
					park = true;
				}
				if (land.isPond(q)) {
					water = true;
				}
			}
		}
		
		if(park && water){
			return 3;
		}else if(park){
			return 1;
		}else if(water){
			return 2;
		}else{
			return 0;
		}
	}

	private int getCorrectRotation(Building request, Building hardCoded, int rotationOfHardCoded) {
		Building newHardCoded = hardCoded.rotations()[rotationOfHardCoded];

		Building[] rotationsOfRequest = request.rotations();
		for (int i = 0; i < rotationsOfRequest.length; i++) {
			Building newRequest = rotationsOfRequest[i];
			if (newRequest.hash == newHardCoded.hash) {
				return i;
			}

		}
		return -1;
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
	// walk n consecutive cells starting from a building. Used to build a random
	// field or pond.
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
		for (int ii = 0; ii < n; ii++) {
			ArrayList<Cell> walk_cells = new ArrayList<Cell>();
			for (Cell p : tail.neighbors()) {
				if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p))
					walk_cells.add(p);
			}
			if (walk_cells.isEmpty()) {
				// return output; //if you want to build it anyway
				return new HashSet<Cell>();
			}
			output.add(tail);
			tail = walk_cells.get(gen.nextInt(walk_cells.size()));
		}
		pond = !pond;
		return output;
	}
	
	public void addParks() {

		Set<Cell> parkSet = new HashSet<Cell>();

		//0
		parkSet.add(new Cell(49, 3));
		parkSet.add(new Cell(48, 3));
		parkSet.add(new Cell(48, 2));
		parkSet.add(new Cell(47, 2));

		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//1
		parkSet.add(new Cell(44, 0));
		parkSet.add(new Cell(44, 1));
		parkSet.add(new Cell(44, 2));
		parkSet.add(new Cell(44, 3));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//2
		parkSet.add(new Cell(47, 6));
		parkSet.add(new Cell(46, 6));
		parkSet.add(new Cell(46, 8));
		parkSet.add(new Cell(46, 7));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//3
		parkSet.add(new Cell(47, 12));
		parkSet.add(new Cell(47, 13));
		parkSet.add(new Cell(46, 12));
		parkSet.add(new Cell(46, 13));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//4
		parkSet.add(new Cell(45, 17));
		parkSet.add(new Cell(45, 18));
		parkSet.add(new Cell(46, 17));
		parkSet.add(new Cell(46, 18));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//5
		parkSet.add(new Cell(39, 4));
		parkSet.add(new Cell(39, 5));
		parkSet.add(new Cell(38, 5));
		parkSet.add(new Cell(37, 5));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//6
		parkSet.add(new Cell(39, 8));
		parkSet.add(new Cell(39, 9));
		parkSet.add(new Cell(38, 9));
		parkSet.add(new Cell(38, 10));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//7
		parkSet.add(new Cell(40, 12));
		parkSet.add(new Cell(40, 13));
		parkSet.add(new Cell(41, 12));
		parkSet.add(new Cell(42, 12));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//8
		parkSet.add(new Cell(37, 16));
		parkSet.add(new Cell(38, 16));
		parkSet.add(new Cell(39, 16));
		parkSet.add(new Cell(40, 16));
		parkSet.add(new Cell(40, 17));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//9
		parkSet.add(new Cell(33, 2));
		parkSet.add(new Cell(33, 3));
		parkSet.add(new Cell(33, 4));
		parkSet.add(new Cell(33, 5));
		parkSet.add(new Cell(32, 4));
		parkSet.add(new Cell(31, 4));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//10
		parkSet.add(new Cell(33, 9));
		parkSet.add(new Cell(32, 9));
		parkSet.add(new Cell(32, 10));
		parkSet.add(new Cell(31, 10));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//11
		parkSet.add(new Cell(31, 14));
		parkSet.add(new Cell(31, 15));
		parkSet.add(new Cell(31, 16));
		parkSet.add(new Cell(32, 16));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
	}

	public void addWater() {
		
		Set<Cell> ponds = new HashSet<Cell>();
		
		//0
		ponds.add(new Cell(47, 0));
		ponds.add(new Cell(47, 1));
		ponds.add(new Cell(46, 1));
		ponds.add(new Cell(46, 2));
		ponds.add(new Cell(46, 3));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//1
		ponds.add(new Cell(36, 1));
		ponds.add(new Cell(36, 2));
		ponds.add(new Cell(37, 1));
		ponds.add(new Cell(37, 2));
		ponds.add(new Cell(38, 2));
		ponds.add(new Cell(38, 3));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//2
		ponds.add(new Cell(37, 6));
		ponds.add(new Cell(38, 6));
		ponds.add(new Cell(39, 6));
		ponds.add(new Cell(38, 7));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//3
		ponds.add(new Cell(48, 14));
		ponds.add(new Cell(48, 15));
		ponds.add(new Cell(49, 14));
		ponds.add(new Cell(49, 15));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//4
		ponds.add(new Cell(43, 12));
		ponds.add(new Cell(43, 13));
		ponds.add(new Cell(43, 14));
		ponds.add(new Cell(44, 13));
		ponds.add(new Cell(45, 13));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//5
		ponds.add(new Cell(47, 5));
		ponds.add(new Cell(48, 5));
		ponds.add(new Cell(48, 6));
		ponds.add(new Cell(49, 6));
		ponds.add(new Cell(49, 7));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//6
		ponds.add(new Cell(36, 15));
		ponds.add(new Cell(37, 15));
		ponds.add(new Cell(36, 16));
		ponds.add(new Cell(36, 17));
		ponds.add(new Cell(36, 18));
		ponds.add(new Cell(36, 19));
		ponds.add(new Cell(37, 19));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//7
		ponds.add(new Cell(42, 18));
		ponds.add(new Cell(42, 19));
		ponds.add(new Cell(43, 18));
		ponds.add(new Cell(43, 19));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//8
		ponds.add(new Cell(33, 1));
		ponds.add(new Cell(32, 1));
		ponds.add(new Cell(32, 2));
		ponds.add(new Cell(32, 3));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//9
		ponds.add(new Cell(31, 6));
		ponds.add(new Cell(31, 7));
		ponds.add(new Cell(32, 7));
		ponds.add(new Cell(32, 8));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//10
		ponds.add(new Cell(33, 11));
		ponds.add(new Cell(33, 12));
		ponds.add(new Cell(32, 12));
		ponds.add(new Cell(32, 13));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//11
		ponds.add(new Cell(31, 17));
		ponds.add(new Cell(31, 18));
		ponds.add(new Cell(32, 17));
		ponds.add(new Cell(32, 18));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//12
		ponds.add(new Cell(40, 11));
		ponds.add(new Cell(39, 11));
		ponds.add(new Cell(39, 12));
		ponds.add(new Cell(38, 12));
		
		allPonds.add(ponds);
	}

	public void addRoads() {
		
		Set<Cell> roads = new HashSet<Cell>();
		
		//0
		roads.add(new Cell(49, 20));
		roads.add(new Cell(48, 20));
		roads.add(new Cell(47, 20));
		roads.add(new Cell(46, 20));
		roads.add(new Cell(45, 20));
		roads.add(new Cell(44, 20));
		roads.add(new Cell(43, 20));
		roads.add(new Cell(42, 20));
		roads.add(new Cell(41, 20));
		roads.add(new Cell(40, 20));
		roads.add(new Cell(39, 20));
		roads.add(new Cell(38, 20));
		roads.add(new Cell(37, 20));
		roads.add(new Cell(36, 20));
		roads.add(new Cell(35, 20));
		
		roads.add(new Cell(41, 19));
		roads.add(new Cell(41, 18));
		roads.add(new Cell(41, 17));
		roads.add(new Cell(41, 16));
		roads.add(new Cell(42, 16));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//1
		roads.add(new Cell(35, 19));
		roads.add(new Cell(35, 18));
		roads.add(new Cell(35, 17));
		roads.add(new Cell(35, 16));
		roads.add(new Cell(35, 15));
		roads.add(new Cell(35, 14));
		roads.add(new Cell(35, 13));
		roads.add(new Cell(35, 12));
		roads.add(new Cell(35, 11));
		roads.add(new Cell(35, 10));
		roads.add(new Cell(35, 9));
		roads.add(new Cell(35, 8));
		roads.add(new Cell(35, 7));
		roads.add(new Cell(35, 6));
		roads.add(new Cell(35, 5));
		roads.add(new Cell(35, 4));
		roads.add(new Cell(35, 3));
		roads.add(new Cell(35, 2));
		roads.add(new Cell(35, 1));
		roads.add(new Cell(35, 0));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//2
		roads.add(new Cell(49, 9));
		roads.add(new Cell(48, 9));
		roads.add(new Cell(47, 9));
		roads.add(new Cell(46, 9));
		roads.add(new Cell(45, 9));
		roads.add(new Cell(44, 9));
		roads.add(new Cell(43, 9));
		roads.add(new Cell(42, 9));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//3
		roads.add(new Cell(42, 8));
		roads.add(new Cell(42, 7));
		roads.add(new Cell(42, 6));
		roads.add(new Cell(42, 5));
		roads.add(new Cell(42, 4));
		roads.add(new Cell(42, 3));
		roads.add(new Cell(42, 2));
		roads.add(new Cell(42, 1));
		roads.add(new Cell(42, 0));

		roads.add(new Cell(43, 5));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//4
		roads.add(new Cell(49, 16));
		roads.add(new Cell(48, 16));
		roads.add(new Cell(47, 16));
		roads.add(new Cell(46, 16));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//5
		roads.add(new Cell(29, 0));
		roads.add(new Cell(29, 1));
		roads.add(new Cell(29, 2));
		roads.add(new Cell(29, 3));
		roads.add(new Cell(29, 4));
		roads.add(new Cell(29, 5));
		roads.add(new Cell(29, 6));
		roads.add(new Cell(29, 7));
		roads.add(new Cell(29, 8));
		roads.add(new Cell(29, 9));
		roads.add(new Cell(29, 10));
		roads.add(new Cell(29, 11));
		roads.add(new Cell(29, 12));
		roads.add(new Cell(29, 13));
		roads.add(new Cell(29, 14));
		roads.add(new Cell(29, 15));
		roads.add(new Cell(29, 16));
		roads.add(new Cell(29, 17));
		roads.add(new Cell(29, 18));
		roads.add(new Cell(29, 19));
		roads.add(new Cell(29, 20));
		roads.add(new Cell(30, 20));
		roads.add(new Cell(31, 20));
		roads.add(new Cell(32, 20));
		roads.add(new Cell(33, 20));
		roads.add(new Cell(34, 20));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();
	}

	/* . .
	 * . . .
	 * 
	 */
	public void addShape1() {
		ArrayList<Move> shape1 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(49, 0);
		building[1] = new Cell(48, 0);
		building[2] = new Cell(48, 1);
		building[3] = new Cell(49, 1);
		building[4] = new Cell(49, 2);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(48, 0), 0, new HashSet<Cell>(),
			allPonds.get(0), parks.get(0)));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(43, 6), 2, allRoads.get(2),
			new HashSet<Cell>(), new HashSet<Cell>()));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(47, 10), 0, allRoads.get(2),
			new HashSet<Cell>(), parks.get(3)));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 14), 3, allRoads.get(4),
			allPonds.get(3), parks.get(3)));
		optimized.add(shape1);
	}

	/*
	 * . . . .
	 * .
	 */
	public void addShape2() {
		ArrayList<Move> shape2 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(46, 0);
		building[1] = new Cell(45, 0);
		building[2] = new Cell(45, 1);
		building[3] = new Cell(45, 2);
		building[4] = new Cell(45, 3);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 0), 0, new HashSet<Cell>(),
			allPonds.get(0), parks.get(1)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 5), 0, allRoads.get(2),
			allPonds.get(5),parks.get(2)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(48, 10), 2, new HashSet<Cell>(),
			allPonds.get(3), parks.get(3)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 12), 1, allRoads.get(0),
			allPonds.get(12), parks.get(7)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 7), 2, allRoads.get(1),
			allPonds.get(10), parks.get(10)));

		optimized.add(shape2);
	}

	public void addShape3() {
		ArrayList<Move> shape3 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(49, 4);
		building[1] = new Cell(48, 4);
		building[2] = new Cell(47, 4);
		building[3] = new Cell(46, 4);
		building[4] = new Cell(47, 3);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		optimized.add(shape3);
	}

	/*
	 * . .
	 * .
	 * . .
	 */
	public void addShape4() {
		ArrayList<Move> shape4 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(49, 5);
		building[1] = new Cell(49, 6);
		building[2] = new Cell(48, 5);
		building[3] = new Cell(47, 5);
		building[4] = new Cell(47, 6);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(44, 14), 1, allRoads.get(4),
			allPonds.get(4), parks.get(4)));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(39, 2), 0, allRoads.get(3),
			allPonds.get(1), parks.get(5)));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(32, 14), 2, allRoads.get(1),
			allPonds.get(10), parks.get(11)));

		optimized.add(shape4);
	}

	/*   . .
	 * . . .
	 */
	public void addShape5() {
		ArrayList<Move> shape5 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(45, 6);
		building[1] = new Cell(45, 7);
		building[2] = new Cell(45, 8);
		building[3] = new Cell(44, 7);
		building[4] = new Cell(44, 8);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(47, 7), 3, new HashSet<Cell>(),
			allPonds.get(5), parks.get(2)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 10), 2, allRoads.get(2),
			allPonds.get(4), parks.get(3)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 3), 3, allRoads.get(1),
			allPonds.get(1),parks.get(5)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 16), 2, allRoads.get(1),
			allPonds.get(11), parks.get(11)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 12), 2, allRoads.get(5),
			allPonds.get(10), parks.get(11)));

		optimized.add(shape5);
	}

	/*
	 * . . . . .
	 */
	public void addShape6() {
		ArrayList<Move> shape6 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(43, 0);
		building[1] = new Cell(43, 1);
		building[2] = new Cell(43, 2);
		building[3] = new Cell(43, 3);
		building[4] = new Cell(43, 4);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(43, 0), 0, new HashSet<Cell>(),
			new HashSet<Cell>(), parks.get(1)));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 0), 1, new HashSet<Cell>(),
			allPonds.get(1), new HashSet<Cell>()));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 14), 1, allRoads.get(1),
			allPonds.get(6),parks.get(7)));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(34, 0), 0, new HashSet<Cell>(),
			allPonds.get(8), parks.get(9)));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 15), 0, allRoads.get(5),
			allPonds.get(11), parks.get(11)));

		optimized.add(shape6);
	}

	/* . .
	 *   .
	 *   . .
	 */
	public void addShape7() {
		ArrayList<Move> shape7 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(44, 4);
		building[1] = new Cell(44, 5);
		building[2] = new Cell(45, 5);
		building[3] = new Cell(46, 5);
		building[4] = new Cell(46, 6);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape7.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(47, 3), 0, new HashSet<Cell>(),
			allPonds.get(5), parks.get(0)));
		shape7.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(39, 8), 1, allRoads.get(3),allPonds.get(12), parks.get(6)));
		shape7.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 4), 0, allRoads.get(5),allPonds.get(9), parks.get(9)));

		optimized.add(shape7);
	}

	/*   .
	 *   .
	 *   .
	 * . . 
	 */
	public void addShape8() {
		ArrayList<Move> shape8 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(47, 18);
		building[1] = new Cell(47, 19);
		building[2] = new Cell(46, 19);
		building[3] = new Cell(45, 19);
		building[4] = new Cell(44, 19);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(44, 18), 0, allRoads.get(0),
			allPonds.get(7), parks.get(4)));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(38, 14), 0, allRoads.get(0),
			allPonds.get(6), parks.get(8)));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(38, 0), 0, new HashSet<Cell>(),
			allPonds.get(1), new HashSet<Cell>()));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(31, 18), 0, allRoads.get(1),
			allPonds.get(11), new HashSet<Cell>()));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 0), 3, new HashSet<Cell>(),
			allPonds.get(8), parks.get(9)));

		optimized.add(shape8);
	}

	/*   
	 *   . . .
	 *   .
	 *   .
	 */
	public void addShape9() {
		ArrayList<Move> shape9 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(44, 4);
		building[1] = new Cell(44, 5);
		building[2] = new Cell(44, 6);
		building[3] = new Cell(45, 4);
		building[4] = new Cell(46, 4);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(44, 4), 0, allRoads.get(3),
			allPonds.get(0), parks.get(1)));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(39, 5), 2, allRoads.get(3),
			allPonds.get(2), parks.get(6)));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(31, 0), 0, new HashSet<Cell>(),
			allPonds.get(8), new HashSet<Cell>()));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 9), 1, allRoads.get(5),
			allPonds.get(10), parks.get(10)));

		optimized.add(shape9);
	}

	/*
	 *   . .
	 * . .
	 * .
	 */
	public void addShape10() {
		ArrayList<Move> shape10 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(38, 8);
		building[1] = new Cell(37, 8);
		building[2] = new Cell(37, 9);
		building[3] = new Cell(36, 9);
		building[4] = new Cell(36, 10);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape10.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 8), 0, allRoads.get(1),
			allPonds.get(2), parks.get(6)));
		shape10.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(38, 17), 3, allRoads.get(0),
			new HashSet<Cell>(), parks.get(8)));
		shape10.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(37, 17), 1, allRoads.get(0),
			allPonds.get(6), parks.get(8)));
		optimized.add(shape10);
	}

	/*
	 * . . .
	 *     . .
	 * 
	 */
	public void addShape11() {
		ArrayList<Move> shape11 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(30, 6);
		building[1] = new Cell(30, 7);
		building[2] = new Cell(30, 8);
		building[3] = new Cell(31, 8);
		building[4] = new Cell(31, 9);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape11.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 6), 0, allRoads.get(5),
			allPonds.get(9), parks.get(10)));

		optimized.add(shape11);
	}

	/*
	 *    . . .
	 *  . .
	 */
	public void addShape12() {
		ArrayList<Move> shape12 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(34, 5);
		building[1] = new Cell(34, 6);
		building[2] = new Cell(33, 6);
		building[3] = new Cell(33, 7);
		building[4] = new Cell(33, 8);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape12.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 5), 0, allRoads.get(1),
			allPonds.get(9),parks.get(9)));
		shape12.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 11), 2, allRoads.get(1),
			allPonds.get(10), new HashSet<Cell>()));

		optimized.add(shape12);
	}

	/*
	 * .
	 * . . .
	 *     .
	 * 
	 */
	public void addShape13() {
		ArrayList<Move> shape13 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(41, 13);
		building[1] = new Cell(42, 13);
		building[2] = new Cell(42, 14);
		building[3] = new Cell(42, 15);
		building[4] = new Cell(43, 15);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape13.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(41, 13), 0, allRoads.get(0),
			allPonds.get(4), parks.get(7)));

		optimized.add(shape13);
	}
}
