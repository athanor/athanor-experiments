
include "alldifferent_except_0.mzn";
include "alldifferent.mzn";
include "globals.mzn";

int : numberLocations;
%costs
%0 is depot
array[0..numberLocations,0..numberLocations] of int: costs;
array[1..numberLocations] of int: orderWeights; 
array[0..numberLocations] of int: orderWts = array1d(0..numberLocations, [0]++orderWeights);
int: vehicleCapacity;

array[1..numberLocations] of var 0..numberLocations: sequenceSizes;
array[1..numberLocations,0..numberLocations+1] of var 0..numberLocations: plan;   % longest is a sequence of all orders, with 0 at both ends. 
%first of every array is 0 (depot).  Fill unused parts of plan with 0.
constraint forall (i in 1..numberLocations)(plan[i,0] = 0 /\ plan[i,numberLocations+1]=0);

constraint forall (i,j in 1..numberLocations)(
    (sequenceSizes[i]<j) <-> (plan[i,j] = 0)
);

constraint alldifferent_except_0([plan[i,j] | i,j in 1..numberLocations]);

constraint numberLocations = sum([sequenceSizes[i] | i in 1..numberLocations]);

int: minVehicles = (sum(orderWeights) div vehicleCapacity) + (sum(orderWeights) mod vehicleCapacity != 0);
constraint forall(i in 1..minVehicles) (
    sequenceSizes[i]>0);

%capacity constraints
constraint forall(i in 1..numberLocations)(
    vehicleCapacity >= sum([orderWts[plan[i,j]] | j in 1..numberLocations]));

int: maxTotalCost = sum([costs[0,i] | i in 1..numberLocations]) * 2;
var 0..maxTotalCost: optVar;

constraint optVar = sum([costs[plan[i,j],plan[i,j+1]] | i in 1..numberLocations, j in 0..numberLocations]);

constraint forall(i in 1..numberLocations-1)(
  plan[i+1,1]<=plan[i,1]
);

solve :: int_search(plan, input_order, indomain_max) minimize optVar;
