% Author: Saad Attieh

include "alldifferent.mzn";

int : numberLocations;
array[1..numberLocations,1..numberLocations] of int: distances;
int : maxDistance = max([distances[i,j] | i,j in 1..numberLocations]);
array[1..numberLocations] of var 1..numberLocations: tour;
var 0..numberLocations*maxDistance  : optVar;
constraint alldifferent(tour);
constraint optVar = sum([distances[tour[i-1],tour[i]] | i in 2..numberLocations]) + distances[tour[numberLocations],tour[1]];
solve minimize optVar;
