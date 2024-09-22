import java.util.Random ;

public class SONETGen {
  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("Usage: java SONETGen <nnodes> <nrings> <capacity> <seed>") ;
      return ;
    }
    int nnodes = Integer.parseInt(args[0]) ;
    int nrings = Integer.parseInt(args[1]) ;
    int capacity = Integer.parseInt(args[2]) ;

    // Essence
    Random random = new Random(Integer.parseInt(args[3])) ;
    System.out.println("language Essence 1.3") ;
    System.out.println("letting nnodes be "+nnodes) ;
    System.out.println("letting nrings be "+nrings) ;
    System.out.println("letting capacity be "+capacity) ;
    System.out.println("letting demand be ") ;
    System.out.print("{") ;
    boolean first = true ;
    for (int i = 1; i < nnodes; i++)
      for (int j = i+1; j <= nnodes; j++)
        if (random.nextBoolean()) {
          if (!first)
            System.out.println(",") ;
          else
            first = false ;
          System.out.print("{"+i+", "+j+"}") ;
        }
    System.out.println("}") ;
    
    // Choco
    random = new Random(Integer.parseInt(args[3])) ;
    String className = "SONET"+nnodes+"_"+nrings+"_"+capacity ;    
    // This bit for SONET.java
    System.out.println("else if(args[0].equals(\""+className+"\")) {") ;
    System.out.println("nnodes = "+className+".nnodes ;") ;
    System.out.println("nrings = "+className+".nrings ;") ;
    System.out.println("capacity = "+className+".capacity ;") ;
    System.out.println("demand = "+className+".demand ;") ;
    System.out.println("}") ;
    // This bit for a separate class named SONET<NN>_<NR>_<CAP>
    System.out.println("public final class "+className+" {") ;
    System.out.println("public static final int nnodes = "+nnodes+";") ;
    System.out.println("public static final int nrings = "+nrings+";") ;
    System.out.println("public static final int capacity = "+capacity+";") ;
    System.out.println("public static final int[][] demand = new int[][]{") ;
    first = true ;
    for (int i = 1; i < nnodes; i++)
      for (int j = i+1; j <= nnodes; j++)
        if (random.nextBoolean()) {
          if (!first)
            System.out.println(",") ;
          else
            first = false ;
          System.out.print("{"+i+", "+j+"}") ;
        }
    System.out.println("};") ;
    System.out.println("}") ;
  }
}
