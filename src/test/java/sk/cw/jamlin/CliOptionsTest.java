package sk.cw.jamlin;

import com.beust.jcommander.JCommander;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CliOptionsTest {

    @Test
    public void helpFlag_isRecognized() {
        CliOptions options = new CliOptions();
        JCommander commander = JCommander.newBuilder().addObject(options).build();
        commander.parse("--help");
        assertTrue(options.help);
    }

    @Test
    public void shortHelpFlag_isRecognized() {
        CliOptions options = new CliOptions();
        JCommander commander = JCommander.newBuilder().addObject(options).build();
        commander.parse("-h");
        assertTrue(options.help);
    }

    @Test
    public void dictionary_canBeDisabled() {
        CliOptions options = new CliOptions();
        JCommander commander = JCommander.newBuilder().addObject(options).build();
        commander.parse("--dictionary", "false");
        assertFalse(options.dictionary);
    }

    @Test
    public void actionAndPaths_areParsed() {
        CliOptions options = new CliOptions();
        JCommander commander = JCommander.newBuilder().addObject(options).build();
        commander.parse("--action", "replace", "--source", "a.json", "--target", "b.html", "-l", "sk", "-w", "/tmp");
        assertEquals("replace", options.action);
        assertEquals("a.json", options.source);
        assertEquals("b.html", options.target);
        assertEquals("sk", options.language);
        assertEquals("/tmp", options.workingDirectory);
    }
}
