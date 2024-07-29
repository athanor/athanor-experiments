#!/usr/bin/env bash

# usage: ./compile.sh

if [ "$#" -ne 0 ]; then
    echo "usage: ./compile.sh"
    exit 1
fi

#  For both solver types.
for chocoType in expbased propguided
do

CURDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
if [ "$chocoType" == "expbased" ]; then
    CHOCO_LIB="$CURDIR/chocoLib/choco-solver-4.0.9.jar"
elif [ "$chocoType" == "propguided" ]; then
    CHOCO_LIB="$CURDIR/chocoLib/choco-solver-4.10.10-jar-with-dependencies.jar"
else
    echo "incorrect Choco type"
    exit 1
fi

for prob in TSP KNAPSACK
do
    echo "BUILDING " $chocoType $prob
    echo "================================================================================"
    out="${prob}-$chocoType"
    mkdir -p $out
    pushd $out
    cp ../$prob.java ../LNSSolve.java ./
    echo "import org.chocosolver.solver.variables.*;" > ChocoConfig.java
    echo "import org.chocosolver.solver.search.loop.lns.*;" >> ChocoConfig.java
    echo "import org.chocosolver.solver.search.loop.lns.neighbors.*;" >> ChocoConfig.java
    echo "import org.chocosolver.solver.search.loop.move.*;" >> ChocoConfig.java
    echo "import org.chocosolver.solver.search.strategy.strategy.*;" >> ChocoConfig.java
    echo "import org.chocosolver.solver.search.strategy.selectors.values.*;" >> ChocoConfig.java
    echo "import org.chocosolver.solver.search.strategy.selectors.variables.*;" >> ChocoConfig.java
    
    echo "public class ChocoConfig { " >> ChocoConfig.java
    echo "public static INeighbor build(IntVar[] decvars) {" >> ChocoConfig.java
    if [ "$chocoType" == "propguided" ]; then
        echo "return INeighborFactory.propagationGuided(decvars);" >> ChocoConfig.java
    elif [ "$chocoType" == "expbased" ]; then
        echo "return INeighborFactory.explanationBased(decvars);" >> ChocoConfig.java
    else
        echo "Invalid argument " $chocoType
        exit 1
    fi
    echo "}" >> ChocoConfig.java
    
    #  Make the Move object for finding the initial solution
    echo "public static Move makeInitMove(IntVar[] decvars, int seed) {" >> ChocoConfig.java
    if [ "$chocoType" == "propguided" ]; then
        echo "return new MoveBinaryDFS(new IntStrategy(decvars, new DomOverWDeg(decvars, seed), new IntDomainRandom(seed))); }" >> ChocoConfig.java
    elif [ "$chocoType" == "expbased" ]; then
        echo "return new MoveBinaryDFS(new DomOverWDeg(decvars, seed, new IntDomainRandom(seed))); }" >> ChocoConfig.java
    else
        echo "Invalid argument " $chocoType
        exit 1
    fi
    
    #  Make the Move object for using within LNS
    echo "public static Move makeLNSMove(IntVar[] decvars, int seed) {" >> ChocoConfig.java
    if [ "$chocoType" == "propguided" ]; then
        echo "return new MoveBinaryDFS(new IntStrategy(decvars, new DomOverWDeg(decvars, seed), new IntDomainMin())); }" >> ChocoConfig.java
    elif [ "$chocoType" == "expbased" ]; then
        echo "return new MoveBinaryDFS(new DomOverWDeg(decvars, seed, new IntDomainMin())); }" >> ChocoConfig.java
    else
        echo "Invalid argument " $chocoType
        exit 1
    fi
    
    echo "}" >> ChocoConfig.java   #  End of class.
    javac -cp ${CHOCO_LIB} $prob.java ChocoConfig.java LNSSolve.java
    mFile="manifest-$prob.txt"
    echo "Main-Class: $prob" > ${mFile} 
    echo "Class-Path: ${CHOCO_LIB}" >>${mFile}
    jar cvfm $prob.jar ${mFile} $prob.class ChocoConfig.class LNSSolve.class
    mv $prob.jar ../$out.jar
    popd
    rm -rf $out
    echo "================================================================================"
done
done
exit 0
