package sk.cw.jamlin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TranslationTest {

    private TranslationConfig baseConfig() {
        ConfigTarget target = new ConfigTarget(false, "*-$lang.*");
        TranslationConfig config = new TranslationConfig("sample.html", null, target);
        config.setLanguage(new Language("en"));
        return config;
    }

    @Test
    public void extractStrings_collectsTextAndAttributeValues() {
        String html = "<html><body><p>Hello world</p><a id=\"tip\" title=\"Tooltip\">Link</a></body></html>";
        TranslationConfig config = baseConfig();
        config.selectors.add(new ConfigSourceFilterSelector("texts", "p", "text"));
        config.selectors.add(new ConfigSourceFilterSelector("params", "a[title]", "attribute", "title"));
        Translation translation = new Translation(config);

        TranslationExtractResult result = translation.extractStrings(html);

        assertEquals(2, result.getTranslationBlocks().size());
        assertEquals("Hello world",
                result.getTranslationBlocks().get(0).getTranslationStrings().get(0).getStringOrig());
        assertEquals("Tooltip",
                result.getTranslationBlocks().get(1).getTranslationStrings().get(0).getStringOrig());
        assertEquals("en",
                result.getTranslationBlocks().get(0).getTranslationStrings().get(0).getTranslations().get(0).getLangCode());
    }

    @Test
    public void extractStrings_appliesRegexFilter() {
        String html = "<html><body><span>123</span><span>abc</span></body></html>";
        TranslationConfig config = baseConfig();
        config.selectors.add(new ConfigSourceFilterSelector("nums", "span", "text", "", "^\\d+$"));

        Translation translation = new Translation(config);
        TranslationExtractResult result = translation.extractStrings(html);

        assertEquals(1, result.getTranslationBlocks().get(0).getTranslationStrings().size());
        assertEquals("123", result.getTranslationBlocks().get(0).getTranslationStrings().get(0).getStringOrig());
    }

    @Test
    public void replaceStrings_writesRequestedLanguage() throws Exception {
        String extractJson = TestResources.read("jamlin_demo-extract.json");
        String templateHtml = TestResources.read("jamlin_demo.html");

        ConfigTarget target = new ConfigTarget(false, "*-$lang.*");
        TranslationConfig config = new TranslationConfig(
                TestResources.path("jamlin_demo-extract.json"),
                TestResources.path("jamlin_demo.html"),
                target);
        config.setLanguage(new Language("sk"));
        config.selectors.add(new ConfigSourceFilterSelector("texts", "p, a", "text"));

        JamlinRunContext context = new JamlinRunContext();
        context.setExpectedFilesCount(1);

        Translation translation = new Translation(config);
        TranslationReplaceResult result = translation.replaceStrings(extractJson, templateHtml, context);

        assertEquals(1, result.getLangCodes().size());
        assertEquals("sk", result.getLangCodes().get(0));
        assertTrue(result.get("sk").contains("Menu"));
    }

    @Test
    public void replaceStrings_collectsAllLanguagesWhenNoneSpecified() throws Exception {
        String extractJson = TestResources.read("jamlin_demo-extract.json");
        String templateHtml = TestResources.read("jamlin_demo.html");

        ConfigTarget target = new ConfigTarget(false, "*-$lang.*");
        TranslationConfig config = new TranslationConfig(
                TestResources.path("jamlin_demo-extract.json"),
                TestResources.path("jamlin_demo.html"),
                target);
        config.selectors.add(new ConfigSourceFilterSelector("texts", "p, a", "text"));

        JamlinRunContext context = new JamlinRunContext();
        context.setExpectedFilesCount(1);

        Translation translation = new Translation(config);
        TranslationReplaceResult result = translation.replaceStrings(extractJson, templateHtml, context);

        assertTrue(result.getLangCodes().size() >= 2);
        assertFalse(result.getLangCodes().isEmpty());
    }
}
