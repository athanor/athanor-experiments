% Adopted from: https://github.com/hakank/hakank/blob/master/minizinc/bin_packing_me.mzn (a minizinc model for bin packing by Hakan Kjellerstrand)
% Modified to include bin_packing_load global constraint
% We use bin_packing_load instead of bin_packing so that the symmetry breaking constraints in the original model can be re-used.

include "globals.mzn";
include "bin_packing_load.mzn";

int: num_stuff;                    % number of things to pack
array[1..num_stuff] of int: stuff; % the value/weight of the things to pack
int: bin_capacity;                 % all bins have the same capacity

% Number of bins cannot exceed num_stuff...
int: num_bins; % num_stuff;

%
% decision variables
%
% bins[i]: the bin where item i is assigned to
array[1..num_stuff] of var 1..num_bins: bins;

% calculate how many things a bin takes
array[1..num_bins] of var 0..bin_capacity: bin_loads;

% number of loaded bins (which we will minimize)
var 0..num_bins: optVar;

% minimize the number of loaded bins
solve minimize optVar;

constraint

    bin_packing_load(bin_loads, bins, stuff)

    /\ % symmetry breaking: 
    %    % if bin_loads[i+1] is > 0 then bin_loads[i] must be > 0
    %  forall(b in 1..num_bins-1) (
    %    (bin_loads[b+1] > 0 -> bin_loads[b] > 0)
    %    % /\ % and should be filled in order of weight
    %    % bin_loads[b] >= bin_loads[b+1] 
    %  ) 
    %/\
    decreasing(bin_loads) :: domain
    /\ % another symmetry breaking: first bin must be loaded
    bin_loads[1] > 0 

    /\ % calculate optVar
    optVar = sum(b in 1..num_bins) (bool2int(bin_loads[b] > 0))

;
