% This model is 'inspired by' the model in 
% https://link.springer.com/chapter/10.1007/978-3-642-40627-0_34 ,
%  with some variables removed (they are reintroduced)
% param:
% 
% n_boats=4;
% n_periods=2;
% capacity=[4,4,4,4];
% crew=[1,2,1,2];

include "alldifferent.mzn";
include "globals.mzn";

int: n_boats;
int: n_periods;

set of int : Boat = 1..n_boats;
set of int : Time = 1..n_periods;

array [Boat] of int : capacity;
array [Boat] of int : crew;

var set of Boat : hosts;
array [Boat, Time] of var Boat : visit;

% hosts stay in the same place
constraint forall(b in Boat, t in Time) ((b in hosts) <-> visit[b,t]=b);

% hosts remain the same
constraint forall(b in Boat, t in Time) (visit[b,t] in hosts);

% boats don't exceed capacity
constraint forall(b in hosts, t in Time) (
(sum(b2 in Boat)(bool2int(visit[b2,t]=b)*crew[b2]) <= capacity[b]));

%No two crews are at the same party more than once
constraint forall (k,l in Boat where k<l)
( sum(t in Time)(bool2int(visit[k,t]=visit[l,t])) <= 1);

%%%% SYMMETRY BREAKING %%%
constraint forall(i in 1..n_periods-1)(lex_lesseq(visit[..,i], visit[..,i+1]));

var int : optVar;
constraint optVar=card(hosts);

solve minimize optVar;
