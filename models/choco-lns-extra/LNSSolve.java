
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

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

public class LNSSolve {
    
public static void solve(ArrayList<IntVar> decvarsal, ArrayList<IntVar> lnsvarsal, IntVar optVar, int seed, int timeLimit, int btlimit, Model model, int useInitialisationRestart, int useTimeForInitialisation, int initialisationBacktrackCounterValue) {
    IntVar[] decvars = new IntVar[decvarsal.size()];
    decvars = decvarsal.toArray(decvars);
    
    IntVar[] lnsvars = new IntVar[lnsvarsal.size()];
    lnsvars = lnsvarsal.toArray(lnsvars);
    
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
    }
    else {
        //System.out.println("No first solution found.");
        System.exit(0);
    }
    
    //   Type of LNS neighbourhood
    INeighbor in =  ChocoConfig.build(lnsvars);
    
    in.init(); // Should this be necessary?
    
    //   Type of search within LNS neighbourhoods
    Move innersearch=ChocoConfig.makeLNSMove(lnsvars, seed);
    
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
}
}