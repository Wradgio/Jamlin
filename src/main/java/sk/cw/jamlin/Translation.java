package sk.cw.jamlin;

import com.google.gson.Gson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by marthol on 21.09.17.
 */
public class Translation {

    private TranslationConfig config;
    private String translateAction;
    private enum translateActions {
        EXTRACT, REPLACE
    }
    private Language language;

    /*public Translation(Config config, String translateAction) {
        this.config = config;
        if ( validAction(translateAction) ) {
            this.translateAction = translateAction;
        }
    }*/

    public Translation(TranslationConfig config) {
        this.config = config;
    }

    public boolean validAction(String action) {
        for (translateActions c : translateActions.values()) {
            if (c.name().toLowerCase().equals(action)) {
                return true;
            }
        }
        return false;
    }


    public String extractStrings(String source) {
        this.translateAction = translateActions.EXTRACT.toString().toLowerCase();

        Map<String, String> results = new HashMap<>();
        Document doc = null;
        try {
            doc = Jsoup.parse(source, "UTF-8");
            TranslationExtractResult translationExtractResult = new TranslationExtractResult(config);

            for (int i=0; i<config.getSelectors().size(); i++) {
                String selector = config.getSelectors().get(i).getSelector();
                String selectorName = config.getSelectors().get(i).getName();
                String selectorType = config.getSelectors().get(i).getType();
                String selectorAttrName = config.getSelectors().get(i).getAttrName();
                Elements selectorResult = doc.select(selector);

                TranslationBlock translationBlock = null;
                if (selectorType.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase()) && !selectorAttrName.trim().equals("")) {
                    System.out.println(selectorName +" --- "+ selector +" --- "+ selectorType +" --- "+ selectorAttrName);
                    translationBlock = new TranslationBlock(selectorName, selector, selectorType, selectorAttrName);
                } else {
                    translationBlock = new TranslationBlock(selectorName, selector, selectorType);
                }

                // adding translation strings according to type
                for (int j=0; j<selectorResult.size(); j++) {
                    if (selectorType.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase()) && !selectorAttrName.trim().equals("")) {
                        translationBlock.addTranslationString(selectorResult.get(j).attr(selectorAttrName), selectorResult.get(j).cssSelector());
                    } else if (selectorType.equals("value")) {
                        translationBlock.addTranslationString(selectorResult.get(j).val(), selectorResult.get(j).cssSelector());
                    } else {
                        translationBlock.addTranslationString(selectorResult.get(j).text(), selectorResult.get(j).cssSelector());
                    }
                }
                translationExtractResult.addTranslationBlock(translationBlock);
            }

            // now we have result with all translates - time to export them to json output

