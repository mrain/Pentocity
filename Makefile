all: compile

compile:
	javac pentos/sim/*.java

gui:
	java pentos.sim.Simulator --gui -g ${g}

run:
	java pentos.sim.Simulator -g ${g} --repeats ${r} -s ${s}
