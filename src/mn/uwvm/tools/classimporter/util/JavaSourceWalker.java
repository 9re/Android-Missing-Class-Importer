package mn.uwvm.tools.classimporter.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class JavaSourceWalker {
    private final File mRootDir;
    private final List<File> mSources = new ArrayList<File>();
    private final FilenameFilter mFilenameFilter = new FilenameFilter(){
        @Override
        public boolean accept(File dir, String name) {
            File file = new File(dir, name);
            if (file.isDirectory()) {
                return true;
            }
            return name.endsWith(".java");
        }
    }; 
    public JavaSourceWalker(String root) {
        mRootDir = new File(root);
    }
    public void walk() {
        mSources.clear();
        walk(mRootDir);
    }
    private void walk(File root) {
        for (File file : root.listFiles(mFilenameFilter)) {
            if (file.isDirectory()) {
                walk(file);
            } else {
                mSources.add(file);
            }
        }
    }
    public List<File> getSourceFiles() {
        return mSources;
    }
}
