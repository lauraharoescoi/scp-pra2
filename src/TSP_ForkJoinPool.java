import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class TSP_ForkJoinPool extends RecursiveTask<Node> {
    private Node root;
    private TSP tsp;

    public TSP_ForkJoinPool(TSP tsp, Node root) {
        this.tsp = tsp;
        this.root = root;
    }

    @Override
    protected Node compute() {
        // Si el nivel es igual al número de ciudades - 1, es una hoja en el árbol de búsqueda
        if (root.getLevel() == tsp.getNCities() - 1) {
            root.addPathStep(root.getVertex(), 0);  // Agregar el paso para volver a la ciudad de origen
            return root;  // Este nodo ahora representa un recorrido completo
        }

        List<TSP_ForkJoinPool> tasks = new ArrayList<>();

        // Crear sub-tareas para cada ciudad no visitada
        for (int j = 0; j < tsp.getNCities(); j++) {
            if (!root.cityVisited(j) && root.getCostMatrix(root.getVertex(), j) != tsp.INF) {
                Node child = new Node(tsp, root, root.getLevel() + 1, root.getVertex(), j);
                child.setCost(root.getCost() + root.getCostMatrix(root.getVertex(), j) + child.calculateCost());

                if (tsp.getSolution() == null || child.getCost() < tsp.getSolution().getCost()) {
                    TSP_ForkJoinPool task = new TSP_ForkJoinPool(tsp, child);
                    tasks.add(task);
                    task.fork();
                }
            }
        }

        Node bestSolution = null;

        // Esperar a que todas las tareas finalicen y obtener la mejor solución
        for (TSP_ForkJoinPool task : tasks) {
            Node solution = task.join();
            if (solution != null && (bestSolution == null || solution.getCost() < bestSolution.getCost())) {
                bestSolution = solution;
            }
        }

        // Sincronizar el acceso a la solución global
        synchronized (tsp) {
            if (tsp.getSolution() == null || (bestSolution != null && bestSolution.getCost() < tsp.getSolution().getCost())) {
                tsp.setSolution(bestSolution);
            }
        }

        return bestSolution;
    }
}
