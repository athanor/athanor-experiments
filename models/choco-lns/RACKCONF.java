
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.BoolVar;

import org.chocosolver.solver.constraints.Constraint;

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
 * Rack Configuration
 */
public class RACKCONF {
    
/**
 * Expected args: instance-name, seed, btlimit, timelimit
 */
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

  // instance parameters
  int n_models = 0, n_types = 0, n_cards = 0, n_racks = 0 ;
  int[] max_power = null, max_connects = null, price = null, req_power = null, ctype = null ;

    String inputFileName = args[0];
    BufferedReader br = new BufferedReader(new FileReader(inputFileName));

    String ln = br.readLine();
    String[] ls = ln.split(" ");
    n_models = Integer.parseInt(ls[0]);
    n_cards = Integer.parseInt(ls[1]);
    n_racks = Integer.parseInt(ls[2]);
    n_types = Integer.parseInt(ls[3]);
    
    max_power = new int[n_models];
    String[] lss = br.readLine().split(" ");
    for (int i=0;i<n_models;i++){
        max_power[i] = Integer.parseInt(lss[i]);
    }

    max_connects = new int[n_models];
    lss = br.readLine().split(" ");
    for (int i=0;i<n_models;i++){
        max_connects[i] = Integer.parseInt(lss[i]);
    }

    price = new int[n_models];
    lss = br.readLine().split(" ");
    for (int i=0;i<n_models;i++){
        price[i] = Integer.parseInt(lss[i]);
    }


    req_power = new int[n_cards];
    lss = br.readLine().split(" ");
    for (int i=0;i<n_cards;i++){
        req_power[i] = Integer.parseInt(lss[i]);
    }
  
    ctype = new int[n_cards];
    lss = br.readLine().split(" ");
    for (int i=0;i<n_cards;i++){
        ctype[i] = Integer.parseInt(lss[i]);
    } 
  
  // We need to know the maximum power of any model to introduce our power variables later
  int powerUB = 0 ;
  for (int i = 0; i < n_models; i++)
    if (max_power[i] > powerUB)
      powerUB = max_power[i] ;
  // Similarly for connections
  int connectionsUB = 0 ;
  for (int i = 0; i < n_models; i++)
    if (max_connects[i] > connectionsUB)
      connectionsUB = max_connects[i] ;
  // We need an upper bound on price to state objective
  int priceUB = 0 ;
  for (int i = 0; i < n_models; i++)
    if (price[i] > priceUB)
      priceUB = price[i] ;
  
  Model model = new Model("RACK");

  // Essence:
  // find configuration :
  //   function Rack --> (Model,
  //                      set (maxSize max(range(max_connects))) of Card)
  BoolVar[] switches = model.boolVarArray("switches", n_racks) ;
  IntVar[] rackModel = model.intVarArray("rackModel", n_racks, 0, n_models-1) ;
  BoolVar[][] pluggedOcc = new BoolVar[n_racks][] ;
  for (int i = 0; i < n_racks; i++)
    pluggedOcc[i] = model.boolVarArray(n_cards) ;
  
  
  // every card is plugged into one rack
  //  Sum the product of the flag and card column and require it to be 1
  //  NB If we required all unused rows of pluggedOcc to be 0 then in fact
  //     we just need to sum the c column of pluggedOcc to 1
  for (int c = 0; c < n_cards; c++) {
    BoolVar[] products = model.boolVarArray(n_racks) ;
    for (int r = 0; r < n_racks; r++)
      model.arithm(switches[r], "*", pluggedOcc[r][c], "=", products[r]).post() ;
    model.sum(products, "=", 1).post() ;
  }
  
  // The power demand placed on a rack does not exceed the maximum it can supply
  // Essence:
  // forAll (r, config) in configuration .
  //   (sum c in config[2] . (req_power(ctype(c))))
  //   <= max_power(config[1])
  // cardPower (array of constant) is req_power[ctype[c]]
  // Element has to be guarded and rackMaxPower pinned to 0 if inactive.
  // NB2 Could we in fact do without the guard if model = 0 had power 0? But it would also need a dummy max conn
  IntVar[] rackMaxPower = model.intVarArray(n_racks, 0, powerUB) ;
  for (int r = 0; r < n_racks; r++) {
    model.ifThenElse(switches[r],
                     model.element(rackMaxPower[r], max_power, rackModel[r]),
                     model.arithm(rackMaxPower[r], "=", 0)) ;
    model.ifThen(switches[r],
                 model.scalar(pluggedOcc[r], req_power, "<=", rackMaxPower[r])) ;
  }
  
