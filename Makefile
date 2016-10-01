r=1
g=g0
s=random

all: compile

compile:
	javac pentos/sim/*.java

gui:
	java pentos.sim.Simulator --gui -s ${s} -g ${g}

run:
	java pentos.sim.Simulator -s ${s} -g ${g} --repeats ${r}

