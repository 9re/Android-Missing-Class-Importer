package mn.uwvm.tools.classimporter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import mn.uwvm.tools.classimporter.filter.FileFilter;
import mn.uwvm.tools.classimporter.util.AndroidProjectWalker;
import mn.uwvm.tools.classimporter.util.CommandLineOptions;
import mn.uwvm.tools.classimporter.util.FileFilterFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

public class ClassImporter {
    private static final String CANNOT_FIND_SYMBOL = "cannot find symbol";
    private static final String ANDROID_SDK_ROOT = "ANDROID_SDK_ROOT";
    
    public static void main(String[] args) {
        {
            File sdkRoot = null;
            {
                Map<String, String> env = System.getenv();
                if (!env.containsKey(ANDROID_SDK_ROOT)) {
                    System.err.println("you must specify " + ANDROID_SDK_ROOT + " as system environment variable to use this tool.");
                    return;
                }
                sdkRoot = new File(env.get(ANDROID_SDK_ROOT));
            }
            if (!sdkRoot.isDirectory() || !sdkRoot.exists()) {
                System.err.println(ANDROID_SDK_ROOT + " is not valid directory.");
                return;
            }
            
            AndroidProjectWalker.setAndroidSdkRoot(sdkRoot);
        }
        
        /* determine following values from command line options */
        File targetProjectRoot = null;
        File sourceProjectRoot = null;
        final boolean verbose;
        final boolean dryRun;
        FileFilter fileFilter = null;
        {
            CommandLineOptions commandLineOptions = CommandLineOptions.newInstance();
            /* options */
            try {
                CommandLine cmd = new GnuParser().parse(commandLineOptions, args);
                if (cmd.hasOption(CommandLineOptions.HELP)) {
                    printUsage(commandLineOptions);
                    return;
                }
                targetProjectRoot = new File(cmd.getOptionValue(CommandLineOptions.TARGET_PROJECT)).getCanonicalFile();
                sourceProjectRoot = new File(cmd.getOptionValue(CommandLineOptions.SOURCE_PROJECT)).getCanonicalFile();
                verbose = cmd.hasOption(CommandLineOptions.VERBOSE);
                dryRun = cmd.hasOption(CommandLineOptions.DRY_RUN);
                if (cmd.hasOption(CommandLineOptions.FILTER)) {
                    URL url = new File(
                            cmd.getOptionValue(CommandLineOptions.FILTER))
                    .toURI()
                    .toURL();
                    ClassLoader classLoader = new URLClassLoader(new URL[]{url});
                    fileFilter = FileFilterFactory.newInstance(classLoader);
                    System.out.println("fileFilter: " + fileFilter);
                }
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                printUsage(commandLineOptions);
                return;
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            }
            
            if (!targetProjectRoot.exists() || !targetProjectRoot.isDirectory()) {
                System.err.println("target project dir " + targetProjectRoot.getAbsolutePath() + " does not exists!");
                return;
            }
            if (!sourceProjectRoot.exists() || !sourceProjectRoot.isDirectory()) {
                System.err.println("source project dir " + sourceProjectRoot.getAbsolutePath() + " does not exists!");
                return;
            }
        }
        
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        AndroidProjectWalker targetProjectWalker = new AndroidProjectWalker(targetProjectRoot);
        try {
            targetProjectWalker.walk();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(
            diagnostics,
            Locale.getDefault(),
            Charset.forName("utf-8"));
        List<File> sourceFiles = targetProjectWalker.getSourceFiles();
        for (File file : sourceFiles) {
            if (verbose) {
                System.out.println(file.getAbsolutePath());
            }
        }
        
        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        List<String> optionList = Arrays.asList(
            "-Xmaxerrs", Integer.MAX_VALUE + "",
            "-Xmaxwarns", Integer.MAX_VALUE + "",
            "-classpath",
            StringUtils.join(targetProjectWalker.getJarFiles(), ":")
            );
        javaCompiler.getTask(null, fileManager, diagnostics, optionList, null, compilationUnits).call();
        Map<String, Boolean> missingClassesMap = new HashMap<String, Boolean>();
        Pattern undefinedSymbol = Pattern.compile("cannot find\\s*symbol[\\s\\n]*symbol\\s*: (variable|class) (\\w+)");
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            String message = diagnostic.getMessage(Locale.getDefault());
            JavaFileObject source = diagnostic.getSource();
            if (message.contains(CANNOT_FIND_SYMBOL) &&
                source != null) {
                String substring = message.substring(message.indexOf(CANNOT_FIND_SYMBOL));
                Matcher matcher =
                    undefinedSymbol.matcher(
                        substring);
                if (matcher.find()) {
                    missingClassesMap.put(matcher.group(2), true);
                } else {
                    if (verbose) {
                        System.out.println("unmatched pattern: " + substring);
                    }
                }
            }
        }
        
        AndroidProjectWalker sourceProjectWalker = new AndroidProjectWalker(sourceProjectRoot);
        try {
            sourceProjectWalker.walk();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        
        List<String> missingClasses = new ArrayList<String>(missingClassesMap.keySet());
        Collections.sort(missingClasses);
        Map<String, File> sourceSourceMap = sourceProjectWalker.sourceMap();
        Map<String, File> targetSourceMap = targetProjectWalker.sourceMap();
        if (fileFilter == null) {
            fileFilter = FileFilterFactory.getDefault();
        }
        for (String missing : missingClasses) {
            if (!targetSourceMap.containsKey(missing) &&// to avoid override copy
                sourceSourceMap.containsKey(missing)) {
                File sourceFile = sourceSourceMap.get(missing);
                fileFilter.copy(sourceFile, sourceProjectRoot, targetProjectRoot, dryRun);
            }
        }
        
        try {
            fileManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void printUsage(CommandLineOptions commandLineOptions) {
        System.err.println("usage:");
        List<Option> options =new ArrayList<Option>((Collection<Option>)commandLineOptions.getOptions());
        Collections.sort(options, new Comparator<Option>(){
            @Override
            public int compare(Option o0, Option o1) {
                if (o0.isRequired()) {
                    if (o1.isRequired()) {
                        return o0.getOpt().compareTo(o1.getOpt());
                    } else {
                        return -1;
                    }
                } else {
                    if (o1.isRequired()) {
                        return 1;
                    } else {
                        return o0.getOpt().compareTo(o1.getOpt());
                    }
                }
            }
        });
        for (Option option : options) {
            StringBuilder message = new StringBuilder();
            message
                .append("    -")
                .append(option.getOpt())
                .append(", --")
                .append(option.getLongOpt())
                .append(" ");
            if (option.isRequired()) {
                message.append("[required] ");
            }
            message.append(option.getDescription());
            System.err.println(message);
        }
    }
}
