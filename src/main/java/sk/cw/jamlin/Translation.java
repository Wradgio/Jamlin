package sk.cw.jamlin;

import com.google.gson.Gson;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(Translation.class);

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
        if (config.getLanguage()!=null) {
            this.language = config.getLanguage();
        }
    }

    public boolean validAction(String action) {
        for (translateActions c : translateActions.values()) {
            if (c.name().toLowerCase().equals(action)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Extract strings from source - most important part for Extract action
     * @param source String
     * @return TranslationExtractResult
     */
    public TranslationExtractResult extractStrings(String source) {
        this.translateAction = translateActions.EXTRACT.toString().toLowerCase();

        TranslationExtractResult translationExtractResult = new TranslationExtractResult(config);

        Map<String, String> results = new HashMap<>();
        Document doc;
        try {
            doc = Jsoup.parse(source);

            // no user setting, and lang is set by document => overwrite lang action
            String extractedLangCode = doc.select("html").first().attr("lang");
            if ( !extractedLangCode.isEmpty() && getLanguage().toString().isEmpty() ) {
                setLanguage( (new Language(extractedLangCode)) );
            }

            // loop selectors and GET THE MOST IMPORTANT RESULTS OF ALL !!!
            for (int i=0; i<config.getSelectors().size(); i++) {
                String selector = config.getSelectors().get(i).getSelector();
                String selectorName = config.getSelectors().get(i).getName();
                String selectorType = config.getSelectors().get(i).getType();
                String selectorAttrName = config.getSelectors().get(i).getAttrName();
                String filter = config.getSelectors().get(i).getFilter();
                Elements selectorResult = doc.select(selector);

                TranslationBlock translationBlock;
                if (selectorType.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase()) && !selectorAttrName.trim().isEmpty()) {
                    translationBlock = new TranslationBlock(selectorName, selector, selectorType, selectorAttrName);
                } else {
                    translationBlock = new TranslationBlock(selectorName, selector, selectorType);
                }

                // adding translation strings according to type
                for (int j=0; j<selectorResult.size(); j++) {
                    if (language==null || language.getCode().trim().isEmpty()) {
                        language = new Language("xx");
                    }
                    String extractedValue = getExtractedValue(selectorResult, j, selectorType, selectorAttrName);
                    if (!passesFilter(extractedValue, filter)) {
                        continue;
                    }
                    translationBlock.addTranslationString(
                            extractedValue,
                            selectorResult.get(j).cssSelector(),
                            language.getCode(),
                            extractedValue);
                }
                translationExtractResult.addTranslationBlock(translationBlock);
            }

            // now we have result with all translates - time to export them to json output

            return translationExtractResult;//.resultToJson();
        } catch (Exception e) {
            log.error("Failed to extract strings", e);
        }

        return translationExtractResult;
    }


    /**
     *
     * @param extractedJson String
     * @param target String
     * @return TranslationReplaceResult
     */
    TranslationReplaceResult replaceStrings(String extractedJson, String target, JamlinRunContext context) {
        this.translateAction = translateActions.REPLACE.toString().toLowerCase();
        extractedJson = extractedJson.trim();
        Gson gson = new Gson();
        TranslationExtractResult extractResult = null;
        try {
            extractResult = gson.fromJson(extractedJson, TranslationExtractResult.class);
        } catch (Exception e) {
            log.error("Failed to parse extract JSON for replace", e);
            if (context != null) {
                context.markFailed();
            }
        }

        List<String> langCodes = new ArrayList<>();

        if ( language!=null && !language.getCode().trim().isEmpty() ) {
            langCodes.add(language.getCode());
        } else {
            if ( extractResult.getTranslationBlocks()!=null && extractResult.getTranslationBlocks().size()>0) {
                for (int i = 0; i < extractResult.getTranslationBlocks().size(); i++) {
                    for (int j = 0; j < extractResult.getTranslationBlocks().get(i).getTranslationStrings().size(); j++) {
                        for (int k = 0; k < extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().size(); k++) {
                            String langCode = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().get(k).getLangCode();
                            if (!langCodes.contains(langCode)) {
                                langCodes.add(langCode);
                            }
                        }
                    }
                }
            }
        }
        if (context != null && langCodes != null) {
            context.multiplyExpectedFilesCount(langCodes.size());
        }

        //doc = Jsoup.parse(target, "UTF-8");
        Map<String, Document> docs = new HashMap<>();
        if (langCodes.size()>0) {
            for (int i=0; i<langCodes.size(); i++) {
                docs.put(langCodes.get(i), Jsoup.parse(target));
            }
        } else {
            docs.put("", Jsoup.parse(target));
        }


        if ( extractResult.getTranslationBlocks()!=null && extractResult.getTranslationBlocks().size()>0 ) {

            for ( int i=0; i<extractResult.getTranslationBlocks().size(); i++ ) {
                String activeBlockType = extractResult.getTranslationBlocks().get(i).getType();
                String activeBlockAttr = extractResult.getTranslationBlocks().get(i).getAttrName();
                for ( int j=0; j<extractResult.getTranslationBlocks().get(i).getTranslationStrings().size(); j++) {
                    for ( int k=0; k<extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().size(); k++) {
                        // get translation string item
                        String translation = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().get(k).getTranslation();
                        String langCode = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getTranslations().get(k).getLangCode();
                        String selector = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getSelector();
                        String stringOrig = extractResult.getTranslationBlocks().get(i).getTranslationStrings().get(j).getStringOrig();
                        Document document = docs.get(langCode);
                        if (document!=null) {
                            Elements selectorResults = document.select(selector);
                            if (selectorResults.size() > 0) {
                                if (activeBlockType.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase())) {
                                    // replace attribute
                                    selectorResults.first().attr(activeBlockAttr, translation);
                                } else if (activeBlockType.equals(TranslationBlock.types.VALUE.toString().toLowerCase())) {
                                    // replace value
                                    selectorResults.first().val(translation);
                                } else if (activeBlockType.equals(TranslationBlock.types.TEXT.toString().toLowerCase())) {
                                    // replace text
                                    selectorResults.first().text(translation);
                                }
                            }
                        }
                    }
                }
            }

        }

        // prepare file replace pattern as set by config
        TranslationReplaceResult result = null;
        result = new TranslationReplaceResult(docs, langCodes);
        if (this.config.getTarget().getReplacePattern() != null
                && !this.config.getTarget().getReplacePattern().isEmpty()) {
            result.setTargetPattern(this.config.getTarget().getReplacePattern());
        }

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

    public void setLanguage(Language language) {
        this.language = language;
    }


    private static String getExtractedValue(Elements elements, int index, String selectorType, String selectorAttrName) {
        if (selectorType.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase()) && !selectorAttrName.trim().isEmpty()) {
            return elements.get(index).attr(selectorAttrName);
        } else if (selectorType.equals(TranslationBlock.types.VALUE.toString().toLowerCase())) {
            return elements.get(index).val();
        }
        return elements.get(index).text();
    }


    private static boolean passesFilter(String value, String filter) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }
        return value.matches(filter);
    }
}
