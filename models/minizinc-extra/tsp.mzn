% The original model was created by Hakan Kjellerstrand, hakank@gamil.com with help from Krzysztof Kuchcinski (of the JaCoP team)
% Link to the original model: https://github.com/hakank/hakank/blob/master/minizinc/tsp_circuit.mzn

include "globals.mzn";
int: numberLocations; % number of nodes

array[1..numberLocations] of var 1..numberLocations: tour;             % the circuit
array[1..numberLocations, 1..numberLocations] of int: distances;  % distance matrix
int: min_val = min([distances[i,j] | i,j in 1..numberLocations where distances[i,j] > 0]);
int: max_val = max([distances[i,j] | i,j in 1..numberLocations]);
array[1..numberLocations] of var min_val..max_val: d;
%var int: optVar = sum(d);

var 0..numberLocations*max_val  : optVar;
solve minimize optVar;

constraint
    optVar = sum(d) /\
    all_different(tour) /\ % this speeds up for some solvers

    circuit(tour) % use this for solvers with built-in circuit predicate, e.g. JaCoP
               % or - lately - the built-in version in MiniZinc distribution

    /\
    forall(i in 1..numberLocations) (
      distances[i,tour[i]] = d[i]
    )
;
