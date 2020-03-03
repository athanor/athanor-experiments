
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

// Command syntax: java -jar MEB.jar <instance> <seed> <expbased/propguided> <backtrackLimit> [<timeLimit_in_second>]
// Example: java -jar MEB.jar instance.lns 123 expbased 280 600

public class MEB{
    
public static void main(String [] args) throws Exception {
    try{
    int seed=Integer.valueOf(args[1]);
    int btlimit=Integer.valueOf(args[3]);

    int timeLimit = 600; //NGUYEN: add this
    if (args.length>4)
        timeLimit = Integer.valueOf(args[4]); //NGUYEN: add this

    int useInitialisationRestart = 1;
    int useTimeForInitialisation = 0; //if 0, use BacktrackCounter instead of TimeCounter
    int initialisationBacktrackCounterValue = 50; // other option: btlimit
    
    int nodes;
    int initialNode1;
    int[][] linkCosts;
    
    String inputFileName = args[0];
    BufferedReader br = new BufferedReader(new FileReader(inputFileName));
    String ln = br.readLine();
    String[] ls = ln.split(" ");
    nodes = Integer.parseInt(ls[0]);
    initialNode1 = Integer.parseInt(ls[1]);
    linkCosts = new int[nodes][nodes];
    for (int i=0;i<nodes;i++){
        ln = br.readLine();
        String[] lss = ln.split(" ");
        for (int j=0;j<nodes;j++)
            linkCosts[i][j]=Integer.parseInt(lss[j]);
    }
    
    int maxLinkCost=0;
    for(int i=0; i<nodes; i++) {
        for(int j=0; j<nodes; j++) {
            if(linkCosts[i][j]>maxLinkCost) maxLinkCost=linkCosts[i][j];
        }
    }
    //System.out.println("Max link cost: "+maxLinkCost);
    
    int initialNode=initialNode1-1;
    
    assert initialNode>=0 && initialNode<nodes;
    assert linkCosts.length==nodes;
    assert linkCosts[0].length==nodes;
    
    Model model = new Model("MEB");
    
    IntVar[] parents = model.intVarArray("parents", nodes, 0, nodes-1);   // indexed by child node
    IntVar[] depths = model.intVarArray("depths", nodes, 0, nodes-1);    // indexed by node
    IntVar[] cost = model.intVarArray("cost", nodes, 1, maxLinkCost);
    IntVar optVar = model.intVar("optVar", 0, maxLinkCost*nodes);
    
    model.arithm(parents[initialNode], "=", initialNode).post();
    model.arithm(cost[initialNode], "=", 1).post();
    
    for(int child=0; child<nodes; child++) {
        if(child!=initialNode) {
            model.arithm(parents[child], "!=", child).post();
            
            // Assuming linkCosts is symmetric, i.e. can swap parent/child indexes
            // Index into linkCosts using the parent and store result in 'cost'
            model.element(cost[child], linkCosts[child], parents[child], 0).post();
            //  Constraint !=0 is implicit in domain of 'cost'.
            
            IntVar depthpar = model.intVar("depthpar"+child, 1, nodes);
            model.element(depthpar, depths, parents[child], 0).post();
            
            model.arithm(depths[child], ">", depthpar).post();
        }
    }
    
    IntVar[] costmaxs=model.intVarArray("costmaxs", nodes, 0, maxLinkCost);
    for(int parent=0; parent<nodes; parent++) {
        // Take the max of transmitting to its children
        
        IntVar[] costmaxpart=model.intVarArray("costmaxpart["+parent+"]", nodes, 0, maxLinkCost);
        
        for(int poschild=0; poschild<nodes; poschild++) {
            // parents[poschild]!=parent, then costmaxpart will be 0.
            // Otherwise, costmaxpart will be the linkCost of parent to child. 
            IntVar reifvar=model.arithm(parents[poschild], "=", parent).reify();
            model.arithm(reifvar, "*", model.intVar(linkCosts[parent][poschild]), "=", costmaxpart[poschild]).post();
        }
        model.max(costmaxs[parent], costmaxpart).post();
    }
    
    int[] coeffs = new int[nodes];
    for(int i=0; i<nodes; i++) coeffs[i]=1;
    model.scalar(costmaxs, coeffs, "=", optVar).post();
    
    model.setObjective(Model.MINIMIZE, optVar);
    
    ////////////////////////////////////////////////////////////////////////////
    
    //  Collect decision variables into one array. 
    IntVar[] decvars=new IntVar[4*nodes];
    for(int i=0; i<nodes; i++) {
        decvars[i]=parents[i];
        decvars[i+nodes]=depths[i];
        decvars[i+(2*nodes)]=cost[i];
        decvars[i+(3*nodes)]=costmaxs[i];
    }
    //decvars[4*nodes]=optVar;
    
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
