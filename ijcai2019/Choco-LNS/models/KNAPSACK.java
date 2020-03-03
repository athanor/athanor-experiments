
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

import java.io.*;
import java.util.* ;

/**
 * KNAPSACK
 */
public class KNAPSACK {
    
/**
 * Expected args: instance-name, seed, LNS-type
 */
public static void main(String [] args) {
    try{
  int seed=Integer.valueOf(args[1]);
  int btlimit=Integer.valueOf(args[3]);


    int timeLimit = 600; //NGUYEN: add this
    if (args.length>4)
        timeLimit = Integer.valueOf(args[4]); //NGUYEN: add this

    int useInitialisationRestart = 1;
    int useTimeForInitialisation = 0; //if 0, use BacktrackCounter instead of TimeCounter
    int initialisationBacktrackCounterValue = 50; // other option: btlimit

  // instance parameters
  int n = 0;
  int capacity=0 ;
  int[] weight = null, gain = null ;
  
  // get thy reader ready
  Scanner scanner = null ;
  String fn = args[0];
  try {
    scanner = new Scanner(new BufferedReader(new FileReader(fn))) ;
    n = scanner.nextInt() ;
    capacity = scanner.nextInt() ;
    weight = new int[n] ;
    for (int i = 0; i < n; i++)
      weight[i] = scanner.nextInt() ;
    gain = new int[n] ;
    for (int i = 0; i < n; i++)
      gain[i] = scanner.nextInt() ;
  }
  catch (IOException e) {
    System.out.println(e) ;
  }
  catch (NoSuchElementException e) {
    System.out.println("EOF reached when reading: "+fn) ;
  }
  finally {
    if (scanner != null) {
      scanner.close();
    }
  }

  /*System.out.println(Integer.toString(n));
  System.out.println(Integer.toString(capacity));
  for (int i=0;i<n;i++)
    System.out.print(Integer.toString(weight[i]) + " ");
  System.out.println("");*/

  /*if (args[0].equals("test")) {
    n = 5 ;
    capacity = 100 ;
    weight = new int[] {15, 25, 45, 50, 60} ;
    gain = new int[] {10, 20, 40, 40, 50} ;
  }
  else {
    System.out.println("Unknown instance: "+args[0]) ;
    return ;
  }*/

  Model model = new Model("KNAPSACK");
  
  // Occurrence model of selected items
  IntVar[] picked = model.intVarArray("picked", n, 0, 1) ;

  // Capacity constraint
  model.scalar(picked, weight, "<=", capacity).post() ;

  
  // optimisation variable. Max domain is sum of gains.
  int maxGain = 0 ;
  for (int i = 0; i < n; i++)
    maxGain += gain[i] ;

  IntVar optVar = model.intVar("optVar", 0, maxGain);
  model.scalar(picked, gain, "=", optVar).post() ;
  model.setObjective(Model.MAXIMIZE, optVar) ;
  
    
    ////////////////////////////////////////////////////////////////////////////
    IntVar[] decvars = picked;
    //System.out.println("Starting search.");
    
    // Set up basic search for first sol.
    Move basicsearch=new MoveBinaryDFS(new DomOverWDeg(decvars, seed, new IntDomainRandom(seed)));
    model.getSolver().setMove(basicsearch);
    model.getSolver().limitTime(timeLimit*1000);
    //model.getSolver().setGeometricalRestart(1000000, 1.5, new TimeCounter(model, 1000000), 1000000000);
    
    
    //NGUYEN: add this for initialisation choices
    if (useInitialisationRestart == 1){
        if (useTimeForInitialisation == 1)
            model.getSolver().setGeometricalRestart(1000000, 1.5, new TimeCounter(model, 1000000), 1000000000);
        else
            //model.getSolver().setGeometricalRestart(initialisationBacktrackCounterValue, 1.5, new BacktrackCounter(model, 1000000000), 100000);
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
        //in=INeighborFactory.explanationBased(decvars);
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
            
            //for(int i=0; i<parents.length; i++) {
            //    System.out.println("parents["+i+"]="+parents[i].getValue());
            //}
            //for(int i=0; i<depths.length; i++) {
            //    System.out.println("depths["+i+"]="+depths[i].getValue());
            //}
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
