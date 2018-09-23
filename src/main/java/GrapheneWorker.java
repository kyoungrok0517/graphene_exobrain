import org.lambda3.graphene.core.Graphene;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;

public class GrapheneWorker implements Runnable {

    Graphene graphene;
    LinkedBlockingQueue<String> queue;
    LinkedBlockingQueue<String> outQueue;

    public GrapheneWorker(LinkedBlockingQueue<String> queue, LinkedBlockingQueue<String> outQueue) {
        this.graphene = new Graphene();
        this.queue = queue;
        this.outQueue = outQueue;
    }

    @Override
    public void run() {
        while (!this.queue.isEmpty()) {
            Path filePath = Paths.get(queue.remove());

            try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath.toString())))) {
                String line;
                String json;
                String fileName;
                while ((line = br.readLine().trim()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    json = this.graphene.doRelationExtraction(line, true, false).serializeToJSON();
                    fileName = filePath.getFileName().toString();
                    outQueue.add(fileName + '\t' + json);
                }
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // try {
            // String content = new
            // String(Files.readAllBytes(Paths.get(filePath.toString())));
            // String json = this.graphene.doRelationExtraction(content, true,
            // false).serializeToJSON();
            // String fileName = filePath.getFileName().toString();
            // outQueue.add(fileName + '\t' + json);
            // } catch (JsonProcessingException e) {
            // e.printStackTrace();
            // } catch (Exception e) {
            // e.printStackTrace();
            // }
        }
    }

}
