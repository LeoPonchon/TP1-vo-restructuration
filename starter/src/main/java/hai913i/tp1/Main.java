package hai913i.tp1;

import java.util.List;

import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.cli.CliOptions;
import hai913i.tp1.cli.CliParser;
import hai913i.tp1.debug.PrintAst;
import hai913i.tp1.extract.FactsExtractor;
import hai913i.tp1.graph.CallGraph;
import hai913i.tp1.metrics.MetricsCalculator;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.report.TextReport;

/** Point d'entrée : arguments, parsing JDT, puis délégation aux parties A et B. */
public final class Main {

    private Main() {
    }

    // B4 : une erreur utilisateur ou d'analyse est affichée sans trace technique.
    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        final CliOptions options;
        try {
            options = CliParser.parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Erreur : " + exception.getMessage());
            return 2;
        }

        try {
            ProjectSources sources = ProjectSources.of(options.project());
            if (sources.javaFiles().isEmpty()) {
                throw new IllegalArgumentException("Aucun fichier Java dans : " + options.project());
            }
            List<ParsedFile> files = JdtParser.parse(sources, List.of());

            // A0 : environnement, racine des sources, unités et erreurs JDT.
            int errors = printCompilerProblems(files);
            System.out.println("Racine des sources     : " + sources.sourceRoot());
            System.out.println("Unites de compilation  : " + files.size());
            System.out.println("Erreurs de compilation : " + errors);

            // B1 : le reste du programme manipule uniquement ce modèle de faits.
            ProjectFacts facts = new FactsExtractor().extract(files);

            // A1 : affichage optionnel de l'AST de Dvd.java et Category.java.
            if (options.printAst()) {
                printA1Ast(files);
            }

            if (options.hasAction("structure")) {
                TextReport.structure(facts);
            }
            if (options.hasAction("calls")) {
                TextReport.calls(facts);
            }
            if (options.hasAction("metrics")) {
                // B2 : les métriques ne consultent pas les AST.
                TextReport.metrics(
                        new MetricsCalculator().calculate(facts, options.threshold()),
                        options.threshold());
            }
            if (options.hasAction("graph")) {
                // B3 : graphe construit à partir des appels déjà extraits.
                CallGraph graph = CallGraph.from(facts);
                TextReport.graph(graph, options.callersQuery(), options.calleesQuery());
            }
            return 0;
        } catch (Exception exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }
            System.err.println("Erreur pendant l'analyse : " + message);
            return 1;
        }
    }

    private static int printCompilerProblems(List<ParsedFile> files) {
        int errors = 0;
        for (ParsedFile file : files) {
            for (IProblem problem : file.unit().getProblems()) {
                if (problem.isError()) {
                    errors++;
                    System.err.println(file.path().getFileName() + ":"
                            + problem.getSourceLineNumber() + " " + problem.getMessage());
                }
            }
        }
        return errors;
    }

    private static void printA1Ast(List<ParsedFile> files) {
        System.out.println("// A1 — AST");
        boolean foundValidationFiles = false;
        for (ParsedFile file : files) {
            String path = file.path().toString().replace('\\', '/');
            if (path.endsWith("/library/model/Dvd.java")
                    || path.endsWith("/library/model/Category.java")) {
                foundValidationFiles = true;
                System.out.println(file.path().getFileName());
                file.unit().accept(new PrintAst());
            }
        }
        if (!foundValidationFiles) {
            for (ParsedFile file : files) {
                System.out.println(file.path().getFileName());
                file.unit().accept(new PrintAst());
            }
        }
    }
}
