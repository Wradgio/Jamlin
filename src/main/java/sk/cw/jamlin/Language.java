package sk.cw.jamlin;

import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Created by marthol on 09.10.17.
 */
public class Language {

    private String code = "";
    private Locale lang = null;
    private String name = "";

    public Language() {
    }

    public Language(String code) {
        this.setCode(code);
    }

    public Language(String code, String name) {
        this.setCode(code);
        if (this.name.trim().equals("")) {
            this.setName(code);
        } else {
            this.setName(name);
        }
    }


    /**
     *
     * @param code
     */
    private void setCode(String code) {
        this.code = code;
        this.lang = processCode(code);
    }

    /**
     *
     * @param code String
     * @return Locale
     */
    private Locale processCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String trimmed = code.trim();
        if (!trimmed.matches("^[a-zA-Z]{2,3}(_[a-zA-Z0-9]+)*$")) {
            return null;
        }
        Locale lang = Locale.forLanguageTag(trimmed.replace('_', '-'));
        if (!lang.getLanguage().isEmpty() && isValid(lang)) {
            return lang;
        }
        return null;
    }

    private boolean isValid(Locale locale) {
        try {
            String iso3Language = locale.getISO3Language();
            if (iso3Language == null || iso3Language.isEmpty()) {
                return false;
            }
            // Language-only codes (e.g. "en", "sk") have no country component
            if (locale.getCountry() == null || locale.getCountry().isEmpty()) {
                return true;
            }
            String iso3Country = locale.getISO3Country();
            return iso3Country != null && !iso3Country.isEmpty();
        } catch (MissingResourceException e) {
            return false;
        }
    }
    public static boolean checkLangCodeValid(String langCode) {
        Language lang = new Language(langCode);
        if (lang.getLang()!=null) {
            return true;
        }
        return false;
    }


    /**
     *
     * @param filePath
     * @return
     */
    static String getLangCodeFromFilePath(String filePath) {
        String fileNameLang = "";
        String pathSplit[] = filePath.split("-");
        if ( pathSplit.length>0 ) {
            pathSplit = pathSplit[pathSplit.length-1].split("\\.");
            if (pathSplit.length>0) {
                fileNameLang = pathSplit[0];
            }
        }

        return fileNameLang;
    }


    /**
     *
     * @param secondLanguage Language
     * @return boolean
     */
    boolean equalsInValues(Language secondLanguage) {
        if (secondLanguage == null) {
            return false;
        }
        if (!this.getCode().equals(secondLanguage.getCode())) {
            return false;
        }
        if (this.getLang() == null) {
            return secondLanguage.getLang() == null;
        }
        return this.getLang().equals(secondLanguage.getLang());
    }



    public String getCode() {
        return code;
    }

    public Locale getLang() {
        return lang;
    }

    public String getName() {
        return name;
    }
    public void setName (String name) {
        this.name = name;
    }

}
