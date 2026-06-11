package sk.cw.jamlin;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LanguageTest {

    @Test
    public void checkLangCodeValid_acceptsLanguageOnlyCodes() {
        assertTrue(Language.checkLangCodeValid("en"));
        assertTrue(Language.checkLangCodeValid("sk"));
    }

    @Test
    public void checkLangCodeValid_acceptsLanguageAndCountryCodes() {
        assertTrue(Language.checkLangCodeValid("en_US"));
        assertTrue(Language.checkLangCodeValid("sk_SK"));
    }

    @Test
    public void checkLangCodeValid_rejectsInvalidCodes() {
        assertFalse(Language.checkLangCodeValid(""));
        assertFalse(Language.checkLangCodeValid("not-a-locale"));
    }

    @Test
    public void getLangCodeFromFilePath_extractsCodeBeforeExtension() {
        assertTrue(Language.checkLangCodeValid(Language.getLangCodeFromFilePath("jamlin_demo-sk.html")));
    }

    @Test
    public void equalsInValues_handlesUnknownLanguageCodes() {
        Language first = new Language("xx");
        Language second = new Language("xx");
        assertTrue(first.equalsInValues(second));
    }
}
