**Athanor** is a general-purpose local search solver for solving combinatorial (optimisation) problems. Athanor begins from a specification of a problem written in Essence, a high-level constraint modelling language that allows combinatorial problems to be described without commitment to low-level modelling decisions through its support for a rich set of abstract types (such as sets, multisets, partitions, sequences, etc), each of which can be *nested arbitrarily*. 
Based on the twin benefits of neighbourhoods derived from high level types and the scalability derived by searching directly over those types, our empirical results demonstrate strong performance in practice relative to existing solution methods.

This folder contains all data used for our experiments in the paper:

Attieh, S., Dang, N., Jefferson, C., Miguel, I. and Nightingale, P. (2024) **Athanor: local search over abstract constraint specifications** (submitted to [AIJ](https://www.sciencedirect.com/journal/artificial-intelligence))

We compare [Athanor](https://github.com/athanor/athanor) with six other solvers:

- [Choco-LNS](https://choco-solver.org/): 
	+ LNS-PG: propagation-guided Large Neighbourhood Search, Choco version 4.10.1
	+ LNS-EB: explanation-based Large Neighbourhood Search, Choco version 4.0.9
- [yuck](https://github.com/informarte/yuck), version dated November 1st, 2022
- [Oscar-CBLS](https://user.it.uu.se/~gusbj192/fzn-oscar-cbls/latest/), version dated August 8th, 2021
- [chuffed](https://github.com/chuffed/chuffed), version 0.10.4
- [OR-Tools](https://github.com/google/or-tools), version 9.4.1874
- [SNS]()

The comparison is conducted on seven combinatorial optimisation problems:

- [Bin Packing](http://people.brunel.ac.uk/~mastjjb/jeb/orlib/binpackinfo.html)
- [Travelling Salesperson Problem (TSP)](http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/)
- [Capacitated Vehicle Routing Problem (CVRP)](http://www.vrp-rep.org/)
- [Knapsack](http://www.diku.dk/~pisinger/hardinstances_pisinger.tgz)
- [Minimum Energy Broadcast (MEB)](https://www.csplib.org/Problems/prob048/)
- [Progressive Party Problem (PPP)](https://www.csplib.org/Problems/prob013/)
- [Synchronous Optical Networking (SONET)](https://www.csplib.org/Problems/prob056/)

Folder structure:
- `models/`: problem specifications (i.e., constraint models) of the seven combinatorial problems described above
	+ `models/essence/*.essence`: models written in Essence, used by Athanor. Those models are used for the experiments in Sections 9 and Section 10.
	+ `models/minizinc/*.mzn`: models written in MiniZinc, used by  Yuck, Oscar-CBLS, Chuffed and OR-Tools. Those models are used for the experiments in Sections 9.
	+ `models/minizinc-extra/*.mzn`: models written in MiniZinc with _specialised_ global constraints (knapsack for Knapsack, bin_packing_load for Bin Packing, circuit for TSP and CVRP). Those models are used for the experiments in Section 10.
	+ `models/choco-lns/*.java`: models for the two Choco-LNS solvers (LNS-PG and LNS-EB). Those models are used for the experiments in Sections 9.
	+ `models/choco-lns-extra/*.java`: models for the two Choco-LNS solvers (LNS-PG and LNS-EB) with _specialised_ global constraints (knapsack for Knapsack, binPacking for Bin Packing, subCircuit for TSP and CVRP). Those models are used for the experiments in Section 10.
	+ `models/sns/*.eprime`: models used by SNS (neighbourhoods for each problem were already generated and encoded in the models). Those models are used for the experiments in Sections 9 and Section 10.

- `instances/`: problem instances of the seven combinatorial problems described above
	+ For each problem, there are two sub-folders: 
		* `normal-size`: instances used in the first set of experiments (to study the effectiveness of each solver).
		* `large-size`: instances used in the second set of experiments (to study the scalability of each solver)
	+ The instances provided are in different formats:
		* `*.param`: instances in Essence format, used by Athanor and SNS.
		* `*.dzn`: instances in MiniZinc format, used by Yuck, Oscar-CBLS, Chuffed and OR-Tools.
		* `*.lns`: instances used by Choco-LNS solvers.

- `results/`: all experiment results used in our paper.
	+ `results-normal-size.zip` and `results-large-size.zip`: experiment results in Section 9 (with normal problem sizes and large sizes for scalability)
	+ `results-normal-size-gc.zip` and `results-large-size-gc.zip`: experiment results in Section 10 (with normal problem sizes and large sizes for scalability)
 	+ `run-status.csv`: status of each run, as shown in figure 13 and figure 16 in the text.


