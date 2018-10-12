import org.lambda3.graphene.core.relation_extraction.model.RelationExtractionContent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
    public static LinkedBlockingQueue<String> inProgressQueue = new LinkedBlockingQueue<>();

    public static void putIntoQueue(String inputFile) {
        try {
            queue.put(inputFile);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void writeOutput(LinkedBlockingQueue<String> outs, Path finishedDir) {
        String[] columns;
        String fname;
        String json;
        String paragraphId;
        String output_path;
        for (String result : outs) {
            columns = result.split("\\t");
            fname = columns[0];
            json = columns[1];
            paragraphId = columns[2];
            output_path = Paths.get(finishedDir.toString(), fname).toString().replace(".txt", ".json");

            // write
            try (FileWriter outWriter = new FileWriter(output_path, true); 
                    PrintWriter outPrinter = new PrintWriter(outWriter)) {
                outPrinter.println(json + '\t' + paragraphId);
                outPrinter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        while (outs.size() > 0) {
            outs.remove();
        }
    }

    public static void writeInProgress(LinkedBlockingQueue<String> inProgress, Path inProgressPath) {
        try (FileWriter queueWriter = new FileWriter(inProgressPath.toString());
                PrintWriter queuePrinter = new PrintWriter(queueWriter)) {
            for (String s : inProgress) {
                queuePrinter.println(s);
            }
            queuePrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteResultFile(Path f) {
        try {
            Files.delete(f);
            System.out.println(f);
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public static void removeInProgressFromResultDir(Path inProgressPath, Path finishedDir) {
        List<String> inProgressFiles = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(inProgressPath.toString())))) {
            // read inProgress file list
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    inProgressFiles.add(line);
                }
            }
            // remove the files from result folder
            System.out.println("Removing the following unfinished files");
            inProgressFiles.stream().map(f -> Paths.get(f).getFileName().toString())
                    .map(f -> f.replace(".txt", ".json")).map(f -> Paths.get(finishedDir.toString(), f))
                    .forEach(Main::deleteResultFile);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Path dataDir = Paths.get(args[0]);
        Path finishedDir = Paths.get(args[1]);
        int numThreads = Integer.parseInt(args[2]);
        Path inProgressPath = Paths.get("./in_progress.txt");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        if (!finishedDir.toFile().exists()) {
            finishedDir.toFile().mkdir();
        }

        // clean unfinished files from the result dir
        removeInProgressFromResultDir(inProgressPath, finishedDir);

        // Process
        try {
            Set<String> finishedFiles = Files.walk(finishedDir).filter(s -> s.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString()).map(f -> f.replace(".json", ".txt"))
                    .collect(Collectors.toSet());
            Stream<String> inputFiles = Files.walk(dataDir).filter(s -> s.toString().endsWith(".txt"))
                    .filter(s -> !finishedFiles.contains(s.getFileName().toString())).map(p -> p.toString());
            System.out.println("Skipping " + finishedFiles.size() + " files.");
            // put into queue
            inputFiles.forEach(Main::putIntoQueue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(new GrapheneWorker(queue, outs, inProgressQueue)));
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    // Save result
                    synchronized (outs) {
                        writeOutput(outs, finishedDir);
                    }
                    // Save in progress list
                    synchronized (inProgressQueue) {
                        writeInProgress(inProgressQueue, inProgressPath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        while (true) {
            boolean allDone = true;
            for (Future<?> future : futures) {
                allDone &= future.isDone();
            }
            if (allDone) {
                break;
            }
            // System.out.println("\n--------------\n\n\nSentences left: " + queue.size() +
            // "\n------------\n");
            try {
                Thread.sleep(20000); // periodically save the result
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Save current outputs
            synchronized (outs) {
                writeOutput(outs, finishedDir);
            }
            // Save in progress list
            synchronized (inProgressQueue) {
                writeInProgress(inProgressQueue, inProgressPath);
            }
        }

        executor.shutdown();

        // Save result
        synchronized (outs) {
            writeOutput(outs, finishedDir);
        }
        // Save in progress list
        synchronized (inProgressQueue) {
            writeInProgress(inProgressQueue, inProgressPath);
        }

        System.out.println("Done");

    }
}
