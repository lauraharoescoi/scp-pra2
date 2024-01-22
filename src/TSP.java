import org.javatuples.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;


public class TSP
{
    //public static final int INF = Integer.MAX_VALUE;
    public static final int INF = -1;
    public static final int CMatrixPading = 3;


    public int DistanceMatrix[][];
    // Priority queue to store live nodes of the search tree
    PriorityQueue<Node> NodesQueue = new PriorityQueue<Node>(Collections.reverseOrder((Node c1, Node c2) -> Integer.compare(c1.getCost(),c2.getCost())));
    //PriorityQueue<Node> NodesQueue = new PriorityQueue<Node>();
    private  int NCities=0;

    private Node Solution=null;

    // Statistics of purged and processed nodes.
    private long PurgedNodes = 0;
    private long ProcessedNodes = 0;
    private CountDownLatch latch;

    // Getters & Setters
    public int getNCities() {
        return NCities;
    }
    public void setNCities(int NCities) {
        this.NCities = NCities;
    }
    public Node getSolution() {
        return Solution;
    }
    public void setSolution(Node solution) {
        Solution = solution;
    }
    public int getDistanceMatrix(int i, int j) { return DistanceMatrix[i][j]; }
    public int[][] getDistanceMatrix() {
        return DistanceMatrix;
    }
    public long getPurgedNodes() { return PurgedNodes; }
    public long getProcessedNodes() { return ProcessedNodes; }

    // Constructors.
    public TSP()
    {
        InitDefaultCitiesDistances();
    }
    public TSP(String citiesPath)
    {
        ReadCitiesFile(citiesPath);
    }


    public void InitDefaultCitiesDistances()
    {
        DistanceMatrix = new int[][]{{INF, 10, 15, 20},
                                    {10, INF, 35, 25},
                                    {15, 35, INF, 30},
                                    {20, 25, 30, INF}};
        NCities = 4;
    }

