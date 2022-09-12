package sk.cw.jamlin;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Main class for command line
 *
 */
public class Main
{
    @Parameter(names={"--action", "-a"})
    public static String action;
    @Parameter(names={"--source", "-s"})
    public static String source;
    @Parameter(names={"--target", "-t"})
    public static String target;
    @Parameter(names={"--language", "-l"})
    public static String language = "";
    @Parameter(names={"--languageName", "-ln"})
    public static String languageName = "";
    @Parameter(names={"--dictionary", "-d"})
    public static boolean dictionary = true;
    @Parameter(names={"--workingdir", "-w"})
    private static String wd;
    @Parameter(names={"--config", "-c"})
    private static String configString;

    static int expectedFilesCount = 0;
    static int exportedFilesCount = 0;
    private static String mode = "";
    static Date startupTimestamp = null;
    static TranslationExtractDictionary extractDictionary;

    static String workingDirectory = "";
    public static Config config;
    enum actions {
        EXTRACT, REPLACE
    }

    /**
     * main function for command line - it all starts here
     * @param argv arguments from command line
     */
    public static void main(String ... argv)
    {
        if ( wd!=null && !wd.isEmpty() ) {
            workingDirectory = wd;
        } else {
            workingDirectory = System.getProperty("user.dir");
        }

        // build command line params
        Main main = new Main();
        JCommander.Builder builder = new JCommander.Builder();
        builder.addObject(main).build().parse(argv);
        main.run();

        startupTimestamp = new Date();
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // check if config exists and fill its object
        if ( configString!=null && !configString.isEmpty() ) {
            config = getConfig(configString);
        } else {
            File configFile = new File(workingDirectory + File.separator + "jamlin_config.json");
            if (configFile.exists()) {
                config = getConfig(workingDirectory + File.separator + "jamlin_config.json");
            }
        }

        // if config exists, get into translating
        if (config!=null) {
            //getFileTranslation(config, action, source, target);
            handleFileTranslations();
        } else {
            System.out.println("Main Error: no config");
        }
    }


    private void run() {
    }


    /**
     * Get config object from file path
     * @param configFilePath String
     * @return Config
     */
    static Config getConfig(String configFilePath) {
        try {
            String jsonConfig = new String ( Files.readAllBytes( Paths.get(configFilePath) ), StandardCharsets.UTF_8 );
            if (language.trim().isEmpty()) {
                return new Config("file", jsonConfig);
            } else {
                return new Config("file", jsonConfig, language, languageName);
            }
        } catch (IOException e) {
            System.out.println("Main IOException: "+e.getMessage());
        } catch (Exception e) {
            System.out.println("Main Exception: "+e.getMessage());
        }

        return null;
    }


