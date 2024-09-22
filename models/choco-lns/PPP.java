
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
 * PPP
 */
public class PPP {
    
/**
 * Expected args: instance-name, seed, LNS-type
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
  int numberBoats = 0, numberPeriods = 0 ;
  int[] capacity = null ;
  int[] crew = null ;
  
    String inputFileName = args[0];
    BufferedReader br = new BufferedReader(new FileReader(inputFileName));

    String ln = br.readLine();
    String[] ls = ln.split(" ");
    numberPeriods = Integer.parseInt(ls[0]);  
    numberBoats = Integer.parseInt(ls[1]);
    
    capacity = new int[numberBoats];
    String[] lss = br.readLine().split(" ");
    for (int i=0;i<numberBoats;i++){
        capacity[i] = Integer.parseInt(lss[i]);
    }

    crew = new int[numberBoats];
    lss = br.readLine().split(" ");
    for (int i=0;i<numberBoats;i++){
        crew[i] = Integer.parseInt(lss[i]);
    }

  Model model = new Model("PPP");

  // Essence:
  // find hosts : set of Boat,
  //      sched : set (size n_periods) of function (total) Boat --> Boat
  
  // Occurrence model of hosts
  BoolVar[] hosts = model.boolVarArray("hosts", numberBoats) ;
  
  ArrayList<IntVar> decvarsal=new ArrayList<IntVar>();
  decvarsal.addAll(Arrays.asList(hosts));
  
  ArrayList<IntVar> lnsvarsal=new ArrayList<IntVar>();
  lnsvarsal.addAll(Arrays.asList(hosts));
  
  // Explicit model of sched  sched[period][boat] = host
  IntVar[][] sched = new IntVar[numberPeriods][] ;
  for (int p = 0; p < numberPeriods; p++) {
    sched[p] = model.intVarArray("sched_period_"+p, numberBoats, 0, numberBoats-1);
    decvarsal.addAll(Arrays.asList(sched[p]));
    lnsvarsal.addAll(Arrays.asList(sched[p]));
  }

  // Occurrence model of sched  occSched[period][host][boat]
  BoolVar[][][] occSched = new BoolVar[numberPeriods][numberBoats][] ;
  for (int p = 0; p < numberPeriods; p++) {
    for (int h = 0; h < numberBoats; h++) {
      occSched[p][h] = model.boolVarArray("occSched_period_"+p+"_host_"+h, numberBoats) ;
      decvarsal.addAll(Arrays.asList(occSched[p][h]));
    }
  }
  
  // Channelling:
  // sched[p][b] = h iff occSched[p][h][b] = 1
  for (int p = 0; p < numberPeriods; p++) {
    for (int h = 0; h < numberBoats; h++) {
      for (int b = 0; b < numberBoats; b++) {
        model.arithm(sched[p][b], "=", h).reifyWith(occSched[p][h][b]);
      }
    }
  }
  
  // forAll p in sched . forAll h in hosts . p(h) = h,
  for (int h = 0; h < numberBoats; h++)
    for (int p = 0; p < numberPeriods; p++) {
      Constraint c = model.arithm(sched[p][h], "=", h) ;
      c.reifyWith(hosts[h]) ;
    }
  
  // Only assign to hosts -- simple version using occurrence repn
  for (int p = 0; p < numberPeriods; p++) {
    for (int b = 0; b < numberBoats; b++) {
      for (int h= 0; h < numberBoats; h++) {
        // occSched[p][h][b] -> hosts[h]
        model.ifThen(occSched[p][h][b], model.arithm(hosts[h],"=",1));
      }
    }
  }
  
  // Don't exceed host capacity
  // forAll p in sched . forAll h in hosts . (sum b in preImage(p,h) . crew(b)) <= capacity(h),
  // Sum of crew sizes, crew[b], of the boats b mapped to host h leq capacity(h)
  // So we are looking for the b such that sched[p][b] = h
  // occ means in this period is this boat assigned to this host.
  // sched[p][b] = h becomes occSched[p][h][b] = 1
  //   Configured like this because we want to get the array of boats assoc with a host
  for (int p = 0; p < numberPeriods; p++) {
    for (int h = 0; h < numberBoats; h++) {
      // NB If h not a host occSched[p][h][b] will be 0 for all b, so wasted scalar but no ifthen.
      model.scalar(occSched[p][h], crew, "<=", capacity[h]).post() ;
    }
  }

  // Socialisation
  // forAll b1,b2 : Boat . b1 < b2 -> (sum p in sched . toInt(p(b1) = p(b2))) <= 1,
  for (int b1 = 0; b1 < numberBoats-1; b1++) {
    for (int b2 = b1 + 1; b2 < numberBoats; b2++) {
      BoolVar[] equalityBools = model.boolVarArray(numberPeriods) ;
      decvarsal.addAll(Arrays.asList(equalityBools));
      
      for (int p = 0; p < numberPeriods; p++) {
        Constraint c = model.arithm(sched[p][b1], "=", sched[p][b2]) ;
        c.reifyWith(equalityBools[p]) ;
      }
      model.sum(equalityBools, "<=", 1).post() ;
    }
  }
  
  // Symmetry Breaking
  if(args.length>4 && args[4].equals("symbreak")) {
      for (int p = 0; p < numberPeriods - 1; p++) {
        model.lexLessEq(sched[p], sched[p+1]).post();
      }
  }
  
  // optimisation variable.
  // Docs says sum of boolvars much faster than intvars.
  IntVar optVar = model.intVar("optVar", 0, numberBoats) ;
  decvarsal.add(optVar);
  
  model.sum(hosts, "=", optVar).post() ;
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
