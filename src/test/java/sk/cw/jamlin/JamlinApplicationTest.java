package sk.cw.jamlin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JamlinApplicationTest {

    @Test
    public void run_returnsSuccessForValidExtract() {
        CliOptions options = new CliOptions();
        options.workingDirectory = TestResources.TESTDATA.toString();
        options.configPath = TestResources.path("jamlin_config.json");
        options.source = "jamlin_demo.html";
        options.language = "sk";

        int exitCode = new JamlinApplication().run(options);
        assertEquals(0, exitCode);
    }

    @Test
    public void run_returnsErrorWhenConfigMissing() {
        CliOptions options = new CliOptions();
        options.workingDirectory = TestResources.TESTDATA.toString();
        options.configPath = TestResources.path("missing-config.json");

        int exitCode = new JamlinApplication().run(options);
        assertEquals(1, exitCode);
    }

    @Test
    public void resolveMode_specificExtractWhenSourceAndLanguageSet() {
        CliOptions options = new CliOptions();
        options.workingDirectory = TestResources.TESTDATA.toString();
        options.configPath = TestResources.path("jamlin_config.json");
        options.source = "jamlin_demo.html";
        options.language = "en";

        JamlinApplication application = new JamlinApplication();
        application.run(options);

        assertEquals("specific", application.getContext().getMode());
        assertTrue(application.getContext().getAction() == JamlinAction.EXTRACT);
    }
}
