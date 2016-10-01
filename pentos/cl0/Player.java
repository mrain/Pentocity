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
	private final int size = 50;
	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();
	List<Cell> orderResidense, orderFactory;
	
	@Override
	public void init() {
		// TODO Auto-generated method stub
		orderResidense = new ArrayList<Cell>(size * size);
		orderFactory = new ArrayList<Cell>(size * size);
		for (int i = 0; i < size; ++ i)
			for (int j = 0; j < size; ++ j) {
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
		for (int i = 0; i < order.size(); ++ i) {
			Building[] rotations = request.rotations();
			for (int k = 0; k < rotations.length; ++ k) {
				if (land.buildable(rotations[k], order.get(i))) {
					Move move = new Move(true, request, order.get(i), k, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>());
					Set<Cell> shiftedCells = new HashSet<Cell>();
				    for (Cell x : move.request.rotations()[move.rotation])
				    	shiftedCells.add(new Cell(x.i+move.location.i,x.j+move.location.j));
					Set<Cell> road_to_build = findShortestRoad(shiftedCells, land);
					if (road_to_build == null) continue;
					move.road = road_to_build;
					//road_cells.addAll(road_to_build);
					
					if (request.type == Building.Type.RESIDENCE) { // for residences, build random ponds and fields connected to it
					    Set<Cell> markedForConstruction = new HashSet<Cell>();
					    markedForConstruction.addAll(road_to_build);
					    move.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
					    markedForConstruction.addAll(move.water);
					    move.park = randomWalk(shiftedCells, markedForConstruction, land, 4);
					}
					//return ret;
					//return move;
					candidates.add(move);
				}
			}
			if (candidates.size() > 6) break;
		}
		if (candidates.isEmpty())
			return new Move(false);
		
		//Choose a candidate
		//return candidates.get(gen.nextInt(candidates.size()));
		Move chosen = candidates.get(0);
		road_cells.addAll(chosen.road);
		return chosen;
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
}
