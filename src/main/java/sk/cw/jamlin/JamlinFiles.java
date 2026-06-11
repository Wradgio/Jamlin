package sk.cw.jamlin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by Marcel Zúbrik on 29.10.2017.
 */
public class JamlinFiles {

    private static final Logger log = LoggerFactory.getLogger(JamlinFiles.class);

    static List<String> collectConfiguredSourceFiles(String workingDirectory, Config config) {
        LinkedHashSet<String> files = new LinkedHashSet<>();
        List<String> extensions = collectExtensions(config);
        boolean listedFromDirectories = false;

        for (IConfigSourceFilter source : config.getSources().getDirectories()) {
            ConfigSourceFilterDirectory dirConfig = (ConfigSourceFilterDirectory) source;
            String path = dirConfig.getPath();
            if (path != null && !path.trim().isEmpty()) {
                listedFromDirectories = true;
                List<String> dirExtensions = dirConfig.getExtensions().isEmpty() ? extensions : dirConfig.getExtensions();
                boolean traverse = Boolean.TRUE.equals(dirConfig.getTraverse());
                files.addAll(listValidFiles(new File(workingDirectory + File.separator + path), dirExtensions, traverse));
            }
        }

        for (IConfigSourceFilter source : config.getSources().getFiles()) {
            files.addAll(listFilesMatchingGlob(workingDirectory, source.getPath()));
        }

        if (!listedFromDirectories && files.isEmpty()) {
            files.addAll(listValidFiles(new File(workingDirectory), extensions, getDirectoryTraverse(config)));
        }

        return new ArrayList<>(files);
    }

    static List<String> collectExtensions(Config config) {
        LinkedHashSet<String> extensions = new LinkedHashSet<>();
        for (IConfigSourceFilter source : config.getSources().getDirectories()) {
            ConfigSourceFilterDirectory dir = (ConfigSourceFilterDirectory) source;
            extensions.addAll(dir.getExtensions());
        }
        if (extensions.isEmpty()) {
            extensions.add("html");
        }
        return new ArrayList<>(extensions);
    }

    static boolean getDirectoryTraverse(Config config) {
        if (config.getSources().getDirectories().size() > 0) {
            ConfigSourceFilterDirectory dir = (ConfigSourceFilterDirectory) config.getSources().getDirectories().get(0);
            return Boolean.TRUE.equals(dir.getTraverse());
        }
        return true;
    }

    static List<String> listFilesMatchingGlob(String workingDirectory, String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedPattern = pattern.replace('\\', '/');
        int slashIndex = normalizedPattern.lastIndexOf('/');
        File searchRoot;
        String fileGlob;
        if (slashIndex >= 0) {
            searchRoot = new File(workingDirectory, normalizedPattern.substring(0, slashIndex));
            fileGlob = normalizedPattern.substring(slashIndex + 1);
        } else {
            searchRoot = new File(workingDirectory);
            fileGlob = normalizedPattern;
        }

        if (!searchRoot.isDirectory()) {
            return Collections.emptyList();
        }

        String fileRegex = globToRegex(fileGlob);
        List<String> matches = new ArrayList<>();
        File[] children = searchRoot.listFiles();
        if (children == null) {
            return matches;
        }

        for (File child : children) {
            if (child.isFile() && !child.getName().contains(".jamlin_history") && child.getName().matches(fileRegex)) {
                matches.add(child.getAbsolutePath());
            }
        }
        return matches;
    }

    static boolean matchesGlobPath(String pattern, String relativePath) {
        if (pattern == null || relativePath == null) {
            return false;
        }
        return relativePath.replace('\\', '/').matches(globToRegex(pattern.replace('\\', '/')));
    }

