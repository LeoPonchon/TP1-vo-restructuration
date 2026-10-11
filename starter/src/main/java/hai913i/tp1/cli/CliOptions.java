package hai913i.tp1.cli;

import java.nio.file.Path;
import java.util.Set;

// B4 : options de la ligne de commande.
public record CliOptions(
        Path project,
        Set<String> actions,
        int threshold,
        boolean printAst,
        String callersQuery,
        String calleesQuery) {

    public boolean hasAction(String action) {
        return actions.contains("all") || actions.contains(action);
    }
}
