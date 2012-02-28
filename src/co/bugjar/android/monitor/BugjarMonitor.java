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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
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
    static final String BM_VERSION = "2012-01-22";
    static final String TAG = BugjarMonitor.class.getSimpleName();
    
    private static final String BJ_SERVER = "http://bugjar.git/server.php";
    private static final String STACK_TRACES_PATH = "/Bugjar/traces";
    
    private final String filesDir;
    private final String apiKey;
    private final String versionName;
    private final String versionCode;
    
    /**
     * @param filesDir directory to write stack traces
     * @param apiKey the Bugjar account API key
     * @param versioName the application version name
     * @param versionCode the application version code
     */
    private BugjarMonitor(String filesDir, String apiKey, String versionName,
            int versionCode) {
        
        this.filesDir = filesDir;
        this.apiKey = apiKey;
        this.versionName = versionName;
        this.versionCode = String.valueOf(versionCode);
    }
    
    /**
     * Get the Bugjar exception handler
     * 
     * @param context a Context reference
     */
    private ExceptionHandler exceptionHandler(Context context)
    {
        return new ExceptionHandler(apiKey, filesDir, versionName, versionCode, context,
                Thread.getDefaultUncaughtExceptionHandler());
    }
    
    /**
     * Initialize the monitor to handle uncaught exceptions
     * 
     * @param context a Context reference
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
                    info.versionName, info.versionCode);
            
            Thread.setDefaultUncaughtExceptionHandler(m.exceptionHandler(context));
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
                            
                            StringBuilder builder = new StringBuilder();
                            
                            BufferedReader reader = new BufferedReader(new FileReader(f));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                builder.append(line).append("\n");
                            }
                            
                            request.setEntity(new StringEntity(builder.toString()));
                            
                            HttpResponse response = httpClient.execute(request);
                            if (response.getStatusLine().getStatusCode() == 200) {
                                f.delete();
                            } else {
                                Log.w(TAG, "Bugjar server responded " + response.getStatusLine().toString());
                            }
                        }
                    } else {
                        // no stack traces to send, ping home
                        StringBuilder builder = new StringBuilder()
                                 .append("monitorVersion:").append(BugjarMonitor.BM_VERSION).append("\n")
                                 .append("apiKey:").append(apiKey).append("\n")
                                 .append("versionName:").append(versionName).append("\n")
                                 .append("versionCode:").append(versionCode).append("\n")
                                 .append("checkin:true");
                        
                        request.setEntity(new StringEntity(builder.toString()));
                        HttpResponse response = httpClient.execute(request);
                        
                        if (response.getStatusLine().getStatusCode() != 200) {
                            Log.w(TAG, "Bugjar server responded " + response.getStatusLine().toString());
                        }
                    }
                } catch(IOException e) {
                    Log.e(TAG, e.getClass().getSimpleName() + " caught submitting stack trace: "
                            + e.getMessage());
                }
            }
        };
        
        new Thread(submitter).start();
    }
    
    static StringBuilder getContextInfo(Context context)
    {
        // put together versionName, code, sdk version, etc.
    }
}
