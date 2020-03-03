
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
import org.chocosolver.solver.exception.*;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Capacitated Vehicle Routing Problem
 */

// Command syntax: java -jar CVRP.jar <instance> <seed> <expbased/propguided> <backtrackLimit> [<timeLimit_in_second>] [symbreak]
// Example: java -jar CVRP.jar instance.lns 123 expbased 280 600

public class CVRP {
    //25+20+30+20+ 20+20 + 10+10 + 25+25
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

    int numberLocations;
    int vehicleCapacity;
    int[] orderWeights;
    int[][] costs;

    String inputFileName = args[0];
    BufferedReader br = new BufferedReader(new FileReader(inputFileName));
    String ln = br.readLine();
    String[] ls = ln.split(" ");
    numberLocations = Integer.parseInt(ls[0]);
    vehicleCapacity = Integer.parseInt(ls[1]);
    orderWeights = new int[numberLocations];
    ln = br.readLine();
    ls = ln.split(" ");
    for (int i=0; i<numberLocations; i++)
        orderWeights[i] = Integer.parseInt(ls[i]);        
    int nNodes = numberLocations + 1;
    costs = new int[nNodes][nNodes];
    for (int i=0; i<nNodes; i++){
        ln = br.readLine();
        ls = ln.split(" ");
        for (int j=0;j<nNodes;j++)
            costs[i][j] = Integer.parseInt(ls[j]);
    }
    
    int maxLinkCost=0;
    for(int i=0; i<=numberLocations; i++) {
        for(int j=0; j<=numberLocations; j++) {
            if(costs[i][j]>maxLinkCost) maxLinkCost=costs[i][j];
        }
    }
    //System.out.println("Max link cost: "+maxLinkCost);
    
    int[] costsFlat=new int[(numberLocations+1)*(numberLocations+1)];
    for(int i=0; i<numberLocations+1; i++) {
        for(int j=0; j<numberLocations+1; j++) {
            costsFlat[i*(numberLocations+1) + j]=costs[i][j];
        }
    }
    
    // Cost of putting each item onto its own vehicle and driving it to its target and back
    int maxTotalCost = 0;
    for(int i=1; i<=numberLocations; i++) {
        maxTotalCost += (2*costs[0][i]);
    }
    
    int totalOrderWeight=0;
    for(int i=1; i<=numberLocations; i++) {
        totalOrderWeight+=orderWeights[i-1];
    }
    
    int minVehicles = (int) Math.ceil( ((double)totalOrderWeight) / ((double)vehicleCapacity) );
    //  Basic type is set of sequences. Outer set is variable-sized explicit, inner
    //  sequence is variable-sized 
    
    Model model = new Model("CVRP");
    
    // Length of each seq. 0 means unused. 
    IntVar[] seqLength=model.intVarArray("seqLength", numberLocations, 0, numberLocations);
    
    //  planZero is same as plan for active parts, 0 for inactive parts. 
    IntVar[][] planZero=new IntVar[numberLocations][];
    for(int i=0; i<numberLocations; i++) {
        planZero[i]=model.intVarArray("planZero["+(i+1)+"]", numberLocations, 0, numberLocations);
        for(int j=0; j<numberLocations; j++) {
            model.ifThenElse(model.arithm(seqLength[i], ">=", j+1),
                model.arithm(planZero[i][j], "!=", 0),
                model.arithm(planZero[i][j], "=", 0));
        }
    }
    
    //  New order weights with 0 at start. 
    int[] orderWts=new int[orderWeights.length+1];
    System.arraycopy(orderWeights, 0, orderWts, 1, orderWeights.length);
    orderWts[0]=0;
    
    //  Capacity restriction on vehicle
    for(int i=0; i<numberLocations; i++) {
        //  Add up order weight in this sequence, <= capacity
        IntVar[] orderwtcumul = model.intVarArray("orderwtcumul["+(i+1)+"]", numberLocations, 0, vehicleCapacity);
        
        for(int j=0; j<numberLocations; j++) {
            model.element(orderwtcumul[j], orderWts, planZero[i][j], 0).post();
        }
        
        //  Sum orderwtcumul to orderWt.  Domain of orderWt
        model.sum(orderwtcumul,"<=", vehicleCapacity).post();
    }
    
    //  Every order must be delivered once
    /*for(int val=1; val<=numberLocations; val++) {
        IntVar[] valAtLoc = new IntVar[numberLocations*numberLocations];
        for(int i=0; i<numberLocations; i++) {
            for(int j=0; j<numberLocations; j++) {
                valAtLoc[i*numberLocations+j]=model.arithm(planZero[i][j], "=", val).reify();
            }
        }
        model.sum(valAtLoc,"=",1).post();
    }*/
    
    //  Put all planZero vars into one 
    IntVar[] planZeroFlat=new IntVar[numberLocations*numberLocations];
    for(int i=0; i<numberLocations; i++) {
        for(int j=0; j<numberLocations; j++) {
            planZeroFlat[i*numberLocations+j]=planZero[i][j];
        }
    }
    //  Deliver each order only once.
    model.allDifferentExcept0(planZeroFlat).post();
    
