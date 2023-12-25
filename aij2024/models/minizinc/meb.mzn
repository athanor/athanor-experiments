include "globals.mzn";

int: numberNodes;
int: initialNode;

set of int: dNodes = 1..numberNodes;

set of int: dDepths=dNodes;

% if a node has power link of 0 to another node, that link is not possible
array[dNodes,dNodes] of int: linkCosts;

array[dNodes] of var dNodes: parents;
array[dNodes] of var dDepths: depths;

var int: optVar;

constraint parents[initialNode]=initialNode;

constraint forall(child in dNodes)(
  (child!=initialNode) -> 
    ( parents[child] != child /\ linkCosts[parents[child], child]!=0) );

constraint forall(child in dNodes)(
  (child!=initialNode) ->
    (depths[child] = depths[parents[child]]+1)
);

constraint optVar=sum(parent in dNodes)(max( [linkCosts[parent, child]*bool2int( parents[child] = parent ) | child in dNodes ] ) );

solve minimize optVar;
