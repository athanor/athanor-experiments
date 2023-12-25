
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

// Command syntax: java -jar MEB.jar <instance> <seed> <backtrackLimit> [<timeLimit_in_second>]
// Example: java -jar MEB.jar instance.lns 123 280 600

public class MEB {
    
public static void main(String [] args) throws Exception {
    try{
    int seed=Integer.valueOf(args[1]);
    int btlimit=Integer.valueOf(args[2]);

    int timeLimit = 600; //NGUYEN: add this
    if (args.length>3)
        timeLimit = Integer.valueOf(args[3]); //NGUYEN: add this

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
    IntVar[] cost = model.intVarArray("cost", nodes, 1, maxLinkCost);    //  cost of each node's link to parent, except initialNode. 
    IntVar[] depthpar = model.intVarArray("depthpar", nodes, 0, nodes-1);    // indexed by child node
    IntVar optVar = model.intVar("optVar", 0, maxLinkCost*nodes);
    
    ArrayList<IntVar> decvarsal=new ArrayList<>();
    decvarsal.addAll(Arrays.asList(parents));
    decvarsal.addAll(Arrays.asList(depths));
    decvarsal.addAll(Arrays.asList(cost));
    decvarsal.addAll(Arrays.asList(depthpar));
    decvarsal.add(optVar);
    
    ArrayList<IntVar> lnsvarsal=new ArrayList<IntVar>();
    lnsvarsal.addAll(Arrays.asList(parents));
    lnsvarsal.addAll(Arrays.asList(depths));  //  Only these; all others are functional. 
    
    model.arithm(parents[initialNode], "=", initialNode).post();
    model.arithm(cost[initialNode], "=", 1).post();  // Get rid of spurious variable. 
    
    for(int child=0; child<nodes; child++) {
        if(child!=initialNode) {
            model.arithm(parents[child], "!=", child).post();
            
            // Assuming linkCosts is symmetric, i.e. can swap parent/child indexes
            // Index into linkCosts using the parent and store result in 'cost'
            model.element(cost[child], linkCosts[child], parents[child], 0).post();
            //  Constraint !=0 is implicit in domain of 'cost'.
            
            //IntVar depthpar = model.intVar("depthpar"+child, 1, nodes);
            //  Look up depth of the parent: depthpar
            model.element(depthpar[child], depths, parents[child], 0).post();
            
            model.arithm(depths[child], "=", depthpar[child], "+", 1).post();
        }
    }
    
    IntVar[] costmaxs=model.intVarArray("costmaxs", nodes, 0, maxLinkCost);
    for(int parent=0; parent<nodes; parent++) {
        // Take the max of transmitting to its children
        
        IntVar[] costmaxpart=model.intVarArray("costmaxpart["+parent+"]", nodes, 0, maxLinkCost);
        decvarsal.addAll(Arrays.asList(costmaxpart));
        
        for(int poschild=0; poschild<nodes; poschild++) {
            // parents[poschild]!=parent, then costmaxpart will be 0.
            // Otherwise, costmaxpart will be the linkCost of parent to child. 
            IntVar reifvar=model.arithm(parents[poschild], "=", parent).reify();
            decvarsal.add(reifvar);
            model.arithm(reifvar, "*", model.intVar(linkCosts[parent][poschild]), "=", costmaxpart[poschild]).post();
        }
        model.max(costmaxs[parent], costmaxpart).post();
    }
    
    model.sum(costmaxs, "=", optVar).post();
    
    model.setObjective(Model.MINIMIZE, optVar);
    
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
