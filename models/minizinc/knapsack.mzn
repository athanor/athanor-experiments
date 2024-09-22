% Author: Peter Nightingale

int: nitems;    % Number of items

array[1..nitems] of int: gain;
array[1..nitems] of int: weight;

int: capacity;

var set of 1..nitems: knapsack;

var int: optVar;

constraint sum([ weight[i] | i in knapsack ])<=capacity;

constraint optVar=sum([ gain[i]  | i in knapsack ]);

solve maximize optVar;
