package sk.cw.jamlin;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Command line entry point for JaMLin.
 */
public class Main {

    public static void main(String... argv) {
        CliOptions options = new CliOptions();
        JCommander commander = JCommander.newBuilder()
                .addObject(options)
                .build();

        try {
            commander.parse(argv);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            System.err.println("Use --help for usage information.");
            System.exit(2);
        }

        if (options.help) {
            CliHelp.printUsage(commander);
            System.exit(0);
        }

        int exitCode = new JamlinApplication().run(options);
        System.exit(exitCode);
    }
}
