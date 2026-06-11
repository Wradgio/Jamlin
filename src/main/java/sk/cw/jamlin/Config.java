package sk.cw.jamlin;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import java.util.Map;

/**
 * Created by marthol on 02.10.17.
 */
public class Config {

    private String type;
    private String input;
    private ConfigTarget target;
    private ConfigSource sources;
    private Language language = null;


    /* CONSTRUCTORS */
    public Config(String type, String input) {
        this.type = type;
        this.input = input;
        this.sources = new ConfigSource();
        parseConfigInput();
    }

    public Config(String type, String input, String langCode) {
        this.type = type;
        this.input = input;
        this.sources = new ConfigSource();
        this.language = new Language(langCode);
        parseConfigInput();
    }

    public Config(String type, String input, String langCode, String langName) {
        this.type = type;
        this.input = input;
        this.sources = new ConfigSource();
        this.language = new Language(langCode, langName);
        parseConfigInput();
    }


    /* METHODS */
    private void parseConfigInput() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(this.input);

        // target
        Boolean saveHistory = JsonPath.read(document, "$.target.save_history");
        String targetReplacePattern = JsonPath.read(document, "$.target.replace_pattern");
        this.target = new ConfigTarget(saveHistory, targetReplacePattern);

        // source Dirs
        int sourceDirectoriesCount = JsonPath.read(document, "$.sources.directories.length()");
        if (sourceDirectoriesCount>0) {
            for (int i = 0; i < sourceDirectoriesCount; i++) {
                String dirPath = JsonPath.read(document, "$.sources.directories[" + i + "].path");
                Boolean dirTraverse = JsonPath.read(document, "$.sources.directories[" + i + "].traverse");
                int lastIndex = this.sources.addConfigSource("directory", dirPath, dirTraverse);
                ConfigSourceFilterDirectory dir = (ConfigSourceFilterDirectory) this.sources.getDirectories().get(lastIndex);
                // get extensions
                int sourceDirExtensionsCount = JsonPath.read(document, "$.sources.directories[" + i + "].extensions.length()");
                if (sourceDirExtensionsCount > 0) {
                    for (int j = 0; j < sourceDirExtensionsCount; j++) {
                        String extension = JsonPath.read(document, "$.sources.directories[" + i + "].extensions[" + j + "]");
                        dir.addExtension(extension);
                    }
                }
                // get selectors
                int sourceDirSelectorsCount = JsonPath.read(document, "$.sources.directories[" + i + "].selectors.length()");
                if (sourceDirSelectorsCount > 0) {
                    Map<String, String> selectors = JsonPath.read(document, "$.sources.directories[" + i + "].selectors");
                    for (Map.Entry<String, String> entry : selectors.entrySet()) {
                        String key = entry.getKey();
                        Map<String, String> selectorData = JsonPath.read(document, "$.sources.directories[" + i + "].selectors." + entry.getKey());
                        String type = "";
                        String selector = "";
                        String attrName = "";
                        String filter = "";
                        for (Map.Entry<String, String> dataEntry : selectorData.entrySet()) {
                            if (dataEntry.getKey().equals("type")) {
                                type = dataEntry.getValue();
                            }
                            if (dataEntry.getKey().equals("selector")) {
                                selector = dataEntry.getValue();
                            }
                            if (dataEntry.getKey().equals("attrName")) {
                                attrName = dataEntry.getValue();
                            }
                            if (dataEntry.getKey().equals("filter")) {
                                filter = dataEntry.getValue();
                            }
                        }
                        // if selector and type are not empty
                        if (!selector.trim().equals("") && !type.trim().equals("")) {
                            if (filter!=null && !filter.trim().equals("")) {
                                // if filter is no empty
                                this.sources.getDirectories().get(lastIndex).addSelector(entry.getKey(), selector, type, attrName, filter);
                            } else if (type.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase()) && !type.trim().equals("")) {
                                // if type is attribute and it's not empty
                                this.sources.getDirectories().get(lastIndex).addSelector(entry.getKey(), selector, type, attrName);
                            } else {
                                IConfigSourceFilter directory = this.sources.getDirectories().get(lastIndex);
                                this.sources.getDirectories().get(lastIndex).addSelector(entry.getKey(), selector, type);
                            }
                        }
                    }
                }
            }
        }
        // source JamlinFiles
        int sourceFilesCount = JsonPath.read(document, "$.sources.files.length()");
        if (sourceFilesCount>0) {
            for (int i = 0; i < sourceFilesCount; i++) {
                String filePath = JsonPath.read(document, "$.sources.files[" + i + "].path");
                int lastIndex = this.sources.addConfigSource("file", filePath);
                Map<String, String> selectors = JsonPath.read(document, "$.sources.files[" + i + "].selectors");
                for (Map.Entry<String, String> entry : selectors.entrySet()) {
                    Map<String, String> selectorData = JsonPath.read(document, "$.sources.files[" + i + "].selectors." + entry.getKey());
                    String type = "";
                    String selector = "";
                    String attrName = "";
                    String filter = "";
                    for (Map.Entry<String, String> dataEntry : selectorData.entrySet()) {
                        if (dataEntry.getKey().equals("type")) {
                            type = dataEntry.getValue();
                        }
                        if (dataEntry.getKey().equals("selector")) {
                            selector = dataEntry.getValue();
                        }
                        if (dataEntry.getKey().equals("attrName")) {
                            attrName = dataEntry.getValue();
                        }
                        if (dataEntry.getKey().equals("filter")) {
                            filter = dataEntry.getValue();
                        }
                    }
                    // if selector and type are not empty
                    if (!selector.trim().equals("") && !type.trim().equals("")) {
                        if (filter!=null && !filter.trim().equals("")) {
                            this.sources.getFiles().get(lastIndex).addSelector(entry.getKey(), selector, type, attrName, filter);
                        } else if (type.equals(TranslationBlock.types.ATTRIBUTE.toString().toLowerCase()) && !type.trim().equals("")) {
                            this.sources.getFiles().get(lastIndex).addSelector(entry.getKey(), selector, type, attrName);
                        } else {
                            this.sources.getFiles().get(lastIndex).addSelector(entry.getKey(), selector, type);
                        }
                    }
                }
            }
        }
    }


    TranslationConfig makeTranslationConfig(String source, String target, String workingDirectory) {
        TranslationConfig translationConfig = new TranslationConfig(source, target, this.target);
        translationConfig.setLanguage(this.language);

        IConfigSourceFilter sourceFilter = findSourceFilterForFile(source, workingDirectory);
        if (sourceFilter != null) {
            translationConfig.selectors.addAll(sourceFilter.getSelectors());
        } else {
            for (int i = 0; i < this.sources.getFiles().size(); i++) {
                translationConfig.selectors.addAll(this.sources.getFiles().get(i).getSelectors());
            }
            for (int i = 0; i < this.sources.getDirectories().size(); i++) {
                translationConfig.selectors.addAll(this.sources.getDirectories().get(i).getSelectors());
            }
        }

        return translationConfig;
    }


    IConfigSourceFilter findSourceFilterForFile(String absoluteSourcePath, String workingDirectory) {
        String relativePath = JamlinFiles.toRelativePath(absoluteSourcePath, workingDirectory);

        for (int i = 0; i < this.sources.getFiles().size(); i++) {
            IConfigSourceFilter fileFilter = this.sources.getFiles().get(i);
            if (JamlinFiles.matchesGlobPath(fileFilter.getPath(), relativePath)) {
                return fileFilter;
            }
        }

        IConfigSourceFilter bestDirectoryMatch = null;
        int bestPathLength = -1;
        for (int i = 0; i < this.sources.getDirectories().size(); i++) {
            IConfigSourceFilter directoryFilter = this.sources.getDirectories().get(i);
            String dirPath = directoryFilter.getPath().replace('\\', '/').replaceAll("/$", "");
            boolean matches;
            if (dirPath.isEmpty()) {
                matches = true;
            } else if (relativePath.equals(dirPath)) {
                matches = true;
            } else {
                matches = relativePath.startsWith(dirPath + "/");
            }

            if (matches && dirPath.length() > bestPathLength) {
                bestPathLength = dirPath.length();
                bestDirectoryMatch = directoryFilter;
            }
        }

        return bestDirectoryMatch;
    }


    /* THE GETTERS & THE SETTERS */

    public String getType() {
        return type;
    }

    public String getInput() {
        return input;
    }

    public ConfigTarget getTarget() {
        return target;
    }

    ConfigSource getSources() {
        return sources;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }
}
