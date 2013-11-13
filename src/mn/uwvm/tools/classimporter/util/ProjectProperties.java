package mn.uwvm.tools.classimporter.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class ProjectProperties {
    final File mPath;
    final Map<String, String> mProperties = new HashMap<String, String>();
    public ProjectProperties(File projectRoot) {
        mPath = new File(projectRoot, "project.properties");
    }
    
    public void read() throws IOException {
        LineIterator it = null;
        try {
            it = FileUtils.lineIterator(mPath, "UTF-8");
            while (it.hasNext()) {
                String line = it.nextLine();
                int commentIndex = line.indexOf('#');
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex == -1 || eqIndex == line.length() - 1) {
                    continue;
                }
                mProperties.put(
                    line.substring(0, eqIndex),
                    line.substring(eqIndex + 1));
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (it != null) {
                it.close();
            }
        }
    }
    
    public Map<String, String> properties() {
        return new HashMap<String, String>(mProperties);
    }
}