    public void ReadCitiesFile (String citiesPath)
    {
        Scanner input = null;
        try {
            input = new Scanner(new File(citiesPath));

            // Read the number of cities
            NCities = 0;
            if (input.hasNextInt())
                NCities = input.nextInt();
            else
                System.err.printf("[TSP::ReadCitiesFile] Error reading cities number on %s.\n", citiesPath);

            // Init cities' distances matrix
            DistanceMatrix = new int[NCities][NCities];

            // Read cities distances
            for (int i = 0; i < NCities; ++i) {
                for (int j = 0; j < NCities; ++j) {
                    DistanceMatrix[i][j] = 0;
                    if (input.hasNextInt())
                        DistanceMatrix[i][j] = input.nextInt();
                    else
                        System.err.printf("[TSP::ReadCitiesFile] Error reading distance beetwen cities %d-%d.\n", i, j);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.printf("[TSP::ReadCitiesFile] File %s not found.\n",citiesPath);
            e.printStackTrace();
        } catch (
            IOException e) {
            System.err.printf("[TSP::ReadCitiesFile] Error file reading %s.\n",citiesPath);
            e.printStackTrace();
        }
    }

    public Node Solve()
    {
        Instant start = Instant.now();

        Node solution = Solve(DistanceMatrix);
        printSolution("\nOptimal Solution: ", solution);

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();  //in millis
        System.out.printf("Total execution time: %.3f secs with %d cities.\n", timeElapsed/1000.0,getNCities());

        return solution;
    }

    public void executeMethod(Methods method) throws InterruptedException {
        int numThreads = NCities;
        switch(method) {
            case FIXED_THREAD_POOL:
                useFixedThreadPool(numThreads);
                break;
            case CACHED_THREAD_POOL:
                useCachedThreadPool(numThreads);
                break;
            case FORK_JOIN_POOL:
                useForkJoinPool(numThreads);
                break;
            default:
                throw new IllegalArgumentException("Método de concurrencia no soportado");
        }
    }

    private void useFixedThreadPool(int numThreads) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("\n___________________________________________________________________________________________________________________________________________________");
        System.out.printf("Test with %d cities.\n",getNCities());

        // Create a root node and calculate its cost. The TSP starts from the first city, i.e., node 0
        Node root = new Node(this, DistanceMatrix);
        //System.out.println(root);

        // Calculate the lower bound of the path starting at node 0
        root.calculateSetCost();

        // Add root to the list of live nodes
        pushNode(root);
        if(NodesQueue.size() < numThreads) latch = new CountDownLatch(NodesQueue.size());
        else latch = new CountDownLatch(numThreads);

        // Enviar tareas iniciales al pool
        while (!NodesQueue.isEmpty()) {
            System.out.println("Entro al bucle");
                Node currentNode = popNode();
                executor.submit(() -> {
                    processNode(currentNode);
                    latch.countDown();
                });
                latch.await();
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.printf("\nFinal Total nodes: %d \tProcessed nodes: %d \tPurged nodes: %d \tPending nodes: %d \tBest Solution: %d.",root.getTotalNodes(), ProcessedNodes, PurgedNodes, NodesQueue.size(),getSolution()==null?0:getSolution().getCost());

    }

    private void useCachedThreadPool(int numThreads){
        ExecutorService executor = Executors.newCachedThreadPool();

        for(int i = 0; i < numThreads && !NodesQueue.isEmpty(); ++i){
            Node currentNode = popNode();
            executor.submit(() -> processNode(currentNode));
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void useForkJoinPool(int numThreads){
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        for(int i = 0; i < numThreads && !NodesQueue.isEmpty(); ++i){
            Node currentNode = popNode();
            forkJoinPool.execute(() -> processNode(currentNode));
        }
        forkJoinPool.shutdown();
        try{
            forkJoinPool.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private void processNode(Node node) {
        PriorityQueue<Node> threadQueue = new PriorityQueue<>();
        // Incrementar el contador de nodos procesados.
        synchronized (this) {
            ProcessedNodes++;
        }

        // i almacena el número de la ciudad actual.
        int i = node.getVertex();

        // Si todas las ciudades han sido visitadas, es decir, si es una hoja en el árbol de búsqueda.
        if (node.getLevel() == NCities - 1) {
            // Completa el ciclo retornando a la ciudad de origen.
            node.addPathStep(i, 0);

            // Sincroniza el acceso a la solución actual para compararla y posiblemente actualizarla.
            synchronized (this) {
                if (getSolution() == null || node.getCost() < getSolution().getCost()) {
                    setSolution(node);
                    PurgeWorseNodes(node.getCost());
                }
            }
        } else {
            // Explora las ciudades no visitadas para crear nodos hijos.
            for (int j = 0; j < NCities; j++) {
                System.out.println("Entro a bucle 2");
                if (!node.cityVisited(j) && node.getCostMatrix(i, j) != INF) {
                    Node child = new Node(this, node, node.getLevel() + 1, i, j);
                    int child_cost = node.getCost() + node.getCostMatrix(i, j) + child.calculateCost();
                    if (getSolution() == null || child_cost < getSolution().getCost()) {
                        child.setCost(child_cost);
                        pushNode(child);
                        //threadQueue.add(child);
                        System.out.println("Afegeixo child");
                    }
                    else if (getSolution()!=null && child_cost>getSolution().getCost()) {
                        synchronized (this){
                            PurgedNodes++;
                            System.out.println("Afegeixo purged");
                        }
                    }
                }
                System.out.println("Acabo bucle");
            }
        }
        System.out.println("Acabo funció");
    }


    // Function to solve the traveling salesman problem using Branch and Bound
    public Node Solve(int CostMatrix[][])
    {
        Node min, child;

        // Pop a live node with the least cost, check it is a solution and adds its children to the list of live nodes.
        while ((min=popNode())!=null) // Pop the live node with the least estimated cost
        {
            ProcessedNodes++;
            if (true && (min.getTotalNodes()%10000)==0) System.out.printf("Total nodes: %d \tProcessed nodes: %d \tPurged nodes: %d \tPending nodes: %d \tBest Solution: %d\r",min.getTotalNodes(), ProcessedNodes, PurgedNodes, NodesQueue.size(),getSolution()==null?0:getSolution().getCost());
            if (false && (min.getTotalNodes()%10000)==0)  System.out.println(NodesQueue);
             // i stores the current city number
            int i = min.getVertex();

            // If all cities are visited
            if (min.getLevel() == NCities-1)
            {
                // Return to starting city
                min.addPathStep(i, 0);

                if (getSolution()==null || min.getCost()<getSolution().getCost())
                {   // Found sub-optimal solution
                    setSolution(min);

                    // Remove nodes from Nodes queue that can not improved last found solution
                    PurgeWorseNodes(min.getCost());
                }
            }

            // Do for each child of min (i, j) forms an edge in a space tree
            for (int j = 0; j < NCities; j++)
            {
                // if city is not visited create child node
                if (min.cityVisited(j)==false && min.getCostMatrix(i,j) != INF)
                {

                    // Create a child node and calculate its cost
                    child = new Node(this, min, min.getLevel() + 1, i, j);
                    int child_cost =    min.getCost() + min.getCostMatrix(i,j) +
                                        child.calculateCost();

                    // Add node to pending nodes queue if its costs is lower than better solution
                    if (getSolution()==null || child_cost<getSolution().getCost())
                    {
                        // Add a child to the list of live nodes
                        child.setCost (child_cost);
                        pushNode(child);
                    }
                    else if (getSolution()!=null && child_cost>getSolution().getCost())
                        PurgedNodes++;
                }
            }
        }

        System.out.printf("\nFinal Total nodes: %d \tProcessed nodes: %d \tPurged nodes: %d \tPending nodes: %d \tBest Solution: %d.",min.getTotalNodes(), ProcessedNodes, PurgedNodes, NodesQueue.size(),getSolution()==null?0:getSolution().getCost());

        return getSolution();  // Return solution
    }

    // Add node to the queue of pending processing nodes
    public synchronized void pushNode(Node node)
    {
        NodesQueue.add(node);
    }

    // Remove node from the queue of pending processing nodes
    public Node popNode()
    {
        if (NodesQueue.peek()==null)
            return null;
        else
            return NodesQueue.poll();
    }

    // Purge nodes from the queue whose cost is bigger than the minCost.
    public void PurgeWorseNodes(int minCost)
    {
        int Pending = NodesQueue.size();
        NodesQueue.removeIf(node -> node.getCost()>=minCost);
        PurgedNodes += Pending-NodesQueue.size();
    }

    // Print the solution to console
    public void printSolution(String msg, Node sol) {
        printSolution(System.out, msg, sol);
    }

    // Print the solution to PrintStream
    public void printSolution(PrintStream out, String msg, Node sol) {
        out.print(msg);
        sol.printPath(out, true);
    }
}
