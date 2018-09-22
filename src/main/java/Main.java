import org.lambda3.graphene.core.relation_extraction.model.RelationExtractionContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Path dataDir = Paths.get(args[0]);
        Path finishedDir = Paths.get("./results");
        Stream<Path> inputFiles = null;

        try {
            Set<String> finishedFiles = Files.walk(finishedDir)
//                    .filter(s -> s.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());

            inputFiles = Files.walk(dataDir)
                    .filter(s -> s.toString().endsWith(".txt"))
                    .filter(s -> !finishedFiles.contains(s.getFileName().toString()));

        } catch (IOException e) {
            System.err.println(e);
        }
        
        // Process each file
        RelationExtractor extractor = new RelationExtractor();
        inputFiles.forEach(extractor::process);

//
//        // ### OUTPUT AS RDFNL #####
//        // default
//        String defaultRep = rec.defaultFormat(false); // set **true** for resolved format
//
//        // flat
//        String flatRep = rec.flatFormat(false); // set **true** for resolved format
//
//        // ### OUTPUT AS PROPER RDF (N-Triples) ###
//        String rdf = rec.rdfFormat();
//
//        // ### SERIALIZE & DESERIALIZE ###
//        try {
//            rec.serializeToJSON(new File("file.json"));
//            RelationExtractionContent loaded = rec.deserializeFromJSON(new File("file.json"), RelationExtractionContent.class);
//        } catch (IOException e) {
//            System.err.println(e);
//        }

    }
}
