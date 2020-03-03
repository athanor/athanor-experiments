
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
 * Expected args: instance-name, seed, LNS-type
 */
public static void main(String [] args) {
    try{
  int seed = Integer.valueOf(args[1]);
  int btlimit=Integer.valueOf(args[3]);


    int timeLimit = 600; //NGUYEN: add this
    if (args.length>4)
        timeLimit = Integer.valueOf(args[4]); //NGUYEN: add this

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

  // Identify some bounds
  //  Max opt is sum of max distance in each row of distances
  int maxOpt = 0, maxDistance = 0 ;
  for (int row = 0; row < numberCities; row++) {
    int maxInRow = 0 ;
    for (int col = 0; col < numberCities; col++) {
      if (distances[row*numberCities+col] > maxInRow)
        maxInRow = distances[row*numberCities+col] ;
    }
    if (maxInRow > maxDistance)
      maxDistance = maxInRow ;
    maxOpt += maxInRow ;
  }
  
  Model model = new Model("TSP");
  
  // Explicit injective sequence model.
  IntVar[] tour = model.intVarArray("tour", numberCities, 0, numberCities-1) ;
  model.allDifferent(tour).post();
  
  // Keep track of the distance travelled between
  IntVar[] adjacentDistances =
    model.intVarArray("adjacentDistances", numberCities, 0, maxDistance) ;
  
  // We need index variables for our element constraint
  // Calculation: row(city1) * numberCities + col (city 2)
  IntVar[] indices =
    model.intVarArray("indices", numberCities, 0, numberCities * numberCities - 1) ;
  for (int i = 0; i < numberCities - 1; i++)
    model.scalar(new IntVar[]{tour[i], tour[i+1]}, new int[]{numberCities, 1}, "=", indices[i]).post() ;
  model.scalar(new IntVar[]{tour[numberCities-1], tour[0]}, new int[]{numberCities, 1}, "=", indices[numberCities-1]).post() ;
  
  // Element constraint connecting adjacentDistances and tour variables
  for (int i = 0; i < numberCities; i++)
    model.element(adjacentDistances[i], distances, indices[i]).post() ;
                                                                       
  // optimisation variable.
  IntVar optVar = model.intVar("optVar", 0, maxOpt);
  model.sum(adjacentDistances, "=", optVar).post() ;
  model.setObjective(Model.MINIMIZE, optVar) ;
    
    ////////////////////////////////////////////////////////////////////////////
    IntVar[] decvars = tour;
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
      
      // Testing:
      //System.out.print("Tour: ") ;
      //for (int i = 0; i < numberCities; i++)
      //  System.out.print(tour[i].getValue()+" ") ;
      //System.out.println() ;
      //System.out.print("Adjacent Distances: ") ;
      //for (int i = 0; i < numberCities; i++)
      //  System.out.print(adjacentDistances[i].getValue()+" ") ;
      //System.out.println() ;
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
