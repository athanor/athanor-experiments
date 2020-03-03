# generate eprime model with sns neighbourhods from essence model
# example command line: ./generate-eprime-model.sh meb
# IMPORTANT: the correct conjure branches must be used. Please change the paths to your conjure binaries

problem=$1

for problem in cvrp tsp sonet meb knapsack socialGolfers
do
    # eprime model with symmetry breaking: use conjure master branch
    rm -rf conjure-output
    ~/.local/bin/conjure-master/conjure modelling ${problem}.essence -ac
    cp conjure-output/model000001.eprime ./${problem}.eprime

    # eprime model without symmetry breaking: use conjure sns branch
    rm -rf conjure-output
    ~/.local/bin/conjure-sns/conjure -ac ${problem}.essence
    cp conjure-output/model000001.eprime ./${problem}-nosym.eprime
    
    # eprime model for sns: use conjure sns branch
    rm -rf conjure-output
    ~/.local/bin/conjure-sns/conjure -ac --generate-neighbourhoods --frameUpdate decomposition ${problem}.essence
    cp conjure-output/model000001.eprime ./${problem}-sns.eprime


done
