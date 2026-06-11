package sk.cw.jamlin;

import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marcel Zúbrik on 30.10.2017.
 */
public class TranslationReplaceResult {
    private Map<String, Document> results = new HashMap<>();
    private List<String> langCodes = new ArrayList<>();
    private String targetPattern = "";

    public TranslationReplaceResult(Map<String, Document> results, List<String> langCodes) {
        this.results = results;
        this.langCodes = langCodes;
    }

    public void addResult(String name, Document result) {
        results.put(name, result);
    }

    public void addResult(String name, Document result, String targetPattern) {
        results.put(name, result);
        this.targetPattern = targetPattern;
    }

    public Document getDocument(String key) {
        if ( results.size()>0 ) {
            return results.get(key);
        }
        return null;
    }

    public String get(String key) {
        if ( results.size()>0 ) {
            return results.get(key).toString();
        }
        return null;
    }

    public int countResults() {
        return results.size();
    }


    public List<String> getLangCodes() {
        return langCodes;
    }

    public String getTargetPattern() {
        return targetPattern;
    }

    public void setTargetPattern(String targetPattern) {
        this.targetPattern = targetPattern;
    }
}
