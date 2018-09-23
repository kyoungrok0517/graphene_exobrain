import com.fasterxml.jackson.core.JsonProcessingException;
import org.lambda3.graphene.core.Graphene;
import org.lambda3.graphene.core.relation_extraction.model.RelationExtractionContent;

import java.io.File;
import java.nio.file.Files;
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

            try {
                String content = new String(Files.readAllBytes(Paths.get(filePath.toString())));
                String json = this.graphene.doRelationExtraction(content, true, false).serializeToJSON();
                String fileName = filePath.getFileName().toString();
                outQueue.add(fileName + '\t' + json);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
