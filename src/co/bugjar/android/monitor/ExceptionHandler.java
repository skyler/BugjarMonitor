/*
 * Copyright 2012 Julius S. Slade 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.bugjar.android.monitor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;


/**
 * Bugjar exception handler
 * 
 * @author Skyler Slade <jsslade@gmail.com>
 */
class ExceptionHandler implements UncaughtExceptionHandler {

    private final String filesPath;
    private final TrackingValues trackingValues;
    private final UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    ExceptionHandler(String filesPath, TrackingValues trackingValues,
            UncaughtExceptionHandler defaultUncaughtExceptionhandler) {
        
        this.filesPath = filesPath;
        this.trackingValues = trackingValues;
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

        File f = new File(filesPath);
        if (!f.exists()) {
            f.mkdirs();
        }

        Log.d(BugjarMonitor.TAG, "writing uncaught stack trace to " + filename);
        try {
            new File(filename).createNewFile();
            PrintWriter writer = new PrintWriter(filename);
            // write the context
            writer.append(trackingValues.toString());
            // write the stack trace
            e.printStackTrace(writer);
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            Log.e(BugjarMonitor.TAG, "caught " + ioe.getClass().getSimpleName()
                    + "writing to " + filename + ": " + ioe.getMessage(), ioe);
        } finally {
            // safe to rethrow this from our thread?
            defaultUncaughtExceptionHandler.uncaughtException(thread, e);
        }
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
