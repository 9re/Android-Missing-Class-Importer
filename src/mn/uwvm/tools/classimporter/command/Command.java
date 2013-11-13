package mn.uwvm.tools.classimporter.command;

public abstract class Command {
    private Throwable mError;

    public abstract void exec();
    public abstract void dryRun();
    
    protected synchronized void setError(Throwable th) {
        mError = th;
    }
    
    public synchronized Throwable getError() {
        return mError;
    }
}