  // the number of cards plugged in to a rack does not exceed the number of slots available
  // Essence:
  // forAll (r, config) in configuration .
  //   |config[2]| <= max_connects(config[1])
  IntVar[] rackMaxConnections = model.intVarArray(n_racks, 0, connectionsUB) ;
  for (int r = 0; r < n_racks; r++) {
    model.ifThenElse(switches[r],
                     model.element(rackMaxConnections[r], max_connects, rackModel[r]),
                     model.arithm(rackMaxConnections[r], "=", 0)) ;
    model.ifThen(switches[r],
                 model.sum(pluggedOcc[r], "<=", rackMaxConnections[r])) ;
  }
  
  // Symmetry Breaking
  for (int r = 0; r < n_racks; r++) {
    model.ifThen(model.boolNotView(switches[r]),
                 model.arithm(rackModel[r], "=", 0)) ;
    model.ifThen(model.boolNotView(switches[r]),
                 model.sum(pluggedOcc[r], "=", 0)) ;
  }
  
  // optimisation variable.
  // Docs says sum of boolvars much faster than intvars.
  // minimise the total rack price
  // find optVar : int(0..n_racks*max([ price(r) | r : Model]))
  // optVar=sum r in defined(model) . price(model(r))
  IntVar[] rackPrices = model.intVarArray(n_racks, 0, priceUB) ;
  for (int r = 0; r < n_racks; r++)
    model.ifThenElse(switches[r],
                     model.element(rackPrices[r], price, rackModel[r]),
                     model.arithm(rackPrices[r], "=", 0)) ;
  IntVar optVar = model.intVar("optVar", 0, n_racks*priceUB) ;
  model.sum(rackPrices, "=", optVar).post() ;
  model.setObjective(Model.MINIMIZE, optVar) ;
  
  ////////////////////////////////////////////////////////////////////////////
  // switches, rackModel, plugged
  IntVar[] decvars = new IntVar[n_racks + n_racks + n_racks * n_cards] ;
  for (int r = 0; r < n_racks; r++) {
    decvars[r] = switches[r] ;
    decvars[r+n_racks] = rackModel[r] ;
  }
  int i = 2 * n_racks ;
  for (int r = 0; r < n_racks; r++) {
    for (int c = 0; c < n_cards; c++) {
      decvars[i] = pluggedOcc[r][c] ;
      i++ ;
    }
  }

  //System.out.println("Starting search.");
    
    // Set up basic search for first sol.
    Move basicsearch=ChocoConfig.makeInitMove(decvars, seed);
    
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
        /*
        System.out.println("<switches> <rackModel> <pluggedOcc>") ;
        for (int r = 0; r < n_racks; r++) {
          System.out.print(switches[r].getValue()+" ") ;
          System.out.print(rackModel[r].getValue()+" ") ;
          for (int c = 0; c < n_cards; c++)
            System.out.print(pluggedOcc[r][c].getValue()+" ") ;
          System.out.println() ;
        }
        */
    }
    else {
        //System.out.println("No first solution found.");
        System.exit(0);
    }
    
    //   Type of LNS neighbourhood -- propagation-guided LNS.
    INeighbor in = ChocoConfig.build(decvars);
    
    in.init(); // Should this be necessary?
    
    //   Type of search within LNS neighbourhoods
    Move innersearch=ChocoConfig.makeLNSMove(decvars, seed);
    
    MoveLNS lns=new MoveLNS(innersearch, in, new BacktrackCounter(model, btlimit));
    
    model.getSolver().setMove(lns);
    model.getSolver().limitTime(timeLimit*1000);
    
    boolean interimsol=true;
    
    while(interimsol) {
        //  Run search in one neighbourhood...?
        interimsol=model.getSolver().solve();
        
        if(interimsol) {
            System.out.println(model.getSolver().getTimeCount()+" "+optVar.getValue());
          /*
          System.out.println("<switches> <rackModel> <pluggedOcc>") ;
          for (int r = 0; r < n_racks; r++) {
            System.out.print(switches[r].getValue()+" ") ;
            System.out.print(rackModel[r].getValue()+" ") ;
            for (int c = 0; c < n_cards; c++)
              System.out.print(pluggedOcc[r][c].getValue()+" ") ;
            System.out.println() ;
          }
          */
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
