import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TSP_Conc {

    public static void main(String[] args) throws InterruptedException {
        TSP tsp;
        int numThreads = Runtime.getRuntime().availableProcessors(); // Valor por defecto
        Methods method = Methods.FIXED_THREAD_POOL; // Valor por defecto

        if (args.length != 3) {
            System.err.println("Use: TSP_Conc [<Cities_File>] [<Num_Threads>] [<Concurrent_Method>]");
            return;
        }

        // Leer el archivo de ciudades si está presente
        tsp = (args.length >= 1) ? new TSP(args[0]) : new TSP();

        // Leer el número de hilos si está presente
        if (args.length >= 2) {
            try {
                numThreads = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Number of threads must be an integer. Using 4 threads.");
            }
        }

        // Leer el método de concurrencia si está presente
        if (args.length == 3) {
            try {
                method = Methods.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid concurrence method. Switching to FixedThreadPool.");
            }
        }

        // Ejecutar la versión concurrente del TSP
        tsp.executeMethod(method);
    }

}
