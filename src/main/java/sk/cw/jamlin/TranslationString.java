package sk.cw.jamlin;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marthol on 21.09.17.
 */
public class TranslationString {

    private String stringOrig;
    private String selector;
    private List<TranslationValue> translations = new ArrayList<TranslationValue>();

    TranslationString(String stringOrig, String selector) {
        this.stringOrig = stringOrig;
        this.selector = selector;
    }
    TranslationString(String stringOrig, String selector, String langCode, String value) {
        this.stringOrig = stringOrig;
        this.selector = selector;
        translations.add(new TranslationValue(langCode, value));
    }

    /**
     *
     * @param other TranslationString
     * @return boolean
     */
    boolean equals(TranslationString other) {
        if ( this.selector.equals(other.getSelector()) ) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param langCode String
     * @return int
     */
    public int getTranslationValueByLang(String langCode) {
        if ( getTranslations().size()>0 ) {
            for (int i=0; i<getTranslations().size(); i++) {
                if (getTranslations().get(i).getLangCode().equals(langCode)) {
                    return i;
                }
            }
        }
        return -1;
    }


    int addTranslationValue(String langCode, String translation) {
        TranslationValue translationValue = new TranslationValue(langCode, translation);
        translations.add(translationValue);
        return (translations.size()-1);
    }



    /**
     *
     * @param translate TranslationValue
     * @return int
     */
    public ArrayList<Integer> findTranslates(TranslationValue translate) {
        ArrayList<Integer> foundTranslates = new ArrayList<>();

        if (translations.size()>0) {
            for (int i = 0; i < translations.size(); i++) {
                if ( translations.get(i).getLangCode().equals(translate.getLangCode()) &&
                        translations.get(i).getTranslation().equals(translate.getTranslation()) ) {
                    foundTranslates.add(i);
                }
            }
        }

        return foundTranslates;
    }

    /**
     * Find if translate exists and if not add it
     *
     * @param translate TranslationValue
     * @return boolean
     */
    boolean addTranslation(TranslationValue translate) {
        // find if any occurrence for this path, selector and language exists
        ArrayList<Integer> foundTranslates = findTranslates(translate);

        // if not, add this
        if (foundTranslates.size() <= 0) {
            this.translations.add(translate);
            return true;
        }

        return false;
    }


    public String getStringOrig() {
        return stringOrig;
    }

    public String getSelector() {
        return selector;
    }

    public List<TranslationValue> getTranslations() {
        return translations;
    }

    public void setStringOrig(String stringOrig) {
        this.stringOrig = stringOrig;
    }
}
