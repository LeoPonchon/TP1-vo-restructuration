package hai913i.tp1.extract;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser.ParsedFile;

public final class FactsExtractor {

    // B1 : construction du modèle utilisé par B2 et B3.
    public ProjectFacts extract(List<ParsedFile> files) {
        StructureExtractor structureExtractor = new StructureExtractor();
        CallsExtractor callsExtractor = new CallsExtractor();

        return new ProjectFacts(
                structureExtractor.extract(files),
                callsExtractor.extract(files),
                countApplicationLines(files));
    }

    // B2 : les métriques n'ont plus besoin de relire les sources.
    private int countApplicationLines(List<ParsedFile> files) {
        int total = 0;
        for (ParsedFile file : files) {
            try {
                total += Files.readAllLines(file.path()).size();
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Impossible de compter les lignes de " + file.path(), exception);
            }
        }
        return total;
    }
}
