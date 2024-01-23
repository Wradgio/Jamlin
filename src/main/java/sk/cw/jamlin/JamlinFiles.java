package sk.cw.jamlin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

    protected static int extractedFilesCount = 0;

    // browse files
    static List<String> listValidFiles(File dir, List<String> extensions) {
        List<String> resultFiles = new ArrayList<>();
        return listValidFiles(dir, 0, extensions, resultFiles);
    }

    private static List<String> listValidFiles(File dir, int level, List<String> extensions, List<String> resultFiles) { //
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children!=null) {
                for (int i = 0; i < children.length; i++) {
                    if (!children[i].contains(".jamlin_history")) { // don't list history
                        String fileExtension = getFileExtension(children[i]);
                        File item = new File(dir, children[i]);
                        if (item.isDirectory()) {
                            listValidFiles(item, level, extensions, resultFiles);
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
    static void outputExtractResultFile(TranslationExtractResult input, File source, Translation translation) {
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
            System.out.println("outputExtractResultFile fileName: ");
            System.out.println(e.getMessage());
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
                e.printStackTrace();
            }
            TranslationExtractResult oldResult = null;
            if (!oldResultInput.isEmpty()) {
                Gson gsonOld = new Gson();
                try {
                    oldResult = gsonOld.fromJson(oldResultInput, TranslationExtractResult.class);
                } catch (Exception e) {
                    System.out.println("outputExtractResultFile oldResult: ");
                    System.out.println(e.getMessage());
                }
            }

            // get new result from input
            if (oldResult!=null && extractResult!=null) {
                extractResult = TranslationExtractResult.mergeTwoResults(oldResult, extractResult);
            }
        }

        // save history
        if ( Main.config.getTarget().getSaveHistory() ) {
            JamlinFiles.makeHistory(Main.startupTimestamp, source);
        }

        // if Dictionary, add record - extractDictionary is cleared later in JamlinFiles.writeExtractDictionary()
        if (Main.action.equals(Main.actions.EXTRACT.toString().toLowerCase()) && Main.dictionary && Main.extractDictionary!=null) {
            Main.extractDictionary.addRecords(translation.getLanguage(), source.getPath(), extractResult);
        } else {
            // if -extract.json file, write it
            writeResultFile(source.getParentFile(), fileName, extractResult.resultToJson());
        }
    }


    /**
     *
     * @param results TranslationReplaceResult
     * @param destination String
     */
    static void outputReplaceResultFiles(TranslationReplaceResult results, String destination) {
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
                if ( Main.config.getTarget().getSaveHistory() ) {
                    JamlinFiles.makeHistory(Main.startupTimestamp, new File(destinationDirectory +File.separator+ resultFileNames.get(langCode)) );
                }
                writeResultFile(destinationDirectory, resultFileNames.get(langCode), results.get(langCode));
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
                System.out.println("getReplaceOutputFileName: ");
                System.out.println(e.getMessage());
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
    private static void writeResultFile(File destination, String fileName, String resultContent) {
        FileWriter locFile = null;
        try {
            locFile = new FileWriter(destination.toString()+ File.separator+ fileName);
            locFile.write(resultContent);
        } catch(IOException e) {
            System.out.println("Write error for: "+fileName);
            e.printStackTrace();
        } finally {
            try {
                if(locFile != null) {
                    locFile.close();
                }
                Main.exportedFilesCount ++;
                System.out.println("Exported #"+ Main.exportedFilesCount+ ": "+ destination.toString()+ File.separator+ fileName);
            } catch(IOException e) {
                System.out.println("Close error for: "+fileName);
                e.printStackTrace();
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
    static int getExpectedFilesCount(String action, String mode, List<String> resultFiles) {
        List<String> usedNames = new ArrayList<String>();

        if (resultFiles!=null && resultFiles.size()>0) {
            for (int i = 0; i < resultFiles.size(); i++) {
                //mode replace automatic
                if ( action!=null && action.equals(Main.actions.REPLACE.toString().toLowerCase()) ) {
                    if ( mode!=null && mode.equals("automatic") ) {
                        /*if ( resultFiles.get(i).indexOf('-extract.json') ) {

                        }*/
                    }
                }
            }
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
                        System.out.println("Cannot create directories: "+exception.getMessage());
                    }
                }

                if ( destinationHistoryFolder.exists() ) {
                    File destinationFile = new File(destinationHistoryFolder.toString() +File.separator+ origFile.getName());
                    System.out.println(destinationFile.toString());
                    try {
                        System.out.println("Copy of: " +origFile.toPath()+ " into: " +destinationFile.toPath());
                        java.nio.file.Files.copy(origFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException exception) {
                        System.out.println("Cannot copy file: "+exception.getMessage());
                    }
                }
            } else {
                System.out.println("No datestrings");
            }

        } else {
            System.out.println("Missing parameters.");
        }
        return null;
    }


    /**
     *
     * @param extractDictionary TranslationExtractDictionary
     */
    static void writeExtractDictionary( TranslationExtractDictionary extractDictionary ) {
        // get path of extractDictionary file
        File target = new File(Main.workingDirectory + File.separator + "project_dictionary.json");
        if ( target.exists() ) {
            // before delete, merge translations from old dictionary with latest
            extractDictionary = mergeDictionaryVersions(extractDictionary, target);
            target.delete();
        }
        // if not created, create it and write start
        if ( !target.exists() ) {
            FileWriter locFile = null;
            try {
                locFile = new FileWriter(target.getPath());
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                TranslationExtractDictionaryFileWrap fileWrap = new TranslationExtractDictionaryFileWrap(Main.workingDirectory, extractDictionary);
//                locFile.write("{\"path\":\"" +Main.workingDirectory+ "\",\"dictionary\":" +gson.toJson(Main.extractDictionary)+ "}");
                locFile.write( gson.toJson(fileWrap) );
            } catch(IOException e) {
                System.out.println("Write error for project_dictionary.json intro");
                e.printStackTrace();
            } finally {
                try {
                    if(locFile != null) {
                        locFile.close();
                    }
                    extractedFilesCount++;
                    System.out.println("Extracted #"+ extractedFilesCount+ ": "+ target.toString());
                } catch(IOException e) {
                    System.out.println("Close error for project_dictionary.json");
                    e.printStackTrace();
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
            e.printStackTrace();
        }
        // then try to convert JSON into dictionary wrap object
        try {
            Gson gsonOld = new Gson();
            oldDictionary = gsonOld.fromJson(oldResultInput, TranslationExtractDictionaryFileWrap.class);
        } catch (Exception e) {
            System.out.println("replaceStrings: ");
            System.out.println(e.getMessage());
        }

        // if old dictionary exists, merge it with new one
        if ( oldDictionary!=null ) {
            extractDictionary = extractDictionary.mergeOldDictionary(oldDictionary.getDictionary());
        }

        return extractDictionary;
    }

}
