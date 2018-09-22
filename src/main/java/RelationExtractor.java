import com.typesafe.config.ConfigException;
import org.lambda3.graphene.core.Graphene;
import org.lambda3.graphene.core.relation_extraction.model.RelationExtractionContent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RelationExtractor {
    Graphene graphene;
    String output_dir = "./results";

    public RelationExtractor() {

        this.graphene = new Graphene();

        File directory = new File(this.output_dir);
        if (! directory.exists()){
            directory.mkdir();
        }
    }

    public void process(Path file)

    {
        System.out.println("Processing: " + file.toString());

        // Process
        try {
            String content = new String(Files.readAllBytes(Paths.get(file.toString())));
            RelationExtractionContent res = this.graphene.doRelationExtraction(content, true, false);
            String output_file = Paths.get(this.output_dir, file.getFileName().toString()).toString().replace(".txt", ".json");
            res.serializeToJSON(new File(output_file));
        } catch (IOException e) {
            System.err.println(e);
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            return;
        }

    }
}
