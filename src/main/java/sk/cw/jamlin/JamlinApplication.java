package sk.cw.jamlin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JamlinApplication {

    private static final Logger log = LoggerFactory.getLogger(JamlinApplication.class);

    private final JamlinRunContext context = new JamlinRunContext();

    public JamlinRunContext getContext() {
        return context;
    }

    public int run(CliOptions options) {
        applyOptions(options);

        context.setStartupTimestamp(new Date());
        context.setConfig(loadConfig(options));

        if (context.getConfig() == null) {
            log.error("No config found");
            context.markFailed();
            return 1;
        }

        handleFileTranslations();
        return resolveExitCode();
    }

    private void applyOptions(CliOptions options) {
        if (options.workingDirectory != null && !options.workingDirectory.isEmpty()) {
            context.setWorkingDirectory(options.workingDirectory);
        } else {
            context.setWorkingDirectory(System.getProperty("user.dir"));
        }

        context.setSource(options.source);
        context.setTarget(options.target);
        context.setLanguage(options.language == null ? "" : options.language);
        context.setLanguageName(options.languageName == null ? "" : options.languageName);
        context.setDictionary(options.dictionary);

        JamlinAction action = JamlinAction.fromString(options.action);
        if (options.action != null && !options.action.isEmpty() && action == null) {
            log.warn("Unknown action '{}', defaulting to extract", options.action);
            action = JamlinAction.EXTRACT;
        }
        context.setAction(action == null ? JamlinAction.EXTRACT : action);
    }

    private Config loadConfig(CliOptions options) {
        String configPath;
        if (options.configPath != null && !options.configPath.isEmpty()) {
            configPath = options.configPath;
        } else {
            configPath = context.getWorkingDirectory() + File.separator + "jamlin_config.json";
            if (!new File(configPath).exists()) {
                return null;
            }
        }
        return readConfig(configPath);
    }

    Config readConfig(String configFilePath) {
        try {
            String jsonConfig = new String(
                    Files.readAllBytes(Paths.get(configFilePath)),
                    StandardCharsets.UTF_8);
            if (context.getLanguage().trim().isEmpty()) {
                return new Config("file", jsonConfig);
            }
            return new Config("file", jsonConfig, context.getLanguage(), context.getLanguageName());
        } catch (IOException e) {
            log.error("Failed to read config from {}", configFilePath, e);
            context.markFailed();
        } catch (Exception e) {
            log.error("Failed to parse config from {}", configFilePath, e);
            context.markFailed();
        }
        return null;
    }

    private void handleFileTranslations() {
        context.setMode(resolveMode());

        List<String> configuredSourceFiles = JamlinFiles.collectConfiguredSourceFiles(
                context.getWorkingDirectory(), context.getConfig());
        List<String> resultFiles = new ArrayList<>();
        List<String> sourceExtractFiles = new ArrayList<>(configuredSourceFiles);

        if (context.isReplaceAction() && context.getTarget() != null && !context.getTarget().isEmpty()) {
            resultFiles.add(context.getTarget());
            context.setExpectedFilesCount(JamlinFiles.getExpectedFilesCount(
                    context.getAction(), context.getMode(), resultFiles, context.isDictionary()));
        } else if (context.isExtractAction()
                && context.getSource() != null && !context.getSource().isEmpty()) {
            context.setExpectedFilesCount(JamlinFiles.getExpectedFilesCount(
                    context.getAction(), context.getMode(), sourceExtractFiles, context.isDictionary()));
        } else {
            resultFiles = new ArrayList<>(configuredSourceFiles);
            context.setExpectedFilesCount(JamlinFiles.getExpectedFilesCount(
                    context.getAction(), context.getMode(), resultFiles, context.isDictionary()));
        }

        context.setStartupTimestamp(new Date());

        log.info("Started JaMLin: {}", context.getStartupTimestamp());
        log.info("ACTION: {}", context.getAction().getCliName());
        log.info("MODE: {}", context.getMode());
        log.info("SOURCE: {}", context.getSource());
        log.info("TARGET: {}", context.getTarget());
        log.info("LANGUAGE: {}", context.getLanguage());
        log.info("WORKING DIR: {}", context.getWorkingDirectory());
        log.info("resultFiles.size(): {}", resultFiles.size());

        if (context.isExtractAction() && context.isDictionary()) {
            context.setExtractDictionary(new TranslationExtractDictionary(new ArrayList<>()));
        }

        if (context.isReplaceAction()) {
            processReplaceAction(resultFiles);
        } else {
            processExtractAction(sourceExtractFiles);
        }

        if (context.isExtractAction() && context.isDictionary()) {
            JamlinFiles.writeExtractDictionary(context);
        }
    }

    private void processReplaceAction(List<String> resultFiles) {
        if (resultFiles.isEmpty()) {
            log.warn("No result files for replace");
            context.markFailed();
            return;
        }

        for (String resultFile : resultFiles) {
            try {
                resultFile = resolvePath(resultFile);
                File parentDirectory = new File(resultFile);
                String fileNameOrig = parentDirectory.getName();
                String fileName = "";
                String langCode = Language.getLangCodeFromFilePath(parentDirectory.getPath());
                String jsonFilePath = "";
                if (langCode != null && Language.checkLangCodeValid(langCode)) {
                    fileName = fileNameOrig.replace("-" + langCode, "-extract");
                    String[] extension = fileName.split("\\.");
                    if (extension.length > 0) {
                        fileName = fileName.replace("." + extension[extension.length - 1], ".json");
                    } else {
                        fileName = null;
                    }
                    parentDirectory = parentDirectory.getParentFile();
                    jsonFilePath = parentDirectory.getPath() + File.separator + fileName;
                } else {
                    if (context.getSource() != null && !context.getSource().isEmpty()) {
                        jsonFilePath = resolvePath(context.getSource());
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

                if (new File(jsonFilePath).exists()) {
                    if (Language.checkLangCodeValid(langCode)) {
                        context.getConfig().setLanguage(new Language(langCode));
                    }
                    log.info("Processing file: {}", resultFile);
                    processFileTranslation(jsonFilePath, resultFile);
                } else {
                    context.decrementExpectedFilesCount();
                    log.warn("Extract file not found: {}", jsonFilePath);
                }
            } catch (Exception e) {
                log.error("Replace action file error", e);
                context.markFailed();
            }
        }
    }

    private void processExtractAction(List<String> sourceExtractFiles) {
        boolean extractSpecificSource = context.getSource() != null
                && !context.getSource().isEmpty()
                && "specific".equals(context.getMode());

        if (extractSpecificSource) {
            processFileTranslation(context.getSource(), null);
        } else if (!sourceExtractFiles.isEmpty()) {
            for (String sourceExtractFile : sourceExtractFiles) {
                log.info("--------------------------------------------");
                log.info(sourceExtractFile);
                log.info("--------------------------------------------");
                processFileTranslation(sourceExtractFile, null);
            }
        } else if (context.getSource() != null && !context.getSource().isEmpty()) {
            processFileTranslation(context.getSource(), null);
        }
    }

    private void processFileTranslation(String source, String target) {
        boolean variablesPassed = true;
        Config config = context.getConfig();
        String actionName = context.getAction().getCliName();

        if (source != null && !source.trim().isEmpty()) {
            source = resolvePath(source);
        } else {
            variablesPassed = false;
            log.error("No 'source' set");
            context.markFailed();
        }

        if (context.isReplaceAction()) {
            if (target != null && !target.trim().isEmpty()) {
                target = resolvePath(target);
            } else {
                variablesPassed = false;
                log.error("No 'target' set");
                context.markFailed();
            }
        }

        if (!variablesPassed) {
            log.error("No output - please check your inputs: action={}, source={}, target={}",
                    actionName, source, target);
            return;
        }

        if (context.isExtractAction()) {
            String fileLangCode = Language.getLangCodeFromFilePath(source);
            if (!fileLangCode.isEmpty() && Language.checkLangCodeValid(fileLangCode)) {
                config.setLanguage(new Language(fileLangCode));
            }
        } else if (context.isReplaceAction() && target != null) {
            String fileLangCode = Language.getLangCodeFromFilePath(target);
            if (!fileLangCode.isEmpty() && Language.checkLangCodeValid(fileLangCode)) {
                config.setLanguage(new Language(fileLangCode));
            }
        }

        String input;
        try {
            input = new String(Files.readAllBytes(Paths.get(source)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read source file: {}", source, e);
            context.markFailed();
            return;
        }

        TranslationConfig translationConfig = config.makeTranslationConfig(source, target, context.getWorkingDirectory());
        Translation translation = new Translation(translationConfig);

        if (!translation.validAction(actionName)) {
            log.error("Invalid action: {}", actionName);
            context.markFailed();
            return;
        }

        if (context.isReplaceAction()) {
            String targetString;
            try {
                targetString = new String(Files.readAllBytes(Paths.get(target)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Failed to read target file: {}", target, e);
                context.markFailed();
                return;
            }

            TranslationReplaceResult replaceResults = translation.replaceStrings(input, targetString, context);
            JamlinFiles.outputReplaceResultFiles(context, replaceResults, target);
        } else {
            TranslationExtractResult resultObject = translation.extractStrings(input);
            JamlinFiles.outputExtractResultFile(context, resultObject, new File(source), translation);
        }
    }

    private String resolveMode() {
        if (context.isReplaceAction()) {
            if (hasValue(context.getSource()) && hasValue(context.getTarget()) && hasValue(context.getLanguage())) {
                return "specific";
            }
            if (hasValue(context.getSource()) && hasValue(context.getTarget())) {
                return "semiautomatic";
            }
        } else {
            if (hasValue(context.getSource()) && hasValue(context.getLanguage())) {
                return "specific";
            }
            if (hasValue(context.getSource())) {
                return "semiautomatic";
            }
        }
        return "automatic";
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }

    private String resolvePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return path;
        }
        path = path.trim();
        if (!path.contains(File.separator)) {
            return context.getWorkingDirectory() + File.separator + path;
        }
        return path;
    }

    private int resolveExitCode() {
        if (context.hasFailed()) {
            return 1;
        }
        if (context.getExportedFilesCount() < context.getExpectedFilesCount()) {
            log.warn("Exported {} files, expected {}",
                    context.getExportedFilesCount(), context.getExpectedFilesCount());
            return 1;
        }
        return 0;
    }
}
