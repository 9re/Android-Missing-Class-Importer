package mn.uwvm.tools.classimporter.command;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class CopyCommand extends Command {
    private final File mFrom;
    private final File mTo;
    public CopyCommand(File from, File to) {
        mFrom = from;
        mTo = to;
    }
    
    @Override
    public void exec() {
        try {
            FileUtils.copyFile(mFrom, mTo);
        } catch (IOException e) {
            setError(e);
        }
    }
    
    @Override
    public void dryRun() {
        File parentDir = mTo.getParentFile();
        if (!parentDir.exists()) {
            System.out.println("mkdir -p " + parentDir.getAbsolutePath());
        }
        System.out.println("cp " + mFrom.getAbsolutePath() + " " + mTo.getAbsolutePath());
    }
}
