package sk.cw.jamlin;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainTest {

    private JamlinRunContext runJamlin(CliOptions options) {
        JamlinApplication application = new JamlinApplication();
        application.run(options);
        return application.getContext();
    }

    private CliOptions baseOptions() {
        CliOptions options = new CliOptions();
        options.workingDirectory = TestResources.TESTDATA.toString();
        options.configPath = TestResources.path("jamlin_config.json");
        return options;
    }

    @Test
    public void getFileTranslation_01_extract_specific_dictionary() {
        CliOptions options = baseOptions();
        options.source = "jamlin_demo.html";
        options.language = "sk";

        JamlinRunContext context = runJamlin(options);
        assertEquals(1, context.getExpectedFilesCount());
        assertEquals(context.getExpectedFilesCount(), context.getExportedFilesCount());
    }

    @Test
    public void getFileTranslation_02_extract_specific_withoutDictionary() {
        CliOptions options = baseOptions();
        options.source = "jamlin_demo.html";
        options.language = "sk";
        options.dictionary = false;

        JamlinRunContext context = runJamlin(options);
        assertEquals(1, context.getExpectedFilesCount());
        assertEquals(1, context.getExportedFilesCount());
    }

    @Test
    public void getFileTranslation_03_extract_semiautomatic() {
        CliOptions options = baseOptions();
        options.source = "jamlin_demo.html";

        JamlinRunContext context = runJamlin(options);
        assertEquals(1, context.getExpectedFilesCount());
        assertEquals(context.getExpectedFilesCount(), context.getExportedFilesCount());
    }

    @Test
    public void getFileTranslation_04_replace_specific() {
        CliOptions options = baseOptions();
        options.action = "replace";
        options.source = TestResources.path("fixtures/multilang-extract.json");
        options.target = "jamlin_demo.html";
        options.language = "sk";

        JamlinRunContext context = runJamlin(options);
        assertEquals(1, context.getExpectedFilesCount());
        assertEquals(1, context.getExportedFilesCount());
        assertTrue(TestResources.file("jamlin_demo-sk.html").exists());
    }

    @Test
    public void getFileTranslation_05_replace_semiautomatic() {
        CliOptions options = baseOptions();
        options.action = "replace";
        options.source = TestResources.path("fixtures/multilang-extract.json");
        options.target = "jamlin_demo.html";

        JamlinRunContext context = runJamlin(options);
        assertEquals(3, context.getExpectedFilesCount());
        assertEquals(3, context.getExportedFilesCount());
    }
}
