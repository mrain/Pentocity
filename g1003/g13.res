Round 1:
	Player g1003 scored 2077
	Number of empty cells: 189
	Number of road cells: 402
	Utility of water/park cells: 1.4329896907216495
	Length of perimeter: 396
	Number of extendable cells: 99
	Largest empty area: 25, perimeter: 42, extendable empty cells: 6
Round 2:
	Player g1003 scored 2102
	Number of empty cells: 134
	Number of road cells: 414
	Utility of water/park cells: 1.3658536585365855
	Length of perimeter: 286
	Number of extendable cells: 81
	Largest empty area: 16, perimeter: 24, extendable empty cells: 5
Exception during play: Land not empty. Contains WATER
java.lang.RuntimeException: Land not empty. Contains WATER
	at pentos.sim.Cell.buildType(Cell.java:89)
	at pentos.sim.Cell.buildPark(Cell.java:83)
	at pentos.sim.Land.buildPark(Land.java:93)
	at pentos.sim.Simulator.play(Simulator.java:277)
	at pentos.sim.Simulator.main(Simulator.java:108)
Exiting the simulator ...
make: *** [Makefile:14: run] Error 1
