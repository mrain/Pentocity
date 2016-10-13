package pentos.g3;

import java.util.*;
import pentos.sim.Building;
import pentos.sim.Cell;

public class Sequencer implements pentos.sim.Sequencer {

    private Random gen;
    private double ratio = 0.9; // ratio of residences to total number of buildings
    private int flag = 0;

    public void init(){
    	flag++;
    }

    public void init(Long seed) {
	if (seed != null) 
	    gen = new Random(seed.longValue());
	else
	    gen = new Random();
    }
    
    public Building next() {
	
	    return randomResidence();
    }

    private static final int[][][] resShapes = {
    	{{0,0}, {1,0}, {1,1}, {1,2}, {2,2}}, 
    	{{1,0}, {1,1}, {1,2}, {0,1}, {0,2}}, 
    	{{1,0}, {1,1}, {1,2}, {1,3}, {2,3}}, 
    	{{2,0}, {1,0}, {1,1}, {1,2}, {0,2}}, 
    	{{0,0}, {0,1}, {0,2}, {0,3}, {1,3}}, 
    	{{1,0}, {1,1}, {1,2}, {0,1}, {2,1}}};

    private Building randomResidence() { // random walk of length 5
    	Set<Cell> residence = new HashSet<Cell>();

    for(int[] shape : resShapes[flag]) {
            residence.add(new Cell(shape[0], shape[1]));
        }
    flag++;
    if (flag == 6){
    	flag = 0;
    }

	return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }    

    private Building randomFactory() { // random rectangle
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