            return translationExtractResult.resultToJson();
        } catch (Exception $e) {
            System.out.println($e.getMessage());
            $e.printStackTrace();
        }

        return "{}";
    }


    public TranslationReplaceResult replaceStrings(String extractedJson, String target) {
        this.translateAction = translateActions.REPLACE.toString().toLowerCase();
        extractedJson = extractedJson.trim();
        Gson gson = new Gson();
        TranslationExtractResult extractResult = null;
        try {
            extractResult = gson.fromJson(extractedJson, TranslationExtractResult.class);
            /*System.out.println("------------------");
            System.out.println(extractResult.getTranslationBlocks().get(0).getName());
            System.out.println(extractResult.getTranslationBlocks().get(0).getCssSelector());
            System.out.println(extractResult.getTranslationBlocks().get(0).getType());
            System.out.println(extractResult.getTranslationBlocks().get(0).getAttrName());
            System.out.println("------------------");
            System.out.println("stringOrig: "+ extractResult.getTranslationBlocks().get(0).getTranslationStrings().get(0).getStringOrig());
            System.out.println("selector: "+ extractResult.getTranslationBlocks().get(0).getTranslationStrings().get(0).getSelector());
            System.out.println("langCode: "+ extractResult.getTranslationBlocks().get(0).getTranslationStrings().get(0).getTranslations().get(0).getLangCode());
            System.out.println("translation: "+ extractResult.getTranslationBlocks().get(0).getTranslationStrings().get(0).getTranslations().get(0).getTranslation());*/
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        List<String> langCodes = new ArrayList<>();

        if ( extractResult.getTranslationBlocks().size()>0 ) {
            for (int i = 0; i < extractResult.getTranslationBlocks().size(); i++) {
                for (int j = 0; j < extractResult.getTranslationBlocks().get(i).getTranslationStrings().size(); j++) {
                    for (int k = 0; k < extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().size(); k++) {
                        String langCode = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().get(k).getLangCode();
                        if ( !langCodes.contains( langCode ) ) {
                            langCodes.add(langCode);
                        }
                    }
                }
            }
        }
        System.out.println("lang codes size: "+langCodes.size());

        //doc = Jsoup.parse(target, "UTF-8");
        Map<String, Document> docs = new HashMap<>();
        if (langCodes.size()>0) {
            for (int i=0; i<langCodes.size(); i++) {
                docs.put(langCodes.get(i), Jsoup.parse(target));
            }
        } else {
            docs.put("", Jsoup.parse(target));
        }


        if ( extractResult.getTranslationBlocks().size()>0 ) {

            for ( int i=0; i<extractResult.getTranslationBlocks().size(); i++ ) {
                String activeBlockType = extractResult.getTranslationBlocks().get(i).getType();
                String activeBlockAttr = extractResult.getTranslationBlocks().get(i).getAttrName();
                System.out.println(activeBlockType+" -> "+activeBlockAttr);
                for ( int j=0; j<extractResult.getTranslationBlocks().get(i).getTranslationStrings().size(); j++) {
//                    System.out.println( extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getStringOrig() );
                    for ( int k=0; k<extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().size(); k++) {
                        // get translation string item
                        String translation = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().get(k).getTranslation();
                        String langCode = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().get(k).getLangCode();
                        String selector = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getSelector();
                        String stringOrig = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getStringOrig();
                        Elements selectorResults = docs.get(langCode).select( selector );
                        System.out.println("selector: "+selector);
                        System.out.println("translation: "+translation);
                        System.out.println(activeBlockType);
                        if (selectorResults.size()>0) {
                            System.out.println("( "+i+" / "+j+" / "+k+" )");
                            if (activeBlockType.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase())) {
                                // replace attribute
                                selectorResults.first().attr(activeBlockAttr, translation);
                                System.out.println("Replaced: " +stringOrig+ " -> " +translation);
                            } else if (activeBlockType.equals(TranslationBlock.types.VALUE.toString().toLowerCase())) {
                                // replace value
                                selectorResults.first().val(translation);
                                System.out.println("Replaced: " +stringOrig+ " -> " +translation);
                            } else if (activeBlockType.equals(TranslationBlock.types.TEXT.toString().toLowerCase())) {
                                // replace text
                                selectorResults.first().text(translation);
                                System.out.println("Replaced: " +stringOrig+ " -> " +translation);
                            }
                            System.out.println("--- --- --- --- --- --- --- --- ---");
                        }
                    }
                }
            }

        }

//        System.out.println(docs.get("sk"));
        // prepare file replace pattern as set by config
        TranslationReplaceResult result = null;
        result = new TranslationReplaceResult(docs, langCodes);
        if ( this.config.getTarget().getReplaceFile() ) {
            result.setTargetPattern(this.config.getTarget().getReplacePattern());
        }
        /*
        * 1. use extractResult with translations to generate jsoup selectors
        * 2. use jsoup selectors to check if item exists
        * 3. if item exists, replace content with translation
        * 4. if item doesn't exist, try to find it:
        *    4.1 - search for same string
        *    4.2 - search in parents
        * */
        return result;
    }

    public TranslationConfig getConfig() {
        return config;
    }

    public String getTranslateAction() {
        return translateAction;
    }

    public Language getLanguage() {
        return language;
    }
}
