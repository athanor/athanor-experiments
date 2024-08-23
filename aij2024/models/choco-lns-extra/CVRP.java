
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import static org.chocosolver.solver.search.strategy.Search.*;

import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.lns.*;
import org.chocosolver.solver.search.loop.lns.neighbors.*;
import org.chocosolver.solver.search.limits.*;
import org.chocosolver.solver.search.loop.move.*;
import org.chocosolver.solver.search.strategy.selectors.values.*;
import org.chocosolver.solver.search.strategy.selectors.variables.*;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.exception.*;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.*;

/**
 * Capacitated Vehicle Routing Problem
 */

// Command syntax: java -jar CVRP.jar <instance> <seed> <backtrackLimit> [<timeLimit_in_second>] [symbreak]
// Example: java -jar CVRP.jar instance.lns 123 280 600

public class CVRP {
    //25+20+30+20+ 20+20 + 10+10 + 25+25
public static void main(String [] args) {
    try{
   
    int seed=Integer.valueOf(args[1]);
    int btlimit=Integer.valueOf(args[2]);
 
    int timeLimit = 600; //NGUYEN: add this
    if (args.length>3)
        timeLimit = Integer.valueOf(args[3]); //NGUYEN: add this

    int useInitialisationRestart = 1;
    int useTimeForInitialisation = 0; //if 0, use BacktrackCounter instead of TimeCounter
    int initialisationBacktrackCounterValue = 50; // other option: btlimit

    int numberLocations;
    int vehicleCapacity;
    int[] orderWeights;
    int[][] costs;

    String inputFileName = args[0];
    BufferedReader br = new BufferedReader(new FileReader(inputFileName));
    String ln = br.readLine();
    String[] ls = ln.split(" ");
    numberLocations = Integer.parseInt(ls[0]);
    vehicleCapacity = Integer.parseInt(ls[1]);
    orderWeights = new int[numberLocations];
    ln = br.readLine();
    ls = ln.split(" ");
    for (int i=0; i<numberLocations; i++)
        orderWeights[i] = Integer.parseInt(ls[i]);        
    int nNodes = numberLocations + 1;
    costs = new int[nNodes][nNodes];
    for (int i=0; i<nNodes; i++){
        ln = br.readLine();
        ls = ln.split(" ");
        for (int j=0;j<nNodes;j++) {
            costs[i][j] = Integer.parseInt(ls[j]);
        }
    }
    
    int maxLinkCost=0;
    for(int i=0; i<=numberLocations; i++) {
        for(int j=0; j<=numberLocations; j++) {
            if(costs[i][j]>maxLinkCost) maxLinkCost=costs[i][j];
        }
    }
    //System.out.println("Max link cost: "+maxLinkCost);
    
    // Cost of putting each item onto its own vehicle and driving it to its target and back
    int maxTotalCost = 0;
    for(int i=1; i<=numberLocations; i++) {
        maxTotalCost += (2*costs[0][i]);
    }
    
    Model model = new Model("CVRP");
    
    int nbCustomers=numberLocations;
    int nbVehicles=numberLocations;
    
    //  NODES in MZN model is 1..nodes_ub
    int nodes_ub=nbCustomers+2*nbVehicles;
    
    int[] demand=new int[nodes_ub];
    //  only demand is for customer locations, not depot values. 
    for(int i=1; i<=numberLocations; i++) {
        demand[i-1]=orderWeights[i-1];
    }
    
    //  Indexed from 0 unlike the Mzn model.
    int[][] distance=new int[nodes_ub][nodes_ub];
    for(int i=1; i<=nodes_ub; i++) {
        for(int j=1; j<=nodes_ub; j++) {
            if(i<=nbCustomers && j<=nbCustomers) {
                distance[i-1][j-1]=costs[i][j];
            }
            else if(i<=nbCustomers && j>nbCustomers) {  // depot-customer
                distance[i-1][j-1]=costs[0][i];
            }
            else if(j<=nbCustomers && i>nbCustomers) { // customer-depot
                distance[i-1][j-1]=costs[j][0];
            }
            else {
                distance[i-1][j-1]=costs[0][0];  //  depot-depot
            }
        }
    }
    
    int distance_ub=0;
    for(int i=1; i<=nodes_ub; i++) {
        for(int j=1; j<=nodes_ub; j++) {
            if(distance[i-1][j-1]>distance_ub) {
                distance_ub=distance[i-1][j-1];
            }
        }
    }
    
    int timeBudget = 0;
    
    for(int i=1; i<=numberLocations; i++) {
        int maxcost=0;
        for(int j=1; j<numberLocations; j++) {
            if(costs[i][j]>maxcost) {
                maxcost=costs[i][j];
            }
        }
        timeBudget+=maxcost;
    }
    
    {
        int maxcost=0;
        for(int j=1; j<numberLocations; j++) {
            if(costs[0][j]>maxcost) {
                maxcost=costs[0][j];
            }
        }
        timeBudget+=maxcost;
    }
    
    {
        int maxcost=0;
        for(int j=1; j<numberLocations; j++) {
            if(costs[j][0]>maxcost) {
                maxcost=costs[j][0];
            }
        }
        timeBudget+=maxcost;
    }
    
    //  VARIABLES
    
    IntVar[] successor = model.intVarArray("successor", nodes_ub, 1, nodes_ub);
    IntVar[] predecessor = model.intVarArray("predecessor", nodes_ub, 1, nodes_ub);
    IntVar[] vehicle = model.intVarArray("vehicle", nodes_ub, 1, nbVehicles);
    IntVar[] load = model.intVarArray("load", nodes_ub, 0, vehicleCapacity);
    IntVar[] arrivalTime = model.intVarArray("arrivalTime", nodes_ub, 0, timeBudget);
    
    IntVar optVar = model.intVar("optVar", 0, maxTotalCost);
    
    ArrayList<IntVar> decvarsal=new ArrayList<IntVar>();
    decvarsal.addAll(Arrays.asList(successor));
    decvarsal.addAll(Arrays.asList(predecessor));
    decvarsal.addAll(Arrays.asList(vehicle));
    decvarsal.addAll(Arrays.asList(load));
    decvarsal.addAll(Arrays.asList(arrivalTime));
    decvarsal.add(optVar);
    
    ArrayList<IntVar> lnsvarsal=new ArrayList<IntVar>();
    lnsvarsal.addAll(Arrays.asList(successor));
    lnsvarsal.addAll(Arrays.asList(predecessor));
    lnsvarsal.addAll(Arrays.asList(vehicle));
    lnsvarsal.addAll(Arrays.asList(load));
    lnsvarsal.addAll(Arrays.asList(arrivalTime));
    
    //  CONSTRAINTS
    for(int n=nbCustomers+2; n<=nbCustomers+nbVehicles; n++) {
        model.arithm(predecessor[n-1], "=", n+nbVehicles-1).post();
    }
    
    model.arithm(predecessor[nbCustomers+1-1], "=", nbCustomers+2*nbVehicles).post();
    
    // successors of end nodes are start nodes
    for(int n=nbCustomers+nbVehicles+1; n<=nbCustomers+2*nbVehicles-1; n++) {
        model.arithm(successor[n-1], "=", n-nbVehicles+1).post();
    }
    
    model.arithm(successor[(nbCustomers+2*nbVehicles)-1], "=", nbCustomers+1).post();
    
    // associate each start/end nodes with a vehicle
    for(int n=nbCustomers+1; n<=nbCustomers+nbVehicles; n++) {
        model.arithm(vehicle[n-1], "=", n-nbCustomers).post();
    }
    for(int n=nbCustomers+nbVehicles+1; n<=nbCustomers+2*nbVehicles; n++) {
        model.arithm(vehicle[n-1], "=", n-nbCustomers-nbVehicles).post();
    }
    
    // vehicles leave the depot at time zero
    for(int n=nbCustomers+1; n<=nbCustomers+nbVehicles; n++) {
        model.arithm(arrivalTime[n-1], "=", 0).post();
    }
    
    // vehicle load when starting at the depot
    for(int n=nbCustomers+1; n<=nbCustomers+nbVehicles; n++) {
        model.arithm(load[n-1], "=", 0).post();
    }
    
    // ------- predecessor/successor constraints --- 
    for(int n=1; n<=nodes_ub; n++) {
        model.element(model.intVar(n), successor, predecessor[n-1], 1).post();  // Note offset of 1.
    }
    
    for(int n=1; n<=nodes_ub; n++) {
        model.element(model.intVar(n), predecessor, successor[n-1], 1).post();
    }
    
    // alldiff + subtour elimination constraints
    model.subCircuit(successor, 1, model.intVar(nodes_ub)).post();  // Note offset of 1
    
    model.subCircuit(predecessor, 1, model.intVar(nodes_ub)).post();  // Note offset of 1
    
    //---- vehicle constraints ------------- 
    
    // vehicle of node i is the same as the vehicle for the predecessor
    for(int n=1; n<=nbCustomers; n++) {
        model.element(vehicle[n-1], vehicle, predecessor[n-1], 1).post();
    }
    for(int n=1; n<=nbCustomers; n++) {
        model.element(vehicle[n-1], vehicle, successor[n-1], 1).post();
    }
    
    // ----- time constraints ------------ %
    IntVar[] distLookup1=model.intVarArray("distLookup1", nbCustomers+nbVehicles, 0, distance_ub);
    IntVar[] arrivalLookup1=model.intVarArray("arrivalLookup1", nbCustomers+nbVehicles, 0, timeBudget);
    decvarsal.addAll(Arrays.asList(distLookup1));
    decvarsal.addAll(Arrays.asList(arrivalLookup1));
    
    // arrivalTime[n] + distance[n,successor[n]] <= arrivalTime[successor[n]]
    for(int n=1; n<=nbCustomers+nbVehicles; n++) {
        model.element(distLookup1[n-1], distance[n-1], successor[n-1], 1).post();
        model.element(arrivalLookup1[n-1], arrivalTime, successor[n-1], 1).post();
        
        model.arithm(arrivalTime[n-1], "+", distLookup1[n-1], "<=", arrivalLookup1[n-1]).post();
    }
    
    // ----- load constraints ------------ %
    
    IntVar[] loadLookup=model.intVarArray("loadLookup", nbCustomers, 0, vehicleCapacity);
    decvarsal.addAll(Arrays.asList(loadLookup));
    
    //  load[n] + demand[n] = load[successor[n]]
    for(int n=1; n<=nbCustomers; n++) {
        model.element(loadLookup[n-1], load, successor[n-1], 1).post();
        
        model.arithm(load[n-1], "+", model.intVar(demand[n-1]), "=", loadLookup[n-1]).post();
    }
    
    //  load[n] = load[successor[n]]
    for(int n=nbCustomers+1; n<nbCustomers+nbVehicles; n++) {
        model.element(load[n-1], load, successor[n-1], 1).post();
    }
    
    // optVar = sum (depot in END_DEPOT_NODES) (arrivalTime[depot]);
    
    IntVar[] endDepotTimes=new IntVar[nbVehicles];
    for(int n=nbCustomers+nbVehicles+1; n<=nbCustomers+2*nbVehicles; n++) {
        endDepotTimes[n-(nbCustomers+nbVehicles+1)]=arrivalTime[n-1];
    }
    
    model.sum(endDepotTimes, "=", optVar).post();
    model.setObjective(Model.MINIMIZE, optVar);
    
    ////////////////////////////////////////////////////////////////////////////
    
    LNSSolve.solve(decvarsal, lnsvarsal, optVar, seed, timeLimit, btlimit, model, useInitialisationRestart, useTimeForInitialisation, initialisationBacktrackCounterValue);
    
    /* Debugging stuff, just find any solution and show it:
    IntVar[] decvars = new IntVar[decvarsal.size()];
    decvars = decvarsal.toArray(decvars);
    
    IntVar[] lnsvars = new IntVar[lnsvarsal.size()];
    lnsvars = lnsvarsal.toArray(lnsvars);
    
    // Set up basic search for first sol.
    Move basicsearch=ChocoConfig.makeInitMove(decvars, seed);
    model.getSolver().setMove(basicsearch);
    model.getSolver().limitTime(timeLimit*1000);
    boolean foundFirstSol=model.getSolver().solve();
    System.out.println(foundFirstSol);
    
    
    System.out.println("Successor:");
    for(int i=0; i<successor.length; i++) {
        System.out.println(successor[i].getValue());
    }
    
    System.out.println("Predecessor:");
    for(int i=0; i<predecessor.length; i++) {
        System.out.println(predecessor[i].getValue());
    }
    
    System.out.println("Vehicle:");
    for(int i=0; i<vehicle.length; i++) {
        System.out.println(vehicle[i].getValue());
    }
    System.out.println("Load:");
    for(int i=0; i<load.length; i++) {
        System.out.println(load[i].getValue());
    }
    System.out.println("arrivalTime:");
    for(int i=0; i<arrivalTime.length; i++) {
        System.out.println(arrivalTime[i].getValue());
    }
    System.out.println("optVar:"+optVar.getValue());
    */
    
    } catch(Exception e){
        System.err.println("Error");
        e.printStackTrace();
        System.exit(1);
    }
    System.exit(0);
}
}
