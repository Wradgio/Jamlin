package sk.cw.jamlin;

import java.util.Date;

public class JamlinRunContext {

    private Config config;
    private String workingDirectory = "";
    private JamlinAction action = JamlinAction.EXTRACT;
    private String source;
    private String target;
    private String language = "";
    private String languageName = "";
    private boolean dictionary = true;
    private String mode = "automatic";
    private Date startupTimestamp;
    private int expectedFilesCount;
    private int exportedFilesCount;
    private TranslationExtractDictionary extractDictionary;
    private boolean failed;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public JamlinAction getAction() {
        return action;
    }

    public void setAction(JamlinAction action) {
        this.action = action;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public boolean isDictionary() {
        return dictionary;
    }

    public void setDictionary(boolean dictionary) {
        this.dictionary = dictionary;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Date getStartupTimestamp() {
        return startupTimestamp;
    }

    public void setStartupTimestamp(Date startupTimestamp) {
        this.startupTimestamp = startupTimestamp;
    }

    public int getExpectedFilesCount() {
        return expectedFilesCount;
    }

    public void setExpectedFilesCount(int expectedFilesCount) {
        this.expectedFilesCount = expectedFilesCount;
    }

    public int getExportedFilesCount() {
        return exportedFilesCount;
    }

    public void setExportedFilesCount(int exportedFilesCount) {
        this.exportedFilesCount = exportedFilesCount;
    }

    public TranslationExtractDictionary getExtractDictionary() {
        return extractDictionary;
    }

    public void setExtractDictionary(TranslationExtractDictionary extractDictionary) {
        this.extractDictionary = extractDictionary;
    }

    public boolean hasFailed() {
        return failed;
    }

    public void markFailed() {
        this.failed = true;
    }

    public void incrementExportedFilesCount() {
        exportedFilesCount++;
    }

    public void decrementExpectedFilesCount() {
        expectedFilesCount--;
    }

    public void multiplyExpectedFilesCount(int factor) {
        expectedFilesCount *= factor;
    }

    public boolean isExtractAction() {
        return action == JamlinAction.EXTRACT;
    }

    public boolean isReplaceAction() {
        return action == JamlinAction.REPLACE;
    }
}
