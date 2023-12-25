# generate Essence Pprime model with sns neighbourhods from an Essence model
# example command line: ./generate-eprime-model.sh meb
# IMPORTANT: the correct conjure branches must be used. Please change the paths to your corresponding conjure binary.

problem=$1

for problem in cvrp tsp sonet meb knapsack socialGolfers
do
    # eprime model for sns: use conjure sns branch (Repository version 02eca2151 (2018-05-31 17:01:00 +1000))
    rm -rf conjure-output
    ~/.local/bin/conjure-sns/conjure -ac --generate-neighbourhoods --frameUpdate decomposition ../essence/${problem}.essence
    cp conjure-output/model000001.eprime ./${problem}-sns.eprime


done