    /**
     * List all files and then do the active translation action with them:
     * - if extract, get strings according to config and create *-extract.json file
     * - if replace, look for related *-extract.json file and replace strings
     */
    static void handleFileTranslations() {
        // get action
        if ( action!=null && !action.isEmpty() && validAction(action) ) {
            action = action.trim().toLowerCase();
        } else {
            action = "extract";// extract | replace;
        }

        // get all files (their paths) that match config options
        List<String> extensions = new ArrayList<String>();
        List<String> dirs = new ArrayList<String>();
        for (int i=0; i<config.getSources().getDirectories().size(); i++) {
            // action EXTRACT specific - if config sets that only specific directories should be extracted
            ConfigSourceFilterDirectory dir = (ConfigSourceFilterDirectory) config.getSources().getDirectories().get(i);
            String path = dir.getPath();
            // there are paths, add to directories to list
            if (!path.trim().isEmpty()) {
                dirs.add(path);
            }
            // list all extension
            for (int j=0; j<((ConfigSourceFilterDirectory) config.getSources().getDirectories().get(i)).getExtensions().size(); j++) {
                String extension = ((ConfigSourceFilterDirectory) config.getSources().getDirectories().get(i)).getExtensions().get(j);
                extensions.add( extension );
            }
        }

        // get mode accoding to settings
        mode = getMode();

        // get result files
        List<String> resultFiles = new ArrayList<String>();

        // action EXTRACT, defined dirs - do not extract from all files
        List<String> sourceExtractFiles = new ArrayList<String>();
        if (dirs.size()>0) {
            for (int i = 0; i < dirs.size(); i++) { // TODO - how to replace for foreach if inside lambda function it's hard to refer outside
                List<String> listedValidFiles = JamlinFiles.listValidFiles(new File(workingDirectory+File.separator+dirs.get(i)), extensions);
                sourceExtractFiles.addAll(listedValidFiles);
            }
        } else {
            List<String> listedValidFiles = JamlinFiles.listValidFiles(new File(workingDirectory), extensions);
            sourceExtractFiles.addAll(listedValidFiles);
        }

        // if REPLACE action without target
        if ( action!=null && action.equals(actions.REPLACE.toString().toLowerCase()) && target!=null && !target.isEmpty() ) {
            // use exact result in replace - replace with target = lang specific and semiautomatic modes
            resultFiles.add(target);
            expectedFilesCount = 1;
        // if EXTRACT action
        } else if ( action!=null && action.equals(actions.EXTRACT.toString().toLowerCase()) && source!=null && !source.isEmpty() && (target==null || !target.isEmpty()) ) {
            // extract should not have target
            if (dictionary) {
                // set only one expected file for dictionary
                expectedFilesCount = 1;
            } else {
                expectedFilesCount = sourceExtractFiles.size();//TODO - fix to work with - JamlinFiles.getExpectedFilesCount(action, mode, resultFiles);
            }
        } else {
            // all other EXTRACT & REPLACE actions
            resultFiles = JamlinFiles.listValidFiles(new File(workingDirectory), extensions);
            if (dictionary && action!=null && action.equals(actions.EXTRACT.toString().toLowerCase())) {
                // set only one expected file for dictionary
                expectedFilesCount = 1;
            } else {
                expectedFilesCount = resultFiles.size();//TODO - fix to work with - sk.cw.jamlin.JamlinFiles.getExpectedFilesCount(action, resultFiles);
            }
        }

        startupTimestamp = new Date();

        System.out.println("Started JaMLin: "+ startupTimestamp.toString() );
        System.out.println("ACTION: "+ (action==null ? "null(extract)" : action) );
        System.out.println("MODE: "+mode);
        System.out.println("SOURCE: "+source);
        System.out.println("TARGET: "+target);
        System.out.println("LANGUAGE: "+language);
        System.out.println("WORKING DIR: "+workingDirectory);

        System.out.println("resultFiles.size(): "+resultFiles.size());

        // if extracting whole project translations
        if ( action!=null && action.equals(actions.EXTRACT.toString().toLowerCase()) && dictionary ) {
            extractDictionary = new TranslationExtractDictionary(new ArrayList<>());
        }

        // main event - finally running getFileTranslation()
        if (action!=null && action.equals(actions.REPLACE.toString().toLowerCase())) { // REPLACE action
            if (resultFiles.size()>0) {
                for (String resultFile : resultFiles) {
                    try {
                        File parentDirectory = new File(resultFile);
                        String fileNameOrig = parentDirectory.getName();
                        String fileName = "";
                        String langCode = Language.getLangCodeFromFilePath(parentDirectory.getPath());
                        String jsonFilePath = "";
                        if (langCode != null && Language.checkLangCodeValid(langCode)) {
                            // if valid, remove lang from filename
                            fileName = fileNameOrig.replace("-" + langCode, "-extract");
                            String[] extension = fileName.split("\\.");
                            if (extension.length > 0) {
                                fileName = fileName.replace("." + extension[extension.length - 1], ".json");
                            } else {
                                fileName = null;
                            }
                            parentDirectory = parentDirectory.getParentFile();
                            jsonFilePath = parentDirectory.getPath() + File.separator + fileName;
                        } else { // not valid language code
                            if (source != null && !source.isEmpty()) {
                                jsonFilePath = source;
                            } else {
                                String[] extension = fileNameOrig.split("\\.");
                                if (extension.length > 0) {
                                    fileName = fileNameOrig.replace("." + extension[extension.length - 1], "");
                                } else {
                                    fileName = null;
                                }
                                if (fileName.contains("-")) {
                                    String[] fileNameBlocks = fileName.split("\\-");
                                    fileNameBlocks = Arrays.copyOf(fileNameBlocks, fileNameBlocks.length - 1);
                                    fileName = String.join("-", fileNameBlocks);
                                }
                                fileName = fileName + "-extract.json";
                                parentDirectory = parentDirectory.getParentFile();
                                jsonFilePath = parentDirectory.getPath() + File.separator + fileName;
                            }

                        }
                        // check if json exists
                        if ((new File(jsonFilePath)).exists()) {
                            if (Language.checkLangCodeValid(langCode)) {
                                config.setLanguage(new Language(langCode));
                            }
                            System.out.println("Processing file: " + resultFile);
                            getFileTranslation(config, action, jsonFilePath, resultFile);
                        } else {
                            expectedFilesCount--;
                            System.out.println("Not exists: " + jsonFilePath);
                        }

                    } catch (Exception e) {
                        System.out.println("Replace action file error: " + e.getMessage());
//                        System.out.print(e.getCause());
                    }
                }
            } else {
                System.out.println("No result files for replace");
            }
        } else {
            // action EXTRACT
            // if any source extract files defined from directories
            if (sourceExtractFiles.size()>0) {
                for (String sourceExtractFile: sourceExtractFiles) {
                    System.out.println(" -------------------------------------------- ");
                    System.out.println(sourceExtractFile);
                    System.out.println(" -------------------------------------------- ");
                    // using sourceExtractFiles as source
                    getFileTranslation(config, action, sourceExtractFile, null);
                }
            } else {
                // extract specific
                getFileTranslation(config, action, source, null);
            }
        }

        // if extracting whole project translations
        if (action.equals(actions.EXTRACT.toString().toLowerCase()) && dictionary) {
            JamlinFiles.writeExtractDictionary(extractDictionary);
        }
    }


