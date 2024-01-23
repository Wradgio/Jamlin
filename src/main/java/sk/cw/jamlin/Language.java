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
        if ( !code.trim().isEmpty() ) {
            String[] codes = code.split("_");
            Locale lang = null;
            switch (codes.length) {
                case 3:
                    lang = new Locale(codes[0], codes[1], codes[2]);
                    break;
                case 2:
                    lang = new Locale(codes[0], codes[1]);
                    break;
                case 1:
                    lang = new Locale(code);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid locale: " + code);
            }
            if (!lang.toString().isEmpty()) {
                if (isValid(lang)) {
                    return lang;
                }
            }
        }
        return null;
    }

    private boolean isValid(Locale locale) {
        try {
            return locale.getISO3Language() != null && locale.getISO3Country() != null;
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
        return (this.getCode().equals(secondLanguage.getCode()) && this.getLang().equals(secondLanguage.getLang()) );
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
