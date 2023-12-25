
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
        for (int j=0;j<nNodes;j++)
            costs[i][j] = Integer.parseInt(ls[j]);
    }
    
    int maxLinkCost=0;
    for(int i=0; i<=numberLocations; i++) {
        for(int j=0; j<=numberLocations; j++) {
            if(costs[i][j]>maxLinkCost) maxLinkCost=costs[i][j];
        }
    }
    //System.out.println("Max link cost: "+maxLinkCost);
    
    int[] costsFlat=new int[(numberLocations+1)*(numberLocations+1)];
    for(int i=0; i<numberLocations+1; i++) {
        for(int j=0; j<numberLocations+1; j++) {
            costsFlat[i*(numberLocations+1) + j]=costs[i][j];
        }
    }
    
    // Cost of putting each item onto its own vehicle and driving it to its target and back
    int maxTotalCost = 0;
    for(int i=1; i<=numberLocations; i++) {
        maxTotalCost += (2*costs[0][i]);
    }
    
    int totalOrderWeight=0;
    for(int i=1; i<=numberLocations; i++) {
        totalOrderWeight+=orderWeights[i-1];
    }
    
    int minVehicles = (int) Math.ceil( ((double)totalOrderWeight) / ((double)vehicleCapacity) );
    //  Basic type is set of sequences. Outer set is variable-sized explicit, inner
    //  sequence is variable-sized 
    
    Model model = new Model("CVRP");
    
    // Length of each seq. 0 means unused. 
    IntVar[] seqLength=model.intVarArray("seqLength", numberLocations, 0, numberLocations);
    
    ArrayList<IntVar> decvarsal=new ArrayList<IntVar>();
    decvarsal.addAll(Arrays.asList(seqLength));
    
    ArrayList<IntVar> lnsvarsal=new ArrayList<IntVar>();
    
    // Define planZero vars, and set them to zero when unused. 
    
    IntVar[][] planZero=new IntVar[numberLocations][];
    for(int i=0; i<numberLocations; i++) {
        planZero[i]=model.intVarArray("planZero["+(i+1)+"]", numberLocations+2, 0, numberLocations);
        decvarsal.addAll(Arrays.asList(planZero[i]));
        for(int j=1; j<numberLocations+1; j++) {
            lnsvarsal.add(planZero[i][j]);
        }
        
        for(int j=1; j<numberLocations+1; j++) {
            model.ifThenElse(model.arithm(seqLength[i], ">=", j),
                model.arithm(planZero[i][j], "!=", 0),
                model.arithm(planZero[i][j], "=", 0));
        }
        
        //  Set the ends to 0. 
        model.arithm(planZero[i][0], "=", 0).post();
        model.arithm(planZero[i][numberLocations+1], "=", 0).post();
    }
    
    //  New order weights with 0 at start. 
    int[] orderWts=new int[orderWeights.length+1];
    System.arraycopy(orderWeights, 0, orderWts, 1, orderWeights.length);
    orderWts[0]=0;
    
    //  Capacity restriction on vehicle
    for(int i=0; i<numberLocations; i++) {
        //  Add up order weight in this sequence, <= capacity
        IntVar[] orderwtcumul = model.intVarArray("orderwtcumul["+(i+1)+"]", numberLocations, 0, vehicleCapacity);
        decvarsal.addAll(Arrays.asList(orderwtcumul));
        
        for(int j=0; j<numberLocations; j++) {
            model.element(orderwtcumul[j], orderWts, planZero[i][j+1], 0).post();
        }
        
        //  Sum orderwtcumul to orderWt.  Domain of orderWt
        model.sum(orderwtcumul,"<=", vehicleCapacity).post();
    }
    
    //  Put all planZero vars into one 1D array (except the start and end 0 vars)
    IntVar[] planZeroFlat=new IntVar[numberLocations*numberLocations];
    for(int i=0; i<numberLocations; i++) {
        for(int j=1; j<numberLocations+1; j++) {
            planZeroFlat[i*numberLocations+j-1]=planZero[i][j];
        }
    }
    //  Deliver each order at most once.
    model.allDifferentExcept0(planZeroFlat).post();
    
    //  Sum of route lengths = number locations (together with above means deliver each order exactly once).
    model.sum(seqLength, "=", numberLocations).post();
    
    for(int i=0; i<minVehicles; i++) {
        model.arithm(seqLength[i], ">", 0).post();
    }
    
    if(args.length>4 && args[4].equals("symbreak")) {
        for(int i=1; i<numberLocations; i++) {
            //  Just order the rows of planZero in decreasing order. 
            //model.lexLessEq(planZero[i], planZero[i-1]).post();
            model.arithm(planZero[i][1], "<=", planZero[i-1][1]).post();
        }
    }
    
    //  Value of the objective. 
    IntVar[] optVarPart=model.intVarArray("optVarPart", numberLocations*(numberLocations+1), 0, maxTotalCost);
    decvarsal.addAll(Arrays.asList(optVarPart));
    
    for(int i=0; i<numberLocations; i++) {
        // The cost of travelling to  each element of the sequence starting with 0 (depot) -> first element
        // and ending with last element -> depot. 
        for(int j=0; j<numberLocations+1; j++) {
            // For each adjacent pair in the sequence, look up the cost. 
            // Includes the 0's at both ends of the sequence. 
            IntVar costidx=model.intVar("costidx", 0, (numberLocations+1)*(numberLocations+1)-1);
            decvarsal.add(costidx);
            
            model.arithm(model.intScaleView(planZero[i][j], (numberLocations+1)), "+", planZero[i][j+1],"=",costidx).post();
            
            model.element(optVarPart[(i*(numberLocations+1)) + j], costsFlat, costidx, 0).post();
        }
    }
    
    IntVar optVar=model.intVar("optVar", 0, maxTotalCost);
    decvarsal.add(optVar);
    
    model.sum(optVarPart, "=", optVar).post();
    
    model.setObjective(Model.MINIMIZE, optVar);
    
    ////////////////////////////////////////////////////////////////////////////
    
    LNSSolve.solve(decvarsal, lnsvarsal, optVar, seed, timeLimit, btlimit, model, useInitialisationRestart, useTimeForInitialisation, initialisationBacktrackCounterValue);
    
    } catch(Exception e){
        System.err.println("Error");
        e.printStackTrace();
        System.exit(1);
    }
    System.exit(0);
}
}
