
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
  int btlimit=Integer.valueOf(args[2]);


    int timeLimit = 600; //NGUYEN: add this
    if (args.length>3)
        timeLimit = Integer.valueOf(args[3]); //NGUYEN: add this

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
  IntVar[] picked = model.boolVarArray("picked", n) ;

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
    ArrayList<IntVar> decvarsal=new ArrayList<IntVar>();
    decvarsal.addAll(Arrays.asList(picked));
    decvarsal.add(optVar);
    
    ArrayList<IntVar> lnsvarsal=new ArrayList<IntVar>();
    lnsvarsal.addAll(Arrays.asList(picked));
    
    LNSSolve.solve(decvarsal, lnsvarsal, optVar, seed, timeLimit, btlimit, model, useInitialisationRestart, useTimeForInitialisation, initialisationBacktrackCounterValue);
    
    } catch(Exception e){
        System.err.println("Error");
        e.printStackTrace();
        System.exit(1);
    }
    System.exit(0);
}
}
