package pentos.cl0;

import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

public class Player implements pentos.sim.Player {
	private final int number_of_random_walks = 200;
	private final int side = 50;
	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();
	List<Cell> orderResidense, orderFactory;
	
	@Override
	public void init() {
		// TODO Auto-generated method stub
		orderResidense = new ArrayList<Cell>(side * side);
		orderFactory = new ArrayList<Cell>(side * side);
		for (int i = 0; i < side; ++ i)
			for (int j = 0; j < side; ++ j) {
				orderResidense.add(new Cell(i, j));
				orderFactory.add(new Cell(i, j));
			}
		Collections.sort(orderResidense, new ResidenseComparator());
		Collections.sort(orderFactory, new FactoryComparator());
	}

	@Override
	public Move play(Building request, Land land) {
		// TODO Auto-generated method stub
		List<Cell> order = (request.type == Building.Type.FACTORY) ? orderFactory : orderResidense;
		List<Move> candidates = new ArrayList<Move>();
		List<Integer> candidates_perimeter = new ArrayList<Integer>();
		List<Integer> candidates_changes = new ArrayList<Integer>();
		int bestPerimeter = 0;
		for (int i = 0; i < order.size(); ++ i) {
			Building[] rotations = request.rotations();
			for (int k = 0; k < rotations.length; ++ k) {
				if (land.buildable(rotations[k], order.get(i))) {
					Move move = new Move(true, request, order.get(i), k, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>());
					Set<Cell> shiftedCells = new HashSet<Cell>();
				    for (Cell x : move.request.rotations()[move.rotation])
				    	shiftedCells.add(new Cell(x.i+move.location.i,x.j+move.location.j));

					int perimeter = 0;
					for (Cell x : shiftedCells) {
						for (Cell y : x.neighbors())
							if (!land.unoccupied(y))
								++ perimeter;
						if(x.i == 0 || x.i == land.side - 1) ++ perimeter;
						if(x.j == 0 || x.j == land.side - 1) ++ perimeter;						
					}
					if (perimeter < bestPerimeter) continue;
					else {
						if (perimeter > bestPerimeter) {
							candidates.clear();
							candidates_perimeter.clear();
							candidates_changes.clear();
						}
						bestPerimeter = perimeter;
					}

					Set<Cell> perimeters = getPerimeters(shiftedCells);

					int num_road = 0, num_water = 0, num_park = 0;
					for (Cell cell : perimeters) {
						if (road_cells.contains(cell)) ++ num_road;
						else if (land.isField(cell)) ++ num_park;
						else if (land.isPond(cell)) ++ num_water;
					}

					if (num_road == 0) {
						Set<Cell> road_to_build = findShortestRoad(shiftedCells, land);
						if (road_to_build == null) continue;
						move.road = road_to_build;
					}
					
					if (request.type == Building.Type.RESIDENCE) { // for residences, build random ponds and fields connected to it
					    Set<Cell> markedForConstruction = new HashSet<Cell>();
					    markedForConstruction.addAll(move.road);
						if (num_water == 0) {
							//move.water = randomWalk(shiftedCells, markedForConstruction, land, 4, 2);
							int best_perimeter = 100;
							Set<Cell> best_water = new HashSet<Cell>();
							int best_size = 100;
							for(int t = 0; t < number_of_random_walks; ++ t) {
								int p = 0;
								int size = 0;
								Set<Cell> water_option = randomWalk(shiftedCells, markedForConstruction, land, 4, 2);
								size = water_option.size()>0?water_option.size():110;
								for(Cell x : water_option) {
									for(Cell y: x.neighbors()) {
										if(!land.unoccupied(y) /*&& !y.isRoad()*/) {
											++p;
										}
									}
								}
								if(size < best_size || (size == best_size && p < best_perimeter)) {
									best_perimeter = p;
									best_water = water_option;
									best_size = size;
									//if(size != 4 && size != 0) System.out.println("hi");
								}
							}
							move.water = best_water;

						}
					    markedForConstruction.addAll(move.water);
						if (num_park == 0) {
							//move.park = randomWalk(shiftedCells, markedForConstruction, land, 4, 1);
							Set<Cell> best_park = new HashSet<Cell>();
							int best_perimeter = 100;
							int best_size = 100;
							for(int t = 0; t < number_of_random_walks; ++ t) {
								int p = 0;
								int size = 0;
								Set<Cell> park_option = randomWalk(shiftedCells, markedForConstruction, land, 4, 1);
								size = park_option.size()>0?park_option.size():110;
								for(Cell x : park_option) {
									for(Cell y: x.neighbors()) {
										if(!land.unoccupied(y) /*&& !y.isRoad()*/) {
											++p;
										}
									}
								}
								if(size < best_size || (size == best_size && p < best_perimeter)) {
									best_perimeter = p;
									best_park = park_option;
									best_size = size;
									//if(size != 4 && size != 0) System.out.println("hi");
								}
							}

							move.park = best_park;
						}
					}
					shiftedCells.addAll(move.road);
					shiftedCells.addAll(move.water);
					shiftedCells.addAll(move.park);
					perimeter = 0;
					for (Cell x : shiftedCells) {
						for (Cell y : x.neighbors())
							if (!land.unoccupied(y))
								++ perimeter;
						if(x.i == 0 || x.i == land.side - 1) ++ perimeter;
						if(x.j == 0 || x.j == land.side - 1) ++ perimeter;						
					}
					candidates_perimeter.add(perimeter);
					candidates_changes.add(getChangesOfEmptySpaces(move, land));
					candidates.add(move);
				}
			}
			if (candidates.size() > 10) break;
		}
		if (candidates.isEmpty())
			return new Move(false);
		
		//Choose a candidate
		//return candidates.get(gen.nextInt(candidates.size()));
		int chosen = 0;
		for (int i = 1; i < candidates.size(); ++ i) {
			if (candidates_perimeter.get(i) > candidates_perimeter.get(chosen)
				|| (candidates_perimeter.get(i) == candidates_perimeter.get(chosen)
					&& candidates_changes.get(i) < candidates_changes.get(chosen)))
				chosen = i;
		}
		road_cells.addAll(candidates.get(chosen).road);
		return candidates.get(chosen);
	}

	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();
		// add border cells that don't have a road currently
		Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
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
				checked[q.i][q.j] = true;
		    }
		}	
		while (!queue.isEmpty()) {
		    Cell p = queue.remove();
		    //checked[p.i][p.j] = true;
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


	private static class ResidenseComparator implements Comparator<Cell> {
		@Override
		public int compare(Cell o1, Cell o2) {
			if (o1.i + o1.j == o2.i + o2.j)
				return Integer.compare(Math.abs(o1.i - o1.j), Math.abs(o2.i - o2.j));
			else return Integer.compare(o1.i + o1.j, o2.i + o2.j);
		}
	}
	
	private static class FactoryComparator implements Comparator<Cell> {
		@Override
		public int compare(Cell o1, Cell o2) {
			if (o1.i + o1.j == o2.i + o2.j)
				return Integer.compare(Math.abs(o1.i - o1.j), Math.abs(o2.i - o2.j));
			else return -Integer.compare(o1.i + o1.j, o2.i + o2.j);
		}
	}

	private Set<Cell> getPerimeters(Set<Cell> cells) {
		Set<Cell> ret = new HashSet<Cell> ();
		for (Cell cell : cells) {
			for (Cell p : cell.neighbors())
				ret.add(p);
		}
		return ret;
	}

	private int getChangesOfEmptySpaces(Move move, Land land) {
		boolean[][] vis = new boolean[side][side];
		int ret = -1;	// If no empty cells nearby then it fills an empty hole
		
		Building[] rotations = move.request.rotations();
		Set<Cell> builds = new HashSet<Cell>();
		for (Cell cell : rotations[move.rotation])
			builds.add(new Cell(cell.i + move.location.i, cell.j + move.location.j));
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


}