    static String toRelativePath(String absolutePath, String workingDirectory) {
        String sourcePath = new File(absolutePath).getAbsolutePath();
        String workPath = new File(workingDirectory).getAbsolutePath();
        if (!workPath.endsWith(File.separator)) {
            workPath = workPath + File.separator;
        }
        if (sourcePath.startsWith(workPath)) {
            return sourcePath.substring(workPath.length()).replace(File.separatorChar, '/');
        }
        return new File(absolutePath).getName();
    }

    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append('.');
                    break;
                default:
                    if (".\\[]{}()+-^$|".indexOf(c) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(c);
                    break;
            }
        }
        regex.append('$');
        return regex.toString();
    }

    // browse files
    static List<String> listValidFiles(File dir, List<String> extensions) {
        return listValidFiles(dir, extensions, true);
    }

    static List<String> listValidFiles(File dir, List<String> extensions, boolean traverse) {
        List<String> resultFiles = new ArrayList<>();
        return listValidFiles(dir, extensions, traverse, resultFiles);
    }

    private static List<String> listValidFiles(File dir, List<String> extensions, boolean traverse, List<String> resultFiles) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children!=null) {
                for (int i = 0; i < children.length; i++) {
                    if (!children[i].contains(".jamlin_history")) { // don't list history
                        String fileExtension = getFileExtension(children[i]);
                        File item = new File(dir, children[i]);
                        if (item.isDirectory()) {
                            if (traverse) {
                                listValidFiles(item, extensions, traverse, resultFiles);
                            }
                        } else if (extensions.contains(fileExtension)) {
                            resultFiles.add(item.toString());
                        }
                    }
                }
            }
            return resultFiles;
        } else {
            resultFiles.add(dir.toString());
            return resultFiles;
        }
    }


    /**
     *
     * @param input TranslationExtractResult
     * @param source File
     * @param translation Translation
     */
    static void outputExtractResultFile(JamlinRunContext context, TranslationExtractResult input, File source, Translation translation) {
        String fileName = "";
        String fileExtension = "";

        // get filename from source file name
        try {
            fileName = source.getName();
            // get name before extension
            if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
        } catch (Exception e) {
            log.error("Failed to resolve extract output file name", e);
            context.markFailed();
        }

        // set language code from translation
        String langCode = null;
        if ( translation.getLanguage()!=null && !translation.getLanguage().toString().isEmpty() ) {
            langCode = translation.getLanguage().getCode();
        } else {
            // get lang code from filename
            langCode = Language.getLangCodeFromFilePath(fileName);
        }
        if (langCode!=null && Language.checkLangCodeValid(langCode) ) {
            // if valid, remove lang from filename
            fileName = fileName.replace("-"+langCode, "");
        }

        // create filename for extract file
        fileName = fileName+"-extract";
        if ( !fileName.contains(".json") ) {
            fileName = fileName+".json";
        }

        TranslationExtractResult extractResult = input;

        // merge two results and create new result
        String oldResultFilePath = source.getParentFile().toString()+ File.separator +fileName;
        if ( (new File(oldResultFilePath)).exists() ) {

            // get old result from file
            String oldResultInput = "";
            try {
                oldResultInput = new String( java.nio.file.Files.readAllBytes(Paths.get(oldResultFilePath)), StandardCharsets.UTF_8 );
            } catch (IOException e) {
                log.error("Failed to read existing extract file: {}", oldResultFilePath, e);
                context.markFailed();
            }
            TranslationExtractResult oldResult = null;
            if (!oldResultInput.isEmpty()) {
                Gson gsonOld = new Gson();
                try {
                    oldResult = gsonOld.fromJson(oldResultInput, TranslationExtractResult.class);
                } catch (Exception e) {
                    log.error("Failed to parse existing extract file: {}", oldResultFilePath, e);
                    context.markFailed();
                }
            }

            // get new result from input
            if (oldResult!=null && extractResult!=null) {
                extractResult = TranslationExtractResult.mergeTwoResults(oldResult, extractResult);
            }
        }

        // save history
        if (context.getConfig().getTarget().getSaveHistory()) {
            makeHistory(context.getStartupTimestamp(), source);
        }

        // if Dictionary, add record - extractDictionary is cleared later in writeExtractDictionary()
        if (context.isExtractAction() && context.isDictionary() && context.getExtractDictionary() != null) {
            context.getExtractDictionary().addRecords(translation.getLanguage(), source.getPath(), extractResult);
        } else {
            writeResultFile(context, source.getParentFile(), fileName, extractResult.resultToJson());
        }
    }


    /**
     *
     * @param results TranslationReplaceResult
     * @param destination String
     */
    static void outputReplaceResultFiles(JamlinRunContext context, TranslationReplaceResult results, String destination) {
        Map<String, String> resultFileNames = new HashMap<>();

        resultFileNames = getReplaceOutputFileName(results, destination);

        File destinationDirectory = new File(destination);
        if (destinationDirectory!=null && !destinationDirectory.isDirectory()) {
            destinationDirectory = destinationDirectory.getParentFile();
        }

        if (results.getLangCodes().size()>0) {
            for (int j=0; j<results.getLangCodes().size(); j++) {
                String langCode = results.getLangCodes().get(j);
                // save history
                if (context.getConfig().getTarget().getSaveHistory()) {
                    makeHistory(context.getStartupTimestamp(),
                            new File(destinationDirectory + File.separator + resultFileNames.get(langCode)));
                }
                writeResultFile(context, destinationDirectory, resultFileNames.get(langCode), results.get(langCode));
            }
        }
    }


    /**
     *
     * @param results TranslationReplaceResult
     * @param destination String
     * @return Map
     */
    private static Map<String, String> getReplaceOutputFileName(TranslationReplaceResult results, String destination) {
        String fileName = "";
        String fileExtension = "";
        Map<String, String> resultFileNames = new HashMap<>();

        // get file name
        if ( !results.getTargetPattern().isEmpty() && !destination.isEmpty() ) {
            try {
                File f = new File(destination);
                fileName = f.getName();
                // get file extension
                fileExtension = getFileExtension(fileName);
                // get name before extension
                if (fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                }
            } catch (Exception e) {
                log.error("Failed to resolve replace output file name", e);
            }
        }

        if (results.getLangCodes().size()>0) {
            for (int j=0; j<results.getLangCodes().size(); j++) {
                // remove language from fileName it is already included
                String foundLangExt = fileName.substring(fileName.length() -1 -results.getLangCodes().get(j).length(), fileName.length());
                if ( foundLangExt.equals("-"+results.getLangCodes().get(j)) ) {
                    fileName = fileName.substring(0, fileName.length() -1 -results.getLangCodes().get(j).length());
                }
                // set filename from config pattern
                String newName = fileNameFromPattern(results.getTargetPattern(), fileName, fileExtension, results.getLangCodes().get(j));
                resultFileNames.put(results.getLangCodes().get(j), newName);
            }
        }

        return resultFileNames;
    }


    /**
     *
     * @param pattern String
     * @param fileName String
     * @param fileExtension String
     * @param langCode String
     * @return String
     */
    private static String fileNameFromPattern(String pattern, String fileName, String fileExtension, String langCode) {
        String resultName = "";

        if ( !pattern.isEmpty() && pattern.contains("*") ) {
            resultName = pattern;
            int starIndex = resultName.indexOf("*");
            if (starIndex>-1) {
                resultName = resultName.substring(0, starIndex) + fileName + resultName.substring(starIndex + 1);
            }
            starIndex = resultName.lastIndexOf("*");
            if (starIndex>-1) {
                resultName = resultName.substring(0, starIndex) + fileExtension + resultName.substring(starIndex + 1);
            }
            resultName = resultName.replaceAll("\\*$",fileExtension);
            resultName = resultName.replace("$lang", langCode);
            if (resultName.contains("$datetime")) {
                Date now = new Date();
                LocalDateTime current = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                String datetime = current.format(DateTimeFormatter.ofPattern("u-mm-dd-HH-mm-ss"));
                resultName = resultName.replace("$datetime", datetime);
            }
        } else {
            resultName = fileName +"-"+ langCode +"."+ fileExtension;
        }

        return resultName;
    }


    /**
     *
     * @param destination File
     * @param fileName String
     * @param resultContent String
     */
    private static void writeResultFile(JamlinRunContext context, File destination, String fileName, String resultContent) {
        FileWriter locFile = null;
        try {
            locFile = new FileWriter(destination.toString() + File.separator + fileName);
            locFile.write(resultContent);
        } catch (IOException e) {
            log.error("Write error for: {}", fileName, e);
            context.markFailed();
        } finally {
            try {
                if (locFile != null) {
                    locFile.close();
                }
                context.incrementExportedFilesCount();
                log.info("Exported #{}: {}{}{}", context.getExportedFilesCount(),
                        destination, File.separator, fileName);
            } catch (IOException e) {
                log.error("Close error for: {}", fileName, e);
                context.markFailed();
            }
        }
    }


    /**
     *
     * @param fileName String
     * @return String
     */
    private static String getFileExtension(String fileName) {
        String fileExtension = "";
        // get file extension
        int i = fileName.lastIndexOf(".");
        if (i>0) {
            fileExtension = fileName.substring(i+1);
            String[] name = fileName.split("\\.", i+1);
            String[] pureName = Arrays.copyOf(name, name.length-1);
            fileName = String.join(".", pureName);
        }

        return fileExtension;
    }


    /**
     *
     * @param action String
     * @param mode String
     * @param resultFiles List<String>
     * @return int
     */
    static int getExpectedFilesCount(JamlinAction action, String mode, List<String> resultFiles, boolean dictionary) {
        if (action == null) {
            action = JamlinAction.EXTRACT;
        }

        if (action == JamlinAction.EXTRACT) {
            if (dictionary) {
                return 1;
            }
            if ("specific".equals(mode)) {
                return 1;
            }
            return resultFiles == null ? 0 : resultFiles.size();
        }

        if (action == JamlinAction.REPLACE) {
            if ("specific".equals(mode)) {
                return 1;
            }
            return resultFiles == null ? 0 : Math.max(resultFiles.size(), 1);
        }

        return 1;
    }


    /**
     *
     * @param startTimestamp Date
     * @param origFile File
     * @return File
     */
    static File makeHistory(Date startTimestamp, File origFile) {
        if ( startTimestamp!=null && origFile!=null && !origFile.getName().equals(".jamlin_history") && !origFile.toPath().toString().contains(".jamlin_history") ) {
            File baseDirectory = origFile.getParentFile();
            SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String formatedDateString = dateFormater.format(startTimestamp);
            String[] dateStrings = null;

            if ( formatedDateString.contains(" ") ) {
                dateStrings = formatedDateString.split("\\s+");
            }

            if (dateStrings!=null && dateStrings.length>=2) {
                // parent location + / + .jamlin_history + / + yyyy-MM-dd + / + HH-mm-ss
                File destinationHistoryFolder = new File(origFile.getParent()+ File.separator+ ".jamlin_history" +File.separator+ dateStrings[0]
                        +File.separator+ dateStrings[1]);
                if ( !destinationHistoryFolder.exists() ) {
                    try {
                        java.nio.file.Files.createDirectories(destinationHistoryFolder.toPath());
                    } catch (IOException exception) {
                        log.error("Cannot create history directories", exception);
                    }
                }

                if ( destinationHistoryFolder.exists() ) {
                    File destinationFile = new File(destinationHistoryFolder.toString() +File.separator+ origFile.getName());
                    try {
                        log.debug("Copy of {} into {}", origFile.toPath(), destinationFile.toPath());
                        java.nio.file.Files.copy(origFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException exception) {
                        log.error("Cannot copy file to history", exception);
                    }
                }
            } else {
                log.warn("Could not parse history timestamp");
            }

        } else {
            log.warn("Missing parameters for history backup");
        }
        return null;
    }


    /**
     *
     * @param extractDictionary TranslationExtractDictionary
     */
    static void writeExtractDictionary(JamlinRunContext context) {
        TranslationExtractDictionary extractDictionary = context.getExtractDictionary();
        File target = new File(context.getWorkingDirectory() + File.separator + "project_dictionary.json");
        if (target.exists()) {
            extractDictionary = mergeDictionaryVersions(extractDictionary, target);
            target.delete();
        }
        if (!target.exists()) {
            FileWriter locFile = null;
            try {
                locFile = new FileWriter(target.getPath());
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                TranslationExtractDictionaryFileWrap fileWrap = new TranslationExtractDictionaryFileWrap(
                        context.getWorkingDirectory(), extractDictionary);
                locFile.write(gson.toJson(fileWrap));
            } catch (IOException e) {
                log.error("Write error for project_dictionary.json", e);
                context.markFailed();
            } finally {
                try {
                    if (locFile != null) {
                        locFile.close();
                    }
                    context.incrementExportedFilesCount();
                    log.info("Extracted #{}: {}", context.getExportedFilesCount(), target);
                } catch (IOException e) {
                    log.error("Close error for project_dictionary.json", e);
                    context.markFailed();
                }
            }
        }

        // append records
        //appendToFile(target, concatExtractRecord);

        // if last, write end
    }


    private static TranslationExtractDictionary mergeDictionaryVersions( TranslationExtractDictionary extractDictionary, File target ) {
        // try to extract old Dictionary
        String oldResultInput = "";
        TranslationExtractDictionaryFileWrap oldDictionary = null;
        // first read JSON file into string
        try {
            oldResultInput = new String( java.nio.file.Files.readAllBytes(Paths.get(target.getPath())), StandardCharsets.UTF_8 );
        } catch (IOException e) {
            log.error("Failed to read existing project dictionary", e);
        }
        // then try to convert JSON into dictionary wrap object
        try {
            Gson gsonOld = new Gson();
            oldDictionary = gsonOld.fromJson(oldResultInput, TranslationExtractDictionaryFileWrap.class);
        } catch (Exception e) {
            log.error("Failed to parse existing project dictionary", e);
        }

        // if old dictionary exists, merge it with new one
        if ( oldDictionary!=null ) {
            extractDictionary = extractDictionary.mergeOldDictionary(oldDictionary.getDictionary());
        }

        return extractDictionary;
    }

}
