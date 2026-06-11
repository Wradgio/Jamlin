package sk.cw.jamlin;

import com.beust.jcommander.JCommander;

final class CliHelp {

    private CliHelp() {
    }

    static void printUsage(JCommander commander) {
        System.out.println("JaMLin — Java Markup Language Internalisation");
        System.out.println("Extract translatable strings from HTML/XML and write localized copies.");
        System.out.println();
        commander.setProgramName("java -jar jamlin-jar-with-dependencies.jar");
        commander.usage();
        printModesAndExamples();
    }

    private static void printModesAndExamples() {
        System.out.println();
        System.out.println("Actions:");
        System.out.println("  extract   Read HTML/XML and write *-extract.json (or project_dictionary.json)");
        System.out.println("  replace   Read *-extract.json and write localized HTML/XML files");
        System.out.println();
        System.out.println("Modes (per action):");
        System.out.println("  specific       --source and --language set (extract) or --source, --target, --language (replace)");
        System.out.println("  semiautomatic  --source set (extract) or --source and --target (replace)");
        System.out.println("  automatic      Uses jamlin_config.json only (default action: extract)");
        System.out.println();
        System.out.println("Config:");
        System.out.println("  Requires jamlin_config.json in the working directory unless --config is set.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar jamlin-jar-with-dependencies.jar --action extract --source page.html --language sk");
        System.out.println("  java -jar jamlin-jar-with-dependencies.jar --action extract --source page.html");
        System.out.println("  java -jar jamlin-jar-with-dependencies.jar");
        System.out.println("  java -jar jamlin-jar-with-dependencies.jar --action replace --source page-extract.json --target page.html --language en");
        System.out.println("  java -jar jamlin-jar-with-dependencies.jar --action replace --source page-extract.json --target page.html");
        System.out.println("  java -jar jamlin-jar-with-dependencies.jar --workingdir /path/to/project --config /path/to/jamlin_config.json");
        System.out.println();
        System.out.println("Exit codes: 0 success, 1 error, 2 invalid arguments");
    }
}
