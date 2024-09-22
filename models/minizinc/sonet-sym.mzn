include "globals.mzn";


int: nnodes;    %  n
int: nrings;    %  m
int: capacity;  %  r

var int: optVar;

array[1..nnodes, 1..nnodes] of int: demand;

array[1..nrings]  of var set of 1..nnodes: network;

constraint forall(i,j in 1..nnodes where i<j)( demand[i,j]=1 -> exists(k in 1..nrings)({i,j} subset network[k]));

constraint forall(i in 1..nrings)(card(network[i])<=capacity);

constraint forall(i in 1..nrings)(card(network[i])!=1);

constraint optVar=sum([ card(network[i]) | i in 1..nrings ]);

solve minimize optVar;

% Sym breaking
constraint forall(i in 1..nrings-1)(lex_lesseq([(j in network[i+1]) | j in 1..nnodes],[(j in network[i]) | j in 1..nnodes]));