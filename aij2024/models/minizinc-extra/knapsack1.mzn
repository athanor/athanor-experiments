% Author: Peter Nightingale

include "knapsack.mzn";

int: nitems;    % Number of items

array[1..nitems] of int: gain;
array[1..nitems] of int: weight;

int: capacity;

array[1..nitems] of var 0..1: knapsack;

var 0..capacity: totalWeight;

var int: optVar;

constraint knapsack(weight, gain, knapsack, totalWeight, optVar);

solve maximize optVar;