    if(args.length>5 && args[5].equals("symbreak")) {
        IntVar plan_marker=model.intVar("plan_marker", 1, numberLocations);  // number of used rows
        
        for(int i=0; i<numberLocations; i++) {
            model.ifThenElse(model.arithm(plan_marker,">=",i),
                model.arithm(seqLength[i],">",0),
                model.arithm(seqLength[i],"=",0));
        }
        
        for(int i=1; i<numberLocations; i++) {
            // For each adjacent pair of sequences, if the second sequence is not
            // empty, then post the lex ordering constraint. 
            
            // First put lengths followed by the sequence into some arrays for lexLess. 
            IntVar[] vars1=new IntVar[numberLocations+1];
            IntVar[] vars2=new IntVar[numberLocations+1];
            
            vars1[0]=seqLength[i-1];
            vars2[0]=seqLength[i];
            
            for(int j=0; j<numberLocations; j++) {
                vars1[j+1]=planZero[i-1][j];
                vars2[j+1]=planZero[i][j];
            }
            
            // If row i is in use, then post the constraint between rows i-1 and i. 
            model.ifThen(model.arithm(plan_marker,">=",i),
                model.lexLess(vars1, vars2));
        }
    }
    
    //  Value of the objective. 
    IntVar[] optVarPart=model.intVarArray("optVarPart", numberLocations, 0, maxTotalCost);
    for(int i=0; i<numberLocations; i++) {
        // The cost of travelling to  each element of the sequence starting with 0 (depot) -> first element
        // and ending with last element -> depot. 
        IntVar[] optVarPartinner=model.intVarArray("optVarPartinner["+i+"]", numberLocations+1, 0, maxLinkCost);
        
        //  costs(0, route(0))
        model.ifThenElse(model.arithm(seqLength[i],">",0),
            model.element(optVarPartinner[0], costs[0], planZero[i][0], 0),
            model.arithm(optVarPartinner[0],"=",0));
        
        //  Find the last element of the sequence
        IntVar last=model.intVar("last", 0, numberLocations);
        model.ifThenElse(model.arithm(seqLength[i],">",0),
            model.element(last, planZero[i], model.intOffsetView(seqLength[i], -1), 0),
            model.arithm(last, "=", 0));
        
        //  costs(0, route(|route|))
        model.ifThenElse(model.arithm(seqLength[i],">",0),
            model.element(optVarPartinner[1], costs[0], last),
            model.arithm(optVarPartinner[1],"=",0));
        
        for(int j=1; j<numberLocations; j++) {
            // For each adjacent pair in the sequence, look up the cost. 
            IntVar costidx=model.intVar("costidx", 0, (numberLocations+1)*(numberLocations+1));
            model.arithm(model.intScaleView(planZero[i][j-1], (numberLocations+1)), "+", planZero[i][j],"=",costidx).post();
            
            IntVar tmp=model.intVar("tmpCost", 0, maxLinkCost);
            model.element(tmp, costsFlat, costidx, 0).post();
            
            //  If the pair lies within the active part of the sequence, make it 
            //  equal to optVarPartInner
            model.ifThenElse(model.arithm(seqLength[i],">=",j+1),
                model.arithm(optVarPartinner[j+1], "=", tmp),
                model.arithm(optVarPartinner[j+1], "=", 0));
        }
        
        model.sum(optVarPartinner, "=", optVarPart[i]).post();
    }
    
    IntVar optVar=model.intVar("optVar", 0, maxTotalCost);
    
    model.sum(optVarPart, "=", optVar).post();
    
    //  Implied constraint, sum of route lengths = number locations 
    model.sum(seqLength, "=", numberLocations).post();
    
    model.setObjective(Model.MINIMIZE, optVar);
    
    ////////////////////////////////////////////////////////////////////////////
    
    //  Collect decision variables into one array. 
    IntVar[] decvars=new IntVar[numberLocations*(numberLocations+2)];
    for(int i=0; i<numberLocations; i++) {
        for(int j=0; j<numberLocations; j++) {
            decvars[i*numberLocations+j]=planZero[i][j];
        }
        decvars[numberLocations*numberLocations+i]=seqLength[i];
        decvars[numberLocations*(numberLocations+1)+i]=optVarPart[i];
    }
    
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
        
        /*for(int i=0; i<numberLocations; i++) {
            for(int j=0; j<numberLocations; j++) {
                System.out.println("planZero["+i+"]["+j+"] = "+planZero[i][j].getValue());
            }
        }
        for(int j=0; j<numberLocations; j++) {
            System.out.println("seqLength["+j+"] = "+seqLength[j].getValue());
        }
        for(int j=0; j<numberLocations; j++) {
            System.out.println("optVarPart["+j+"] = "+optVarPart[j].getValue());
        }*/
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
