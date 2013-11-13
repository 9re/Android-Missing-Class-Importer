package mn.uwvm.tools.classimporter.filter;

import java.io.File;

public interface FileFilter {
    public void copy(File source, File sourceProjectRoot, File destProjectRoot, boolean dryRun);
}
