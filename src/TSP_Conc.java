import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TSP_Conc {

    public static void main(String[] args) throws InterruptedException {
        TSP tsp = new TSP();
        int numThreads = Runtime.getRuntime().availableProcessors(); // Valor por defecto
        Methods method = Methods.FIXED_THREAD_POOL; // Valor por defecto


        if (args.length != 3) {
            System.err.println("Use: TSP_Conc [<Cities_File>] [<Num_Threads>] [<Concurrent_Method>]");
            return;
        }

        if (args.length == 1) {
            try {
                tsp = new TSP(args[0], numThreads, method);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid concurrence method. Switching to FixedThreadPool.");
            }
        }

        // Leer el número de hilos si está presente
        if (args.length == 2) {
            try {
                numThreads = Integer.parseInt(args[1]);
                tsp = new TSP(args[0], numThreads, method);
            } catch (NumberFormatException e) {
                System.err.println("Number of threads must be an integer. Using 4 threads.");
            }
        }

        // Leer el método de concurrencia si está presente
        if (args.length == 3) {
            try {
                numThreads = Integer.parseInt(args[1]);
                method = Methods.valueOf(args[2].toUpperCase());
                tsp = new TSP(args[0], numThreads, method);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid concurrence method. Switching to FixedThreadPool.");
            }
        }

        // Ejecutar la versión concurrente del TSP
        tsp.Solve_Conc();
    }

}
