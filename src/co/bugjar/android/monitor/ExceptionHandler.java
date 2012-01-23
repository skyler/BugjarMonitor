package co.bugjar.android.monitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;

/**
 * Bugjar exception handler
 * 
 * @author Skyler Slade <jsslade@gmail.com>
 * @see http://www.bugjar.co
 */
class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String STACK_TRACES_PATH = "/Bugjar/traces";

    private final String filesPath;
    private final String versionName;
    private final String versionCode;
    private final UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    ExceptionHandler(String filesPath, String versionName, String versionCode,
            UncaughtExceptionHandler defaultUncaughtExceptionhandler) {
        
        this.filesPath = filesPath + STACK_TRACES_PATH;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.defaultUncaughtExceptionHandler = defaultUncaughtExceptionhandler;
    }

    /**
     * Method invoked when the given thread terminates due to the given uncaught exception.
     * 
     * @param thread the crashed thread
     * @param e the exception
     */
    @Override
    public void uncaughtException(final Thread thread, final Throwable e) {
        final long time = System.currentTimeMillis();
        final String filename = filesPath + "/" + time + ".stacktrace";

        Runnable writer = new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder("version:")
                        .append(BugjarMonitor.BM_VERSION).append("\n")
                        .append("versionName:").append(versionName)
                        .append("\n").append("versionCode").append(versionCode)
                        .append("\n").append("time:").append(time).append("\n")
                        .append("\r\n");

                Log.d(BugjarMonitor.TAG, "writing uncaught stack trace to " + filename);
                try {
                    FileWriter fw = new FileWriter(filename);
                    BufferedWriter buffered = new BufferedWriter(fw);
                    // write the context
                    buffered.write(builder.toString());
                    // write the stack trace
                    e.printStackTrace(new PrintWriter(fw));
                    buffered.close();
                } catch (IOException ioe) {
                    Log.e(BugjarMonitor.TAG, "caught "
                            + ioe.getClass().getSimpleName() + "writing to " + filename
                            + ": " + ioe.getMessage(), ioe);
                } finally {
                    // safe to rethrow this from our thread?
                    defaultUncaughtExceptionHandler.uncaughtException(thread, e);
                }
            }
        };

        new Thread(writer).start();
    }

    /**
     * Search for stack traces
     */
    static class StackTraceFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.endsWith(".stacktrace");
        }
    }
}
