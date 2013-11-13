package mn.uwvm.tools.classimporter.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class ExcludePatterns {
    private final List<Pattern> mPattern = new ArrayList<Pattern>();
    public ExcludePatterns() { }
    public ExcludePatterns(String path) {
        LineIterator it = null;
        try {
            it = FileUtils.lineIterator(new File(path), "UTF-8");
            while (it.hasNext()) {
                String line = it.nextLine();
                mPattern.add(Pattern.compile(line));
            }
        } catch (IOException e) {
            // do nothing
            e.printStackTrace();
        } finally {
            if (it != null) {
                it.close();
            }
        }
    }
    
    public boolean shouldExclude(String fqn) {
        for (Pattern pattern : mPattern) {
            if (pattern.matcher(fqn).find()) {
                return true;
            }
        }
        return false;
    }
}
