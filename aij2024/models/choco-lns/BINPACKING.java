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
 * BINPACKING
 */
public class BINPACKING {
  
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
      int noItems = 0 ;
      int noBins = 0 ;
      int capacity =0 ;
      int[] weights = null ;
      int totalWeight = 0 ;
      
      // get thy reader ready
      Scanner scanner = null ;
      String fn = args[0];
      try {
        scanner = new Scanner(new BufferedReader(new FileReader(fn))) ;
        noItems = scanner.nextInt() ;
        noBins = scanner.nextInt() ;
        capacity = scanner.nextInt() ;
        weights = new int[noItems] ;
        for (int i = 0; i < noItems; i++) {
          weights[i] = scanner.nextInt() ;
          totalWeight += weights[i] ;
        }
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
      
      // for testing
      /*System.out.println("noItems: "+noItems) ;
      System.out.println("noBins: "+noBins) ;
      System.out.println("Bin capacity: "+capacity) ;
      System.out.println("Item weights:") ;
      for (int i = 0; i < noItems; i++)
        System.out.print(weights[i]+" ") ;*/
      
      Model model = new Model("BINPACKING");
      
      // Occurrence model of packed items
      IntVar[][] packing = model.intVarMatrix("packing", noBins, noItems, 0, 1);
      
      // bin loads
      IntVar[] loads = model.intVarArray("loads", noBins, 0, capacity) ;
      
      // Indicate whether bin open
      BoolVar[] binOpen = model.boolVarArray("binOpen", noBins) ;
      
      // optimisation variable for minimisation.
      IntVar optVar = model.intVar("optVar", 0, noBins);
      
      ArrayList<IntVar> decvarsal=new ArrayList<>();
      for(int i=0; i<noBins; i++) {
          decvarsal.addAll(Arrays.asList(packing[i]));
      }
      decvarsal.addAll(Arrays.asList(loads));
      decvarsal.addAll(Arrays.asList(binOpen));
      decvarsal.add(optVar);
      
      ArrayList<IntVar> lnsvarsal=new ArrayList<IntVar>();
      for(int i=0; i<noBins; i++) {
          lnsvarsal.addAll(Arrays.asList(packing[i]));
      }
      
      // All items packed once - sum of each column of packing is 1.
      for (int item = 0; item < noItems; item++) {
        IntVar[] packingCol = new IntVar[noBins] ;
        for (int bin = 0; bin < noBins; bin++)
          packingCol[bin] = packing[bin][item] ;
        model.sum(packingCol, "=", 1).post() ;
      }
      
      // Total load in bin cannot exceed capacity
      // Weighted sum of each row of packing equals loads
      for (int bin = 0; bin < noBins; bin++) {
        model.scalar(packing[bin], weights, "=", loads[bin]).post() ;
      }
      
      // Connect loads and binOpen via reification
      for (int bin = 0; bin < noBins; bin++) {
        model.arithm(loads[bin], ">", 0).reifyWith(binOpen[bin]);
      }
      
      // Implied constraint: total bin load is total weight of items.
      model.sum(loads, "=", totalWeight).post() ;
      
      if(args.length>4 && args[4].equals("symbreak")) {
          // symmetry breaking as in mz model:
          model.arithm(loads[0], ">", 0).post() ;
          for (int bin = 0; bin < noBins-1; bin++) {
              model.arithm(loads[bin], ">=", loads[bin+1]).post() ;
          }
      }
      
      model.sum(binOpen, "=", optVar).post() ;
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
