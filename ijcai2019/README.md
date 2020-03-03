This folder contains all data used for our experiments in the IJCAI2019 paper:
Attieh, S., Dang, N., Jefferson, C., Miguel, I. and Nightingale, P., 2019. Athanor: high-level local search over abstract constraint specifications in Essence. [link](https://www.ijcai.org/Proceedings/2019/148)

The folder includes models and instances for six problem classes: CVRP, TSP, MEB, Knapsack, SONET and SocialGolfers. The first five are optimisation problems, while the last one is decision problem.

- `Choco-LNS`:  Choco-LNS models and instances

- `Essence`: Essence models and param (instance) files
    + `Essence/models/*.essence`: Essence models. These are used directly by Athanor.
    + `Essence/models/eprime-models`: Essence Prime models generated from the Essence models through *conjure*. These are used for translating Essence instance files into `flatzinc` input format required by *oscar_cbls*, *yuck*, and *chuffed*. For each problem classes, there are three Essence Prime models: one with symmetry-breaking constraints enabled (`<problem>.eprime`), one without symmetry-breaking constraints (`<problem>-nosym.eprime`), and one with neighbourhoods encoded inside (`<problem>-sns.eprime`). The last one is for *sns* only (see our (IJCAI2018 paper)[https://www.ijcai.org/Proceedings/2018/173] on sns)
    + `Essence/models/instances`: Essence param files
    + To convert an Essence param file into `flatzinc`, we use the following commands:
    ```
        conjure translate-parameter --eprime <problem>.eprime --essence-param <instance>.param --eprime-param <instance>.eprime-param
        savilerow <problem>.eprime <instance>.eprime-param -minizinc -out-minizinc <instance>.mzn
        minizinc -c --no-output-ozn <instance>.mzn -o <instance>.fzn -G <solver>
    ```
    For example, to convert the `Essence/cvrp/eil13.param` to `flatzinc` format for *yuck*:
    ```
        cd Essence
        conjure translate-parameter --eprime models/eprime-models/cvrp.eprime --essence-param instances/cvrp/eil13.param --eprime-param eil13.eprime-param
        savilerow models/eprime-models/cvrp.eprime eil13.eprime-param -minizinc -out-minizinc eil13.mzn
        minizinc -c --no-output-ozn eil13.mzn -o eil13.fzn -G yuck
    ```
