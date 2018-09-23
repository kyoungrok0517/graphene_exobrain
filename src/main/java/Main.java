import org.lambda3.graphene.core.relation_extraction.model.RelationExtractionContent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    public static LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    public static LinkedBlockingQueue<String> outs = new LinkedBlockingQueue<>();

    public static void putIntoQueue(String inputFile) {
        try {
            queue.put(inputFile);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void writeOutput(LinkedBlockingQueue<String> outs, Path finishedDir) {
        for (String result : outs) {
            String[] fnameAndResult = result.split("\t");
            String fname = fnameAndResult[0];
            String json = fnameAndResult[1];
            String output_path = Paths.get(finishedDir.toString(), fname).toString().replace(".txt", ".json");

            // write
            try (FileWriter outWriter = new FileWriter(output_path, true);
                    PrintWriter outPrinter = new PrintWriter(outWriter)) {
                outPrinter.println(json);
                outPrinter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Path dataDir = Paths.get(args[0]);
        Path finishedDir = Paths.get(args[1]);
        int numThreads = Integer.parseInt(args[2]);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        if (!finishedDir.toFile().exists()) {
            finishedDir.toFile().mkdir();
        }

        try {
            Set<String> finishedFiles = Files.walk(finishedDir).filter(s -> s.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString()).collect(Collectors.toSet());
            Stream<String> inputFiles = Files.walk(dataDir).filter(s -> s.toString().endsWith(".txt"))
                    .filter(s -> !finishedFiles.contains(s.getFileName().toString())).map(p -> p.toString());
            // put into queue
            inputFiles.forEach(Main::putIntoQueue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(new GrapheneWorker(queue, outs)));
        }

        // Runtime.getRuntime().addShutdownHook(new Thread() {
        // public void run() {
        // try {
        // synchronized (outs) {
        // FileWriter outWriter = new FileWriter("./all_news_tgt.txt", true);
        // PrintWriter outPrinter = new PrintWriter(outWriter);
        // for (String s : outs) {
        // outPrinter.println(s);
        // }
        // outPrinter.close();
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // }
        // });

        while (true) {
            boolean allDone = true;
            for (Future<?> future : futures) {
                allDone &= future.isDone();
            }
            if (allDone) {
                break;
            }
            System.out.println("\n--------------\n\n\nSentences left: " + queue.size() + "\n------------\n");
            try {
                Thread.sleep(10000); // Once a 1 minute, print the remanining file count
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Save current outputs
            synchronized (outs) {
                writeOutput(outs, finishedDir);
                while (outs.size() > 0) {
                    outs.remove();
                }
            }

        }

        executor.shutdown();

        synchronized (outs) {
            writeOutput(outs, finishedDir);
        }

        System.out.println("Done");

    }
}
