
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.BoolVar;

import static org.chocosolver.solver.search.strategy.Search.*;

import org.chocosolver.solver.search.loop.lns.neighbors.PropagationGuidedNeighborhood;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.lns.*;
import org.chocosolver.solver.search.loop.lns.neighbors.*;
import org.chocosolver.solver.search.limits.*;
import org.chocosolver.solver.search.loop.move.*;
import org.chocosolver.solver.search.strategy.selectors.values.*;
import org.chocosolver.solver.search.strategy.selectors.variables.*;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.exception.SolverException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;


/**
 * SONET
 */

// Command syntax: java -jar SONET.jar <instance> <seed> <backtrackLimit> [<timeLimit_in_second>] [symbreak]
// Example: java -jar SONET.jar instances/sonet/SONET60_30_30.lns 0 100000 600 symbreak

public class SONET {
    

public static void main(String [] args){
    try{
    int seed=Integer.valueOf(args[1]);
    int btlimit=Integer.valueOf(args[2]);
    
    int timeLimit = 600; //NGUYEN: add this
    if (args.length>3)
        timeLimit = Integer.valueOf(args[3]); //NGUYEN: add this

    int useInitialisationRestart = 1;
    int useTimeForInitialisation = 0; //if 0, use BacktrackCounter instead of TimeCounter
    int initialisationBacktrackCounterValue = 50; // other option: btlimit
    
    int nnodes;
    int nrings;
    int capacity;
    int[][] demand;

    String inputFileName = args[0];
    BufferedReader br = new BufferedReader(new FileReader(inputFileName));
    String ln = br.readLine();
    String[] ls = ln.split(" ");
    nnodes = Integer.parseInt(ls[0]);
    nrings = Integer.parseInt(ls[1]);
    capacity = Integer.parseInt(ls[2]);
    ArrayList<int[]> d = new ArrayList<int[]>();
    int secondDim = 2;
    while (ln != null){
        ls = ln.split(" ");
        int[] sd = new int[secondDim];
        for (int i=0; i < secondDim;i++)
            sd[i] = Integer.parseInt(ls[i]);
        d.add(sd);
        ln = br.readLine();
    }
    demand = new int[d.size()][secondDim];
    demand = d.toArray(demand);
    
    // if(args.length>5 && args[5].equals("symbreak"))
      
  
    Model model = new Model("SONET");
    
    ArrayList<IntVar> decvarsal=new ArrayList<IntVar>();
    ArrayList<IntVar> lnsvarsal=new ArrayList<IntVar>();
    
    BoolVar[][] network = new BoolVar[nrings][];
    for(int i=0; i<nrings; i++) {
        network[i]=model.boolVarArray("network["+i+"]", nnodes);
        decvarsal.addAll(Arrays.asList(network[i]));
        lnsvarsal.addAll(Arrays.asList(network[i]));
    }
    
    // Aux vars recording the number of installations on each ring
    IntVar[] ringSums = model.intVarArray("ringSums", nrings, 0, capacity) ;
    decvarsal.addAll(Arrays.asList(ringSums));
    
    for (int i = 0; i < nrings; i++) {
      model.sum(network[i], "=", ringSums[i]).post() ;
    }
    
    // Need an or over the conjunction of two entries in nwork
    for(int i=0; i<demand.length; i++) {
      BoolVar[] disjuncts = new BoolVar[nrings] ;
      
      for(int j=0; j<nrings; j++) {
        disjuncts[j] = model.and(network[j][demand[i][0]-1],
                                 network[j][demand[i][1]-1]).reify() ;
      }
      decvarsal.addAll(Arrays.asList(disjuncts));
      
      model.or(disjuncts).post() ;
    }
    
    for(int i=0; i<nrings; i++) {
      model.arithm(ringSums[i], "!=", 1).post();
    }
    
    if(args.length>4 && args[4].equals("symbreak")) {
      for (int i = 1; i < nrings; i++) {
        model.lexLessEq(network[i], network[i-1]).post() ;
      }
    }
    
    IntVar optVar=model.intVar("optVar", 0, capacity*nrings);
    decvarsal.add(optVar);
    
    model.sum(ringSums, "=", optVar).post() ;
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
