package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import hai913i.tp1.cli.CliOptions;
import hai913i.tp1.cli.CliParser;

// B4 : quelques cas de ligne de commande.
class CliParserTest {

    @Test
    void parsesActionsThresholdAndQueries() {
        CliOptions options = CliParser.parse(new String[] {
                "dossier avec espaces",
                "--action", "metrics,graph",
                "--threshold", "4",
                "--callers", "demo.A#a()"
        });

        assertEquals(Path.of("dossier avec espaces"), options.project());
        assertEquals(4, options.threshold());
        assertEquals("demo.A#a()", options.callersQuery());
        assertTrue(options.hasAction("metrics"));
        assertTrue(options.hasAction("graph"));
    }

    @Test
    void rejectsMissingAndMalformedThreshold() {
        assertThrows(IllegalArgumentException.class, () ->
                CliParser.parse(new String[] {"project", "--action", "metrics"}));
        assertThrows(IllegalArgumentException.class, () ->
                CliParser.parse(new String[] {"project", "--action", "metrics", "--threshold", "abc"}));
    }
}
