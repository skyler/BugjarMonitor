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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;


/**
 * Bugjar monitor
 * 
 * Logs stack traces and other useful information for application developers.
 * 
 * @author Skyler Slade <jsslade@gmail.com>
 * @see http://www.bugjar.co
 */
public class BugjarMonitor {
    static final String VERSION = "2012-01-22";
    static final String TAG = BugjarMonitor.class.getSimpleName();
    
    private static final String BJ_SERVER = "http://bugjar.git/server.php";
    private static final String STACK_TRACES_PATH = "/Bugjar/traces";
    
    private final TrackingValues trackingValues = new TrackingValues();
    private final String filesDir;
    private final String apiKey;
    private final String versionName;
    private final String versionCode;
    
    /**
     * @param packageName the application package name
     * @param apiKey the Bugjar account API key
     * @param versioName the application version name
     * @param versionCode the application version code
     */
    private BugjarMonitor(String filesDir, String apiKey, String versionName,
            int versionCode, DisplayMetrics displayMetrics) {
        
        this.filesDir = filesDir;
        this.apiKey = apiKey;
        this.versionName = versionName;
        this.versionCode = String.valueOf(versionCode);
        
        trackingValues.put("monitorVersion", VERSION);
        trackingValues.put("apiKey", apiKey);
        
        trackingValues.put("versionName", versionName);
        trackingValues.put("versionCode", String.valueOf(versionCode));
        trackingValues.put("apiKey", apiKey);
        
        trackingValues.put("widthPixels",
                String.valueOf(displayMetrics.widthPixels));
        
        trackingValues.put("heightPixels",
                String.valueOf(displayMetrics.heightPixels));
    }
    
    /**
     * Get the Bugjar exception handler
     */
    private ExceptionHandler exceptionHandler()
    {
        return new ExceptionHandler(filesDir, versionName, versionCode,
                Thread.getDefaultUncaughtExceptionHandler());
    }
    
    /**
     * Initialize the monitor to handle uncaught exceptions
     * 
     * @param packageName the application package name
     * @param apiKey the account API key
     */
    public static void initialize(Context context, String apiKey)
    {
        String packageName = context.getPackageName();
        
        String filesDir = Environment.getExternalStorageDirectory()
                + "/Android/data/" + packageName + STACK_TRACES_PATH;
        
        Log.d(TAG, "files dir is " + filesDir);
        
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            BugjarMonitor m = new BugjarMonitor(filesDir, apiKey,
                    info.versionName, info.versionCode, context.getResources()
                            .getDisplayMetrics());
            
            Thread.setDefaultUncaughtExceptionHandler(m.exceptionHandler());
            m.submitStackTraces(context);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "couldn't determine version name or code while initializing: " + e.getMessage());
        }
    }
    
    /**
     * Submit any saved stack traces
     * @param context
     */
    private void submitStackTraces(Context context)
    {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        
        final List<NameValuePair> values = trackingValues.getNameValuePairs();
        final int valuesSize = values.size();
 
        Runnable submitter = new Runnable() {

            @Override
            public void run() {
                HttpPost request = new HttpPost(BJ_SERVER);
                
                try {
                    File d = new File(filesDir);
                    File[] stackTraces = d.listFiles(new ExceptionHandler.StackTraceFilter());
                    if (stackTraces != null && stackTraces.length > 0) {
                        for (int i = 0; i < stackTraces.length; i++) {
                            File f = stackTraces[i];
                            Log.d(TAG, "sending " + f.getAbsolutePath());
                            String trace = readStackTrace(f);
                            
                            values.add("stackTrace", trace);
                            
                            request.setEntity(new UrlEncodedFormEntity(values));
                            HttpResponse response = httpClient.execute(request);
                            
                            values.remove(valuesSize);
                            
                            if (response.getStatusLine().getStatusCode() == 200) {
                                f.delete();
                            } else {
                                Log.w(TAG, "Bugjar server returned " + response.getStatusLine().toString());
                            }
                        }
                    } else {
                        // no stack traces to send; say hello!
                        request.setEntity(new UrlEncodedFormEntity(values));
                        HttpResponse response = httpClient.execute(request);
                        Log.d(TAG, response.getStatusLine().toString());
                    }
                } catch(IOException e) {
                    Log.e(TAG, e.getClass().getSimpleName() + " caught submitting stack trace: "
                            + e.getMessage());
                }
            }
        };
        
        new Thread(submitter).start();
    }
    
    /**
     * Read stack trace lines into a string
     * @throws IOException 
     */
    private static String readStackTrace(File file) throws IOException
    {
        byte[] b = new byte[4096];
        FileInputStream in = new FileInputStream(file);
        int bytesRead = 0;
        StringBuilder builder = new StringBuilder();
        while ((bytesRead += in.read(b, bytesRead, 4096)) != -1) {
            builder.append(new String(b));
        }
        return builder.toString();
    }
}
