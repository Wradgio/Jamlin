package sk.cw.jamlin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TranslationExtractResultTest {

    @Test
    public void mergeTwoResults_updatesExistingTranslationValue() {
        TranslationBlock block = new TranslationBlock("texts", "p", "text");
        int stringId = block.addTranslationString("Hello", "#greet", "en", "Hello");
        block.getTranslationStrings().get(stringId).addTranslationValue("sk", "Ahoj");

        TranslationExtractResult oldResult = new TranslationExtractResult();
        oldResult.addTranslationBlock(block);

        TranslationBlock updatedBlock = new TranslationBlock("texts", "p", "text");
        updatedBlock.addTranslationString("Hello", "#greet", "sk", "Updated SK");

        TranslationExtractResult newResult = new TranslationExtractResult();
        newResult.addTranslationBlock(updatedBlock);

        TranslationExtractResult merged = TranslationExtractResult.mergeTwoResults(oldResult, newResult);

        assertEquals(1, merged.getTranslationBlocks().size());
        assertEquals(1, merged.getTranslationBlocks().get(0).getTranslationStrings().size());
        TranslationString mergedString = merged.getTranslationBlocks().get(0).getTranslationStrings().get(0);
        int skIndex = mergedString.getTranslationValueByLang("sk");
        assertEquals("Updated SK", mergedString.getTranslations().get(skIndex).getTranslation());
    }

    @Test
    public void mergeTwoResults_insertsNewTranslationBlock() {
        TranslationExtractResult oldResult = new TranslationExtractResult();

        TranslationBlock newBlock = new TranslationBlock("params", "a[title]", "attribute", "title");
        newBlock.addTranslationString("Tip", "a.tip", "en", "Tip");

        TranslationExtractResult newResult = new TranslationExtractResult();
        newResult.addTranslationBlock(newBlock);

        TranslationExtractResult merged = TranslationExtractResult.mergeTwoResults(oldResult, newResult);

        assertEquals(1, merged.getTranslationBlocks().size());
        assertEquals("params", merged.getTranslationBlocks().get(0).getName());
    }

    @Test
    public void getExtractResultLanguages_returnsDistinctLanguageCodes() {
        TranslationBlock block = new TranslationBlock("texts", "p", "text");
        block.addTranslationString("One", "#one", "en", "One");
        block.addTranslationString("Two", "#two", "sk", "Dva");

        TranslationExtractResult result = new TranslationExtractResult();
        result.addTranslationBlock(block);

        assertTrue(result.getExtractResultLanguages().contains("en"));
        assertTrue(result.getExtractResultLanguages().contains("sk"));
        assertEquals(2, result.getExtractResultLanguages().size());
    }
}
