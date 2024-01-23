package sk.cw.jamlin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marcel Zúbrik on 26.3.2018.
 */
public class TranslationExtractDictionary {

    private ArrayList<TranslationExtractDictionaryRecord> records;

    Map<String, String> languages = new HashMap<>();

    TranslationExtractDictionary(ArrayList<TranslationExtractDictionaryRecord> records) {
        this.records = records;
    }

    // METHODS

    /**
     *
     * @param phrase String
     * @return ArrayList<TranslationExtractDictionaryRecord>
     */
    private ArrayList<Integer> findRecordByPhrase(String phrase, Language language) {
        ArrayList<Integer> found = new ArrayList<>();

        if (records!=null) {
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i) != null && records.get(i).getPhrase() != null &&
                        records.get(i).getPhrase().equals(phrase) && records.get(i).getLanguage() != null &&
                        records.get(i).getLanguage().equalsInValues(language)) {
                    found.add(i);
                }
            }
        }

        return found;
    }

    /**
     *
     * @param language Language
     * @param path String
     * @param block TranslationBlock
     * @return boolean
     */
    private boolean addRecord(String phrase, String selector, Language language, String path, TranslationBlock block) {
        // find if any record for this phrase and language exists
        ArrayList<Integer> foundRecords = findRecordByPhrase(phrase, language);
        // create occurrence of path & related block
        TranslationExtractDictionaryOccurrence occurrence = new TranslationExtractDictionaryOccurrence(phrase, path, block);

        if (foundRecords.size()>0) {

            // loop found records and find if they have this occurrence
            for (Integer foundRecordIndex : foundRecords) {
                TranslationExtractDictionaryRecord record = this.records.get(foundRecordIndex);
                // if no occurrence found, add it
                if (record.findOccurrences(occurrence).size() < 1) {
                    boolean occurrenceAdded = record.addOccurrence(occurrence);
                    // update record
                    if (occurrenceAdded) {
                        this.records.set(foundRecordIndex, record);
                        return true;
                    }
                }
            }

        } else {
            TranslationExtractDictionaryRecord record = new TranslationExtractDictionaryRecord(language, phrase, occurrence);
            this.records.add(record);
            return true;
        }

        return false;
    }


    void addRecords(Language language, String path, TranslationExtractResult extractResult) {
        //Loop translation blocks from result, get its data and translation strings array, put in into TranslationExtractDictionaryOccurrence
        if (extractResult.getTranslationBlocks().size()>0) {
            for (TranslationBlock block: extractResult.getTranslationBlocks()) {
                for (TranslationString phrase: block.getTranslationStrings()) {
                    this.addRecord(phrase.getStringOrig(), phrase.getSelector(), language, path, block);
                }
            }
        }
        this.languages = this.getDictionaryLanguages();
    }

    /**
     *
     * @param oldDictionary TranslationExtractDictionary
     * @return TranslationExtractDictionary
     */
    TranslationExtractDictionary mergeOldDictionary(TranslationExtractDictionary oldDictionary) {
        // loop new record (html) to update its records - old records (json) that don't match are not merged and will be removed
        for (int i = 0; i < this.getRecords().size(); i++) {
            String phrase = this.getRecords().get(i).getPhrase();
            Language language = this.getRecords().get(i).getLanguage();
            // get indexes of old records (json) with same phrase as new one (html)
            ArrayList<Integer> oldRecordsMatchIndexes = oldDictionary.findRecordByPhrase(phrase, language);
            if ( oldRecordsMatchIndexes.size() > 0 ) { // found some records
                // loop found old records' indexes (json) and add their translations to new records (html)
                for (Integer j: oldRecordsMatchIndexes) {
                    ArrayList<TranslationValue> translates = oldDictionary.getRecords().get(j).getTranslates();
                    if ( translates.size() > 0 ) {
                        for (TranslationValue translate: translates) {
                            // adding translates
                            this.getRecords().get(i).addTranslate(translate);
                            // add translates to occurrences
                            for (int o = 0; o < this.getRecords().get(i).getOccurrences().size(); o++) {
                                for (int ts = 0; ts < this.getRecords().get(i).getOccurrences().get(o).getTranslationStrings().size(); ts++) {
                                    this.getRecords().get(i).getOccurrences().get(o).getTranslationStrings().get(ts).addTranslation(translate);
                                }
                            }
                        }
                    }

                }
            }
        }

        return this;
    }


    /**
     *
     * @return Map<String, String>
     */Map<String, String> getDictionaryLanguages() {
        Map<String, String> languages = new HashMap<>();

        this.records.forEach(r -> {
            if (!languages.containsKey(r.getLanguage().getCode())) {
                languages.put(r.getLanguage().getCode(), r.getLanguage().getCode());
            }
            r.getOccurrences().forEach(o -> {
                o.getTranslationStrings().forEach(ts -> {
                    ts.getTranslations().forEach(t -> {
                        if (!languages.containsKey(t.getLangCode())) {
                            languages.put(t.getLangCode(), t.getLangCode());
                        }
                    });
                });
            });
        });

        return languages;
    }



    // GETTERS & SETTERS

    public ArrayList<TranslationExtractDictionaryRecord> getRecords() { return records; }

    public void setRecords(ArrayList<TranslationExtractDictionaryRecord> records) { this.records = records; }

}
