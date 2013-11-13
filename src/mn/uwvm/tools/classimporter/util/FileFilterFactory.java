package mn.uwvm.tools.classimporter.util;

import java.io.File;
import java.util.ServiceLoader;

import mn.uwvm.tools.classimporter.command.Command;
import mn.uwvm.tools.classimporter.command.CopyCommand;
import mn.uwvm.tools.classimporter.filter.FileFilter;

public class FileFilterFactory {
    public static FileFilter getDefault() {
        ServiceLoader<FileFilter> ldr = ServiceLoader.load(FileFilter.class);
        for (FileFilter filter : ldr) {
            System.out.println(filter + "");
            return filter;
        }
        throw new Error("implementation of " + FileFilter.class.getCanonicalName() +" not registerd!");
    }
    public static FileFilter newInstance(ClassLoader classLoader) {
        ServiceLoader<FileFilter> ldr = ServiceLoader.load(FileFilter.class, classLoader);
        for (FileFilter filter : ldr) {
            if (!filter.getClass().equals(DefaultFileFilter.class)) {
                return filter;
            }
        }
        return null;
    }
    public static class DefaultFileFilter implements FileFilter {
        @Override
        public void copy(File source, File sourceProjectRoot, File destProjectRoot, boolean dryRun) {
            File target = new File(
                source.getAbsolutePath()
                    .replace(sourceProjectRoot.getAbsolutePath(), destProjectRoot.getAbsolutePath()));
            Command cmd = 
                new CopyCommand(source, target);
            
            if (dryRun) {
                cmd.dryRun();
            } else {
                cmd.exec();
            }
        }
    }
}

