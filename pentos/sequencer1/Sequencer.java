package pentos.sequencer1;

import java.util.*;
import pentos.sim.Building;
import pentos.sim.Cell;

public class Sequencer implements pentos.sim.Sequencer {

    private Random gen;
    private int count;
    private final double ratio = 0.8; // ratio of residences to total number of buildings

    public void init(Long seed) {
		gen = new Random();
		count = 0;
    }
    
    public Building next() {
    	++ count;
    	//System.out.println(count);
		if (gen.nextDouble() < ratio)
		    return starsResidence();
		else {
			if (count <= 130)
		    	return randomSmallFactory();
		    else return largeFactory();
		}
    }

    private Building randomResidence() { // random walk of length 5
		Set<Cell> residence = new HashSet<Cell>();
		Cell tail = new Cell(0,0);
		residence.add(tail);
		for (int i=0; i<4; i++) {
		    ArrayList<Cell> walk_cells = new ArrayList<Cell>();
		    for (Cell p : tail.neighbors()) {
			if (!residence.contains(p))
			    walk_cells.add(p);
		    }
		    tail = walk_cells.get(gen.nextInt(walk_cells.size()));
		    residence.add(tail);
		}
		return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }    

    private Building starsResidence() {
		Set<Cell> residence = new HashSet<Cell>();
		residence.add(new Cell(1,1));
		residence.add(new Cell(0,1));
		residence.add(new Cell(1,0));
		residence.add(new Cell(2,1));
		residence.add(new Cell(1,2));
		return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }

    private Building randomSmallFactory() {
		Set<Cell> factory = new HashSet<Cell>();
		int width = 1;
		int height = gen.nextInt(2) + 1;
		for (int i=0; i<width; i++) {
		    for (int j=0; j<height; j++) {
				factory.add(new Cell(i,j));
		    }
		}
		return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }

    private Building largeFactory() { // random rectangle with side lengths biased towards 1 or 5
		Set<Cell> factory = new HashSet<Cell>();
		int width = 5;
		int height = 5;
		for (int i=0; i<width; i++) {
		    for (int j=0; j<height; j++) {
			factory.add(new Cell(i,j));
		    }
		}
		return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }    
    
}
