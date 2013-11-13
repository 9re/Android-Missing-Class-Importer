package mn.uwvm.tools.classimporter.util;

import mn.uwvm.tools.classimporter.filter.FileFilter;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

@SuppressWarnings("serial")
public final class CommandLineOptions extends Options {
    public static final String TARGET_PROJECT= "target-project";
    public static final String SOURCE_PROJECT = "source-project";
    public static final String VERBOSE = "verbose";
    public static final String DRY_RUN = "dry-run";
    public static final String HELP = "help";
    public static final String FILTER = "filter";
    public static final String EXCLUDE_RULE = "exclude-rule";
    
    private CommandLineOptions() {}
    
    public static CommandLineOptions newInstance() {
        CommandLineOptions options = new CommandLineOptions();
        { // target project
            Option option = new Option("t",
                TARGET_PROJECT,
                true,
                "provide the path to the target project.");
            option.setRequired(true);
            options.addOption(option);
        }
        { // source project
            Option option = new Option("s",
                SOURCE_PROJECT,
                true,
                "provide the path to the source project.");
            option.setRequired(true);
            options.addOption(option);
        }
        { // verbose
            options.addOption("v", VERBOSE, false, "show verbose messages.");
        }
        { // dry-run
            options.addOption("r", DRY_RUN, false, "dry run. show only what operations will occurr.");
        }
        { // help
            options.addOption("h", HELP, false, "show this help.");
        }
        { // rule
            options.addOption(
                "f", FILTER, true,
                "provide source code filter which implements " + FileFilter.class.getCanonicalName() +
                " as its service. this filter is loaded dynamically.");
        }
        { // exclude-rule
            options.addOption("e", EXCLUDE_RULE, true, "provide exclude pattern file.");
        }
        
        return options;
    }
}
