package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

// B4 : lecture des arguments de la ligne de commande.
public final class CliParser {

    private static final Set<String> ACTIONS =
            Set.of("all", "structure", "calls", "metrics", "graph");

    public static final String USAGE = "Usage : java -jar target/hai913i-tp1-analyzer.jar "
            + "PROJET --action all|structure|calls|metrics|graph --threshold X "
            + "[--ast] [--callers ID] [--callees ID]";

    private CliParser() {
    }

    public static CliOptions parse(String[] args) {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            throw new IllegalArgumentException(USAGE);
        }

        Path project = Path.of(args[0]);
        Set<String> actions = new LinkedHashSet<>();
        boolean actionProvided = false;
        boolean thresholdProvided = false;
        int threshold = 0;
        boolean printAst = false;
        String callers = null;
        String callees = null;

        for (int i = 1; i < args.length; i++) {
            String argument = args[i];
            switch (argument) {
                case "--ast" -> printAst = true;
                case "--help", "-h" -> throw new IllegalArgumentException(USAGE);
                case "--action" -> {
                    actions = parseActions(value(args, ++i, argument));
                    actionProvided = true;
                }
                case "--threshold" -> {
                    threshold = parseThreshold(value(args, ++i, argument));
                    thresholdProvided = true;
                }
                case "--callers" -> callers = value(args, ++i, argument);
                case "--callees" -> callees = value(args, ++i, argument);
                default -> {
                    if (argument.startsWith("--action=")) {
                        actions = parseActions(argument.substring("--action=".length()));
                        actionProvided = true;
                    } else if (argument.startsWith("--threshold=")) {
                        threshold = parseThreshold(argument.substring("--threshold=".length()));
                        thresholdProvided = true;
                    } else {
                        throw new IllegalArgumentException("Option inconnue : " + argument + "\n" + USAGE);
                    }
                }
            }
        }

        if (!actionProvided && callers == null && callees == null) {
            throw new IllegalArgumentException("L'action est obligatoire (--action ...)\n" + USAGE);
        }
        if (!thresholdProvided) {
            throw new IllegalArgumentException("Le seuil X est obligatoire (--threshold X)\n" + USAGE);
        }
        if (callers != null || callees != null) {
            actions.add("graph");
        }

        return new CliOptions(project, Set.copyOf(actions), threshold, printAst, callers, callees);
    }

    private static Set<String> parseActions(String text) {
        Set<String> result = new LinkedHashSet<>();
        for (String part : text.split(",")) {
            String action = part.trim().toLowerCase(Locale.ROOT);
            if (action.isEmpty()) {
                continue;
            }
            if (!ACTIONS.contains(action)) {
                throw new IllegalArgumentException("Action inconnue : " + action);
            }
            result.add(action);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("L'action ne peut pas être vide");
        }
        return result;
    }

    private static int parseThreshold(String text) {
        try {
            int result = Integer.parseInt(text);
            if (result < 0) {
                throw new NumberFormatException();
            }
            return result;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Le seuil X doit être un entier positif : " + text);
        }
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException("Valeur manquante pour " + option + "\n" + USAGE);
        }
        return args[index];
    }
}