    /**
     *
     * @param config Config
     * @param action String
     * @param source String
     * @param target String
     */
    private static void getFileTranslation(Config config, String action, String source, String target) {
        boolean variablesPassed = true;

        // get source - use relative path if no separator
        if ( source!=null && !source.trim().isEmpty() ) { // no source
            source = source.trim();
            if (!source.contains(File.separator)) {
                source = workingDirectory + File.separator + source;
            }
        } else {
            variablesPassed = false;
            System.out.println("ERROR: No 'source' set.");
        }

        // get target - REPLACE only
        if ( action.equals(actions.REPLACE.toString().toLowerCase()) ) {
            if (target != null && !target.trim().isEmpty()) {
                target = target.trim();
                if (!target.contains(File.separator)) {
                    target = workingDirectory + File.separator + target;
                }
            } else {
                variablesPassed = false;
                System.out.println("ERROR: No 'target' set.");
            }
        }

        if ( variablesPassed ) {
            if (action.equals(actions.EXTRACT.toString().toLowerCase())) {
                String fileLangCode = Language.getLangCodeFromFilePath(source);
                if (!fileLangCode.isEmpty() && Language.checkLangCodeValid(fileLangCode)) {
                    config.setLanguage(new Language(fileLangCode));
                }
            } else if (action.equals(actions.REPLACE.toString().toLowerCase())) {
                if ( source!=null && !source.trim().isEmpty() && target!=null && !target.trim().isEmpty() ) {
                    System.out.println("REPLACE - Have source string, have target string - continue");
                } else if (target!=null) {
                    String fileLangCode = Language.getLangCodeFromFilePath(target);
                    if (!fileLangCode.isEmpty() && Language.checkLangCodeValid(fileLangCode)) {
                        config.setLanguage(new Language(fileLangCode));
                    }
                }
            }

            String input = "";
            try {
                input = new String(Files.readAllBytes(Paths.get(source)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println("Main.getFileTranslation() - getting input from source:");
                System.out.println(source);
                e.printStackTrace();
            }

            TranslationConfig translationConfig = config.makeTranslationConfig(source, target);

            Translation translation = new Translation(translationConfig);
            String result = "";

            if (translation.validAction(action)) {
                if (action.equals(actions.REPLACE.toString().toLowerCase())) {
                    String targetString = "";
                    try {
                        if (target!=null) {
                            targetString = new String(Files.readAllBytes(Paths.get(target)), StandardCharsets.UTF_8);
                        } else {
                            System.out.println("Target is NULL");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    TranslationReplaceResult replaceResults = translation.replaceStrings(input, targetString);
                    result = "Please, set output language for result translation";
                    if (language!=null && !language.isEmpty() && replaceResults.getLangCodes().size()>0) {
                        // if language is set, return result of that language
                        result = replaceResults.get(language);
                    } else {
                        // else if any results, output files and return first
                        result = replaceResults.get(replaceResults.getLangCodes().get(0));
                    }
                    JamlinFiles.outputReplaceResultFiles(replaceResults, target);

                } else { // extract
                    // MOST IMPORTANT part for EXTRACT action - get translation strings from html and create object from it
                    TranslationExtractResult resultObject = translation.extractStrings(input);
                    File sourceFile = new File(source);
                    // write result - merge with existing if possible
                    JamlinFiles.outputExtractResultFile(resultObject, sourceFile, translation);
                }
            }
        } else {
            System.out.println("No output - please check your inputs:");
            System.out.println("action: "+action);
            System.out.println("source: "+source);
            System.out.println("target: "+target);
        }
    }


    /**
     *
     * @param type String
     * @return boolean
     */
    private static boolean validAction(String type) {
        for (actions c : actions.values()) {
            if (c.name().toLowerCase().equals(type)) {
                return true;
            }
        }
        return false;
    }


    /**
     *
     * @return String
     */
    private static String getMode() {
        if ( action!=null && action.equals(actions.REPLACE.toString().toLowerCase()) ) {
            if ( source!=null && !source.isEmpty() && target!=null && !target.isEmpty() && language!=null && !language.isEmpty() ) {
                return "specific";
            } else if ( source!=null && !source.isEmpty() && target!=null && !target.isEmpty() ) {
                return "semiautomatic";
            }
        } else { // extract
            if ( source!=null && !source.isEmpty() && language!=null && !language.isEmpty() ) {
                return "specific";
            } else if ( source!=null && !source.isEmpty() ) {
                return "semiautomatic";
            }
        }
        return "automatic";
    }

}
