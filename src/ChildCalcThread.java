import java.util.concurrent.Callable;

public class ChildCalcThread  implements Callable<Node> {
    private Node root, child;
    private TSP tsp;
    private int i, j;

    public ChildCalcThread(TSP tsp, Node root, int i, int j){
        this.tsp = tsp;
        this.root = root;
        this.i = i;
        this.j = j;
    }

    @Override
    public Node call(){
        child = new Node(tsp, root, root.getLevel() + 1, i, j);
        child.setCost(root.getCost() + root.getCostMatrix(i, j) + child.calculateCost());
        return child;
    }
}
