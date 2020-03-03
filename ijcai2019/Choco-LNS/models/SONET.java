
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
import java.util.ArrayList;


/**
 * SONET
 */

// Command syntax: java -jar SONET.jar <instance> <seed> <expbased/propguided> <backtrackLimit> [<timeLimit_in_second>] [symbreak]
// Example: java -jar SONET.jar instances/sonet/SONET60_30_30.lns 0 propguided 100000 600 symbreak

public class SONET {
    

public static void main(String [] args){
    try{
    int seed=Integer.valueOf(args[1]);
    int btlimit=Integer.valueOf(args[3]);
    
    int timeLimit = 600; //NGUYEN: add this
    if (args.length>4)
        timeLimit = Integer.valueOf(args[4]); //NGUYEN: add this

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
    
    // Eprime:
    //  find network_ExplicitVarSizeWithMarkerR2_Marker: int(0..nrings)
    //  find network_ExplicitVarSizeWithMarkerR2_Values_Occurrence: matrix indexed by [int(1..nrings), int(1..nnodes)] of bool
    BoolVar[][] network = new BoolVar[nrings][];
    for(int i=0; i<nrings; i++) {
        network[i]=model.boolVarArray("network["+i+"]", nnodes);
    }
    IntVar marker = model.intVar("marker", 0, nrings);
      
    // Aux vars recording whether marker <= a particular index i
    BoolVar[] markerReifications = model.boolVarArray("markerReifications", nrings);
    for (int i = 0; i < nrings; i++) {
      model.reifyXltC(marker, i+1, markerReifications[i]) ;
    }
    
    // Aux vars recording the number of installations on each ring
    IntVar[] ringSums = model.intVarArray("ringSums", nrings, 0, nnodes) ;
    for (int i = 0; i < nrings; i++) {
      model.sum(network[i], "=", ringSums[i]).post() ;
    }
    
    // Aux vars recording the product of the no of installations on ring i with the state of the
    // marker variable
    IntVar[] markerRingSumProducts = model.intVarArray("markerRingSumProducts", nrings, 0, nnodes) ;
    for (int i = 0; i < nrings; i++) {
      model.times(markerReifications[i], ringSums[i], markerRingSumProducts[i]).post() ;
    }
      
    // Active rings have at least two installations, but cannot exceed capacity
    // EPrime:
    // and([q3 <= network_ExplicitVarSizeWithMarkerR2_Marker ->
    //   2 <= sum([toInt(network_ExplicitVarSizeWithMarkerR2_Values_Occurrence[q3, q4]) | q4 : int(1..nnodes)])
    //   | q3 : int(1..nrings)]),
    // and([q3 <= network_ExplicitVarSizeWithMarkerR2_Marker ->
    // sum([toInt(network_ExplicitVarSizeWithMarkerR2_Values_Occurrence[q3, q4]) | q4 : int(1..nnodes)]) <= capacity
    // | q3 : int(1..nrings)])
    for (int i = 0; i < nrings; i++) {
      model.ifThen(markerReifications[i], model.arithm(ringSums[i], ">=", 2)) ;
      model.ifThen(markerReifications[i], model.arithm(ringSums[i], "<=", capacity)) ;
    }
      
    // Each Demand is met
    // Eprime:
    // and([or([q11 <= network_ExplicitVarSizeWithMarkerR2_Marker /\
    // and([network_ExplicitVarSizeWithMarkerR2_Values_Occurrence[q11, demand_ExplicitR3_Explicit[q10, q13]]
    //                | q13 : int(1..2)])
    //           | q11 : int(1..nrings)])
    //       | q10 : int(1..fin1)]),
      
    // Need an or over the conjunction of a markerreification and two entries in nwork
    for(int i=0; i<demand.length; i++) {
      BoolVar[] disjuncts = new BoolVar[nrings] ;
      for(int j=0; j<nrings; j++) {
        disjuncts[j] = model.and(markerReifications[j],
                                 network[j][demand[i][0]-1],
                                 network[j][demand[i][1]-1]).reify() ;
      }
      model.or(disjuncts).post() ;
    }
      
      
    if(args.length>5 && args[5].equals("symbreak")) {
      // Symmetry breaking on the active rings
      // Eprime:     and([q1 + 1 <= network_ExplicitVarSizeWithMarkerR2_Marker ->
      //  [toInt(network_ExplicitVarSizeWithMarkerR2_Values_Occurrence[q1, q5]) | q5 : int(1..nnodes)] <lex
      //  [toInt(network_ExplicitVarSizeWithMarkerR2_Values_Occurrence[q1 + 1, q6]) | q6 : int(1..nnodes)]
      //  | q1 : int(1..nrings - 1)]),
      for (int i = 1; i < nrings; i++) {
        model.ifThen(markerReifications[i],
                     model.lexLess(network[i-1], network[i])) ;
      }
    }
    else {
      // For every pair of active rings they must differ somewhere.
      for (int i = 0; i < nrings; i++) {
        for (int j = i+1; j < nrings; j++) {
          // reify diseqs between pairs of nodes
          BoolVar[] reifiedDiseqs = new BoolVar[nnodes] ;
          for (int k = 0; k < nnodes; k++) {
            reifiedDiseqs[k] = model.arithm(network[i][k], "!=", network[j][k]).reify() ;
          }
          // check if both rings active.
          model.ifThen(markerReifications[j],
                       model.or(reifiedDiseqs)) ;
        }
      }
    }
      
    // Symmetry breaking on the inactive rings.
    for (int i = 1; i < nrings; i++) {
      model.ifThen(model.boolNotView(markerReifications[i]),
                   model.sum(network[i], "=", 0)) ;
    }
      
    // Eprime: optVar =
    // sum([toInt(q8 <= network_ExplicitVarSizeWithMarkerR2_Marker) *
    // catchUndef(sum([toInt(network_ExplicitVarSizeWithMarkerR2_Values_Occurrence[q8, q9]) | q9 : int(1..nnodes)]),
    //                0)
    // | q8 : int(1..nrings)]),
    // Sum over all possible rings, guarding with index <= marker
    //   So we will need a set of bools for the reification.
    //   Then we need to sum the product of each reified variable with the right slice of network
    IntVar optVar=model.intVar("optVar", 0, capacity*nrings);
    model.sum(markerRingSumProducts, "=", optVar).post() ;
    model.setObjective(Model.MINIMIZE, optVar);
    
    ////////////////////////////////////////////////////////////////////////////
    // eprime branching on [network_ExplicitVarSizeWithMarkerR2_Marker, network_ExplicitVarSizeWithMarkerR2_Values_Occurrence, optVar]
    IntVar[] allvars = new IntVar[2 + nrings * nnodes] ;
    allvars[0] = marker ;
    allvars[allvars.length-1] = optVar ;
    int index = 1 ;
    for (int i = 0; i < nrings; i++)
      for (int j = 0; j < nnodes; j++) {
        allvars[index] = network[i][j] ;
        index++ ;
      }
      
    IntVar[] decvars=allvars;
    //System.out.println("Starting search.");
    
    // Set up basic search for first sol.
    Move basicsearch=new MoveBinaryDFS(new DomOverWDeg(decvars, seed, new IntDomainRandom(seed)));
    model.getSolver().setMove(basicsearch);
    model.getSolver().limitTime(timeLimit*1000);


    //NGUYEN: add this for initialisation choices
    if (useInitialisationRestart == 1){
        if (useTimeForInitialisation == 1)
            model.getSolver().setGeometricalRestart(1000000, 1.5, new TimeCounter(model, 1000000), 1000000000);
        else
            model.getSolver().setGeometricalRestart(initialisationBacktrackCounterValue, 1.5, new BacktrackCounter(model, 1000000000), 100000);
    }
    
    boolean foundFirstSol=model.getSolver().solve();
    if(foundFirstSol) {
        //System.out.println("First solution:");
        //System.out.println(optVar.getValue());
        System.out.println(model.getSolver().getTimeCount()+" "+optVar.getValue());
    }
    else {
        //System.out.println("No first solution found.");
        System.exit(0);
    }
    
    //   Type of LNS neighbourhood -- propagation-guided LNS.
    INeighbor in=INeighborFactory.propagationGuided(decvars);
    /*INeighbor in;
    if(args[2].equals("propguided")) {
        in=INeighborFactory.propagationGuided(decvars);
    }
    else {
        in=INeighborFactory.explanationBased(decvars);
    }*/
    
    in.init(); // Should this be necessary?
    
    //   Type of search within LNS neighbourhoods
    Move innersearch=new MoveBinaryDFS(new DomOverWDeg(decvars, seed, new IntDomainMin()));
    
    MoveLNS lns=new MoveLNS(innersearch, in, new BacktrackCounter(model, btlimit));
    
    model.getSolver().setMove(lns);
    model.getSolver().limitTime(timeLimit*1000);
    
    boolean interimsol=true;
    
    while(interimsol) {
        //  Run search in one neighbourhood...?
        interimsol=model.getSolver().solve();
        
        if(interimsol) {
            System.out.println(model.getSolver().getTimeCount()+" "+optVar.getValue());
            
            /*for(int i=0; i<parents.length; i++) {
                System.out.println("parents["+i+"]="+parents[i].getValue());
            }
            for(int i=0; i<depths.length; i++) {
                System.out.println("depths["+i+"]="+depths[i].getValue());
            }*/
        }
    }
    
    } catch(Exception e){
        System.err.println("Error");
        e.printStackTrace();
        System.exit(1);
    }
    System.exit(0);
}
}
