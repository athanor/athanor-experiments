
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.BoolVar;

import static org.chocosolver.solver.search.strategy.Search.*;

import org.chocosolver.solver.search.loop.lns.neighbors.PropagationGuidedNeighborhood;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.loop.lns.*;
import org.chocosolver.solver.search.loop.lns.neighbors.*;
import org.chocosolver.solver.search.limits.BacktrackCounter;
import org.chocosolver.solver.search.limits.*;
import org.chocosolver.solver.search.loop.move.*;
import org.chocosolver.solver.search.strategy.selectors.values.*;
import org.chocosolver.solver.search.strategy.selectors.variables.*;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.exception.SolverException;

import java.io.*;
import java.util.* ;

/**
 * TSP
 */
public class TSP {
    
/**
 * Expected args: instance-name, seed, btlimit, timelimit
 */
public static void main(String [] args) {
    try{
  int seed = Integer.valueOf(args[1]);
  int btlimit=Integer.valueOf(args[2]);


    int timeLimit = 600; //NGUYEN: add this
    if (args.length>3)
        timeLimit = Integer.valueOf(args[3]); //NGUYEN: add this

    int useInitialisationRestart = 1;
    int useTimeForInitialisation = 0; //if 0, use BacktrackCounter instead of TimeCounter
    int initialisationBacktrackCounterValue = 50; // other option: btlimit
  
  // instance parameters
  int numberCities = 0;
  int[] distances = null ;
  
  // get thy reader ready
  Scanner scanner = null ;
  String fn = args[0];
  try {
    scanner = new Scanner(new BufferedReader(new FileReader(fn))) ;
    numberCities = scanner.nextInt() ;
    distances = new int[numberCities*numberCities] ;
    for (int i = 0; i < numberCities * numberCities; i++)
      distances[i] = scanner.nextInt() ;
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
  
  // this is the wikipedia instance
  /*if (args[0].equals("test")) {
    numberCities = 4 ;
    distances = new int[] { 0, 20, 42, 35,
                           20, 0, 30, 34,
                           42, 30, 0, 12,
                           35, 34, 12, 0 } ;
  }
  else {
    System.out.println("Unknown instance: "+args[0]) ;
    return ;
  }*/
  
  // 
  int[][] dist2d = new int[numberCities][];
  for (int row = 0; row < numberCities; row++) {
    dist2d[row]=new int[numberCities];
    for (int col = 0; col < numberCities; col++) {
      dist2d[row][col]=distances[row*numberCities+col];
    }
  }
  
  // Identify some bounds
  //  Max opt is sum of max distance in each row of distances
  int maxOpt = 0, maxDistance = 0 ;
  for (int row = 0; row < numberCities; row++) {
    int maxInRow = 0 ;
    for (int col = 0; col < numberCities; col++) {
      if (distances[row*numberCities+col] > maxInRow)
        maxInRow = distances[row*numberCities+col] ;
    }
    if (maxInRow > maxDistance) {
      maxDistance = maxInRow ;
    }
    maxOpt += maxInRow ;
  }
  
  Model model = new Model("TSP");
  
  ArrayList<IntVar> decvarsal=new ArrayList<>();
  ArrayList<IntVar> lnsvarsal=new ArrayList<>();
  
  //  successor model
  IntVar[] succ = model.intVarArray("succ", numberCities, 0, numberCities-1);
  
  decvarsal.addAll(Arrays.asList(succ));
  lnsvarsal.addAll(Arrays.asList(succ));
  
  //  Single circuit.
  model.subCircuit(succ, 0, model.intVar(numberCities)).post();
  
  //  Alldiff to match mzn model
  model.allDifferent(succ).post();
  
  // Keep track of the distance travelled between
  IntVar[] adjacentDistances =
    model.intVarArray("adjacentDistances", numberCities, 0, maxDistance) ;
  decvarsal.addAll(Arrays.asList(adjacentDistances));
  
  // Element constraint connecting adjacentDistances and successor variables (to match the mzn model)
  for (int i = 0; i < numberCities; i++)
    model.element(adjacentDistances[i], dist2d[i], succ[i]).post();
                                                                       
  // optimisation variable.
  IntVar optVar = model.intVar("optVar", 0, maxOpt);
  decvarsal.add(optVar);
  model.sum(adjacentDistances, "=", optVar).post() ;
  model.setObjective(Model.MINIMIZE, optVar) ;
    
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
