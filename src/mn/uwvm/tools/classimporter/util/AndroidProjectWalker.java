package mn.uwvm.tools.classimporter.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidProjectWalker {
    private static File sAndroidSdkRoot;
    private final File mPath;
    private final boolean mIsRoot;
    private final ProjectProperties mProjectProperties;
    private final JavaSourceWalker mSourceWalker;
    private final List<File> mSourceFiles = new ArrayList<File>();
    private final List<File> mJarFiles = new ArrayList<File>();
    private final Map<String, File> mSourceMap = new HashMap<String, File>();
    public AndroidProjectWalker(File projectRoot) {
        this(projectRoot, true);
    }
    public AndroidProjectWalker(File projectRoot, boolean isRoot) {
        mPath = projectRoot;
        mIsRoot = isRoot;
        mProjectProperties = new ProjectProperties(projectRoot);
        mSourceWalker = new JavaSourceWalker(mPath.getAbsolutePath());
    }
    
    public void walk() throws IOException {
        mProjectProperties.read();
        mSourceFiles.clear();
        mSourceMap.clear();
        mJarFiles.clear();
        
        if (mIsRoot) {
            String target = mProjectProperties.properties().get("target");
            if (target.startsWith("android-")) {
                addFrameworkJar(target, new File(getAndroidSdkRoot(), "platforms/" + target + "/android.jar"));
            } else if (target.startsWith("Google Inc.:Google APIs:")) {
                String version = target.substring("Google Inc.:Google APIs:".length());
                addFrameworkJar(target, new File(getAndroidSdkRoot(), "platforms/android-" + version + "/android.jar"));
                addFrameworkJar(target, new File(getAndroidSdkRoot(), "add-ons/addon-google_apis-google-" + version + "/libs/maps.jar"));
                System.out.println("google map version: " + version);
            } else {
                throw new RuntimeException("unsupported target: " + target);
            }
        }
        
        mSourceWalker.walk();
        for (Map.Entry<String, String> property : mProjectProperties.properties().entrySet()) {
            if (property.getKey().startsWith("android.library.reference.")) {
                File libraryPath = new File(mPath, property.getValue()).getCanonicalFile();
                AndroidProjectWalker libraryProject =
                    new AndroidProjectWalker(libraryPath, false);
                libraryProject.walk();
                List<File> sourceFiles = libraryProject.getSourceFiles();
                mSourceFiles.addAll(sourceFiles);
                mJarFiles.addAll(libraryProject.getJarFiles());
            }
        }
        mSourceFiles.addAll(mSourceWalker.getSourceFiles());
        mJarFiles.addAll(listJarFiles());
        for (File file : mSourceFiles) {
            String filename = file.getName();
            // TODO this approach is weak in duplicate class names
            mSourceMap.put(filename.replace(".java", ""), file);
        }
    }
    
    private void addFrameworkJar(String target, File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("framework jar file for " + target + " not found: " + file.getAbsolutePath());
        }
        mJarFiles.add(file);
    }
    public Map<String, File> sourceMap() {
        return mSourceMap;
    }
    
    private List<File> listJarFiles() {
        File[] files = new File(mPath, "libs").listFiles(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
            });
        if (files == null) {
            files = new File[0];
        }
        return Arrays.asList(files);
    }
    
    public List<File> getJarFiles() {
        return mJarFiles;
    }
    
    public List<File> getSourceFiles() {
        return mSourceFiles;
    }
    
    public static synchronized void setAndroidSdkRoot(File file) {
        sAndroidSdkRoot = file;
    }
    
    public static synchronized File getAndroidSdkRoot() {
        return sAndroidSdkRoot;
    }
}
