import org.lambda3.graphene.core.Graphene;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import edu.stanford.nlp.util.RuntimeInterruptedException;

public class GrapheneWorker implements Runnable {

    Graphene graphene;
    LinkedBlockingQueue<String> queue;
    LinkedBlockingQueue<String> outQueue;
    LinkedBlockingQueue<String> inProgressQueue;

    public GrapheneWorker(LinkedBlockingQueue<String> queue, LinkedBlockingQueue<String> outQueue,
            LinkedBlockingQueue<String> inProgressQueue) {
        this.graphene = new Graphene();
        this.queue = queue;
        this.outQueue = outQueue;
        this.inProgressQueue = inProgressQueue;
    }

    @Override
    public void run() {
        while (!this.queue.isEmpty()) {
            String filePathStr = queue.remove();
            Path filePath = Paths.get(filePathStr);
            // set the file as in progress
            this.inProgressQueue.add(filePathStr);

            try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath.toString())))) {
                String line;
                String json;
                String fileName;
                while ((line = br.readLine().trim()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    try {
                        json = this.graphene.doRelationExtraction(line, true, false).serializeToJSON();
                        fileName = filePath.getFileName().toString();
                        outQueue.add(fileName + '\t' + json);
                    } catch (RuntimeInterruptedException e) {
                        System.err.println("Error: " + filePath.getFileName().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                br.close();
                // mark the file as done
                this.inProgressQueue.remove(filePathStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
