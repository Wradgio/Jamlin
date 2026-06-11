package sk.cw.jamlin;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigSourceFilterTest {

    private static final String TESTDATA = System.getProperty("user.dir") + File.separator + "testdata";
    private Config config;

    @Before
    public void setUp() throws Exception {
        String jsonConfig = new String(
                Files.readAllBytes(Paths.get(TESTDATA + File.separator + "jamlin_config.json")),
                StandardCharsets.UTF_8);
        config = new Config("file", jsonConfig);
    }

    @Test
    public void findSourceFilterForFile_usesDirectoryConfigWhenFileGlobDoesNotMatch() {
        String sourcePath = TESTDATA + File.separator + "jamlin_demo.html";
        IConfigSourceFilter filter = config.findSourceFilterForFile(sourcePath, TESTDATA);

        assertNotNull(filter);
        assertEquals("", filter.getPath());
    }

    @Test
    public void findSourceFilterForFile_prefersMatchingFilePattern() {
        String jsonConfig = "{"
                + "\"sources\":{"
                + "\"directories\":[{\"path\":\"\",\"extensions\":[\"html\"],\"traverse\":false,"
                + "\"selectors\":{\"dirTexts\":{\"type\":\"text\",\"selector\":\"h1\"}}}],"
                + "\"files\":[{\"path\":\"jamlin_demo.html\","
                + "\"selectors\":{\"fileTexts\":{\"type\":\"text\",\"selector\":\"p\"}}}]"
                + "},"
                + "\"target\":{\"save_history\":false,\"replace_pattern\":\"*-$lang.*\"}"
                + "}";
        Config customConfig = new Config("file", jsonConfig);
        String sourcePath = TESTDATA + File.separator + "jamlin_demo.html";

        IConfigSourceFilter filter = customConfig.findSourceFilterForFile(sourcePath, TESTDATA);
        TranslationConfig translationConfig = customConfig.makeTranslationConfig(sourcePath, null, TESTDATA);

        assertNotNull(filter);
        assertEquals("jamlin_demo.html", filter.getPath());
        assertEquals(1, translationConfig.getSelectors().size());
        assertEquals("fileTexts", translationConfig.getSelectors().get(0).getName());
    }

    @Test
    public void makeTranslationConfig_usesMatchedSourceSelectorsOnly() {
        String sourcePath = TESTDATA + File.separator + "jamlin_demo.html";
        TranslationConfig translationConfig = config.makeTranslationConfig(sourcePath, null, TESTDATA);

        assertEquals(2, translationConfig.getSelectors().size());
    }
}
