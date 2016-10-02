package pentos.cl1;

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
    public void init() { // function is called once at the beginning before play is called
    	
    	isDisconnected = new boolean[side][side];
    }

	private int getChangesOfEmptySpaces(Move move, Land land) {
		boolean[][] vis = new boolean[side][side];
		int ret = -1;	// If no empty cells nearby then it fills an empty hole
		
		Building[] rotations = move.request.rotations();
		Set<Cell> builds = new HashSet<Cell>();
		for (Cell cell : rotations[move.rotation])
			builds.add(cell);
		builds.addAll(move.road);
		builds.addAll(move.water);
		builds.addAll(move.park);
		Set<Cell> perimeter = new HashSet<Cell>();
		for (Cell cell : builds) {
			for (Cell p : cell.neighbors())
				if (land.unoccupied(p))
					perimeter.add(p);
		}

		for (Cell p : perimeter) {
			if (vis[p.i][p.j]) continue;

			// found a new connected empty region
			++ ret;
			Queue<Cell> queue = new LinkedList<Cell>();
			queue.add(p);
			vis[p.i][p.j] = true;
			while (!queue.isEmpty()) {
				Cell x = queue.poll();
				for (Cell y : x.neighbors()) {
					if (vis[y.i][y.j] || builds.contains(y) || !land.unoccupied(y)) continue;
					vis[y.i][y.j] = true;
					queue.add(y);
				}
			}
		}
		return ret;
	}

    public Move getBestMove(Building request, Land land) {
    	Building[] rotations = request.rotations();
    	int best_i = land.side + 1,
    		best_j = land.side + 1;
    	int best_perimeter = -1;
		int best_changes = 1000;
    	Move best_move = null;
    	
    	//find first free location row by row
	    if(request.type == Building.Type.RESIDENCE) {
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

					int changes = best_changes;
					if (perimeter == best_perimeter)
						changes = getChangesOfEmptySpaces(temp, land);
					
					if(!disconnected && ((perimeter > best_perimeter)
							|| (perimeter == best_perimeter && changes < best_changes)
							||(perimeter==best_perimeter && changes == best_changes && (i  + j) < best_i +  best_j)
							|| (perimeter==best_perimeter && changes == best_changes && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j))) {
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
	    //find closest free location to end
	    } else if(request.type == Building.Type.FACTORY) {
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
					int changes = getChangesOfEmptySpaces(temp, land);
					if(!disconnected && (perimeter > best_perimeter 
							|| (perimeter == best_perimeter && changes < best_changes)
							|| (perimeter==best_perimeter && changes == best_changes && (i  + j) >  best_i + best_j))
							||(perimeter==best_perimeter && changes == best_changes && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j)) {
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
	    }
	    return best_move;
    }
    
    private static int num = 0;
    static Set<Cell> prev_water=null;
    public Move play(Building request, Land land) {
	    Move best_move = getBestMove(request, land);
	    /*if(prev_water!=null) {
	    	boolean built = true;
	    	for(Cell x: prev_water) {
	    		if(!land.isPond(x)) {
	    			built = false;
	    		}
	    	}
	    	if(!built) {
	    		System.out.println("complain");
	    		if(prev_water.size()!=0 && prev_water.size()!=4) {
	    			System.out.println("built good");
	    		}
	    	}
	    }*/
	    //no move
		if (best_move == null) {
			//System.out.println("no moves");
			return new Move(false);
		}
	    // get coordinates of building placement (position plus local building cell coordinates)
	    Set<Cell> shiftedCells = new HashSet<Cell>();
	    //System.out.println(request.type == request.type.RESIDENCE);
	    //System.out.println();

//    	System.out.println("building: ");
	    for (Cell x : best_move.request.rotations()[best_move.rotation]) {
	    	shiftedCells.add(
	    			new Cell(x.i+best_move.location.i,
	    					x.j+best_move.location.j));
	    	//System.out.println(x.i + " " + x.j + " shifted to " + (x.i+best_move.location.i) + " " + (x.j + best_move.location.j));
	    }
	    // builda road to connect this building to perimeter
	    Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
	    if (roadCells != null) {
			best_move.road = roadCells;
			road_cells.addAll(roadCells);
			//int x = gen.nextInt();
			if(request.type == request.type.RESIDENCE) {
				
				Set<Cell> markedForConstruction = new HashSet<Cell>();
			    markedForConstruction.addAll(roadCells);
			    Set<Cell> best_park = new HashSet<Cell>();
			    int best_perimeter = 100;
			    int best_size = 100;
			    for(int i = 0; i < ITERATION_COUNT; ++i) {
			    	int perimeter = 0;
			    	int size = 0;
			    	Set<Cell> park_option = randomWalk(shiftedCells, markedForConstruction, land, 4, 1);
			    	size = park_option.size()>0?park_option.size():110;
			    	for(Cell x : park_option) {
			    		for(Cell y: x.neighbors()) {
			    			if(!land.unoccupied(y) /*&& !y.isRoad()*/) {
			    				++perimeter;
			    			}
			    		}
			    	}
			    	if(size < best_size || (size == best_size && perimeter < best_perimeter)) {
			    		best_perimeter = perimeter;
			    		best_park = park_option;
			    		best_size = size;
			    		//if(size != 4 && size != 0) System.out.println("hi");
			    	}
			    }

			    best_move.park = best_park;
			    markedForConstruction.addAll(best_move.park);
			    
			    best_perimeter = 100;
			    best_park = new HashSet<Cell>();
			    best_size = 100;
			    for(int i = 0; i < ITERATION_COUNT; ++i) {
			    	int perimeter = 0;
			    	int size = 0;
			    	Set<Cell> park_option = randomWalk(shiftedCells, markedForConstruction, land, 4, 2);
			    	size = park_option.size()>0?park_option.size():110;
			    	for(Cell x : park_option) {
			    		for(Cell y: x.neighbors()) {
			    			if(!land.unoccupied(y) /*&& !y.isRoad()*/) {
			    				++perimeter;
			    			}
			    		}
			    	}
			    	if(size < best_size || (size == best_size && perimeter < best_perimeter)) {
			    		best_perimeter = perimeter;
			    		best_park = park_option;
			    		best_size = size;
			    		//if(size != 4 && size != 0) System.out.println("hi");
			    	}
			    }
			    best_move.water = best_park;
			    prev_water = best_move.water;
			}
			/*
			if(num==0) {
	    		++num;
	    		Set<Cell> road_cell2 = new HashSet<Cell>();
	    		for(int i=0; i<15;++i) {
	    			road_cell2.add(new Cell(i,land.side/2));
	    			road_cell2.add(new Cell(land.side - 1 - i,land.side/2));
	    		}
	    		road_cells.addAll(road_cell2);
	    		best_move.road.addAll(road_cell2);
	    	}*/
			return best_move;
	    }
	    else {// reject placement if building cannot be connected by road
	    	//Kailash: This should never happen now.
	    	return new Move(false);    
	    }
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
