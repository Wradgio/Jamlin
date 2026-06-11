package sk.cw.jamlin;

import com.beust.jcommander.Parameter;

public class CliOptions {

    @Parameter(names = {"--help", "-h"}, description = "Show usage information and exit")
    public boolean help;

    @Parameter(names = {"--action", "-a"}, description = "Action: extract (default) or replace")
    public String action;

    @Parameter(names = {"--source", "-s"}, description = "Source file: HTML/XML for extract, *-extract.json for replace")
    public String source;

    @Parameter(names = {"--target", "-t"}, description = "Target HTML/XML template (replace only)")
    public String target;

    @Parameter(names = {"--language", "-l"}, description = "Language code (e.g. en, sk, en_US)")
    public String language = "";

    @Parameter(names = {"--languageName", "-ln"}, description = "Optional display name for the language code")
    public String languageName = "";

    @Parameter(names = {"--dictionary", "-d"}, arity = 1, description = "Build project_dictionary.json on extract (default: true)")
    public boolean dictionary = true;

    @Parameter(names = {"--workingdir", "-w"}, description = "Working directory (default: current directory)")
    public String workingDirectory;

    @Parameter(names = {"--config", "-c"}, description = "Path to jamlin_config.json (default: <workingdir>/jamlin_config.json)")
    public String configPath;
}
