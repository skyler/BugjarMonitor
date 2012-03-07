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
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
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
 */
public class BugjarMonitor
{
    static final int VERSION = 20120122;
    static final String TAG = BugjarMonitor.class.getSimpleName();
    
    private static final String BJ_SERVER = "http://bugjar.git/monitor_client.php";
    //private static final String BJ_SERVER = "http://www.google.com";
    private static final String STACK_TRACES_PATH = "/Bugjar/traces";
    
    private final String filesDir;
    private final TrackingValues trackingValues;
    
    /**
     * @param packageName the application package name
     * @param apiKey the Bugjar account API key
     * @param versioName the application version name
     * @param versionCode the application version code
     * @param clientIdentifier some token to unique identify the device or null if none
     */
    private BugjarMonitor(String filesDir, String apiKey, String versionName,
            int versionCode, DisplayMetrics displayMetrics,
            String deviceIdentifier)
    {
        
        this.filesDir = filesDir;
        this.trackingValues = new TrackingValues(apiKey, versionName,
                String.valueOf(versionCode),
                String.valueOf(displayMetrics.widthPixels),
                String.valueOf(displayMetrics.heightPixels),
                deviceIdentifier);
    }
    
    /**
     * Get the Bugjar exception handler
     */
    private ExceptionHandler exceptionHandler()
    {
        return new ExceptionHandler(filesDir, trackingValues,
                Thread.getDefaultUncaughtExceptionHandler());
    }
    
    /**
     * Initialize the monitor to handle uncaught exceptions
     * 
     * @param packageName the application package name
     * @param apiKey the account API key
     */
    public static void initialize(Context context, String apiKey,
            String deviceIdentifier)
    {
        String packageName = context.getPackageName();
        
        String filesDir = Environment.getExternalStorageDirectory()
                + "/Android/data/" + packageName + STACK_TRACES_PATH;
        
        Log.d(TAG, "files dir is " + filesDir);
        
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            BugjarMonitor m = new BugjarMonitor(filesDir, apiKey,
                    info.versionName, info.versionCode, context.getResources()
                            .getDisplayMetrics(), deviceIdentifier);
            
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
        
        Runnable submitter = new Runnable() {

            @Override
            public void run() {
                
                try {
                    File d = new File(filesDir);
                    File[] stackTraces = d.listFiles(new ExceptionHandler.StackTraceFilter());
                    if (stackTraces != null && stackTraces.length > 0) {
                        
                        HttpPost request = new HttpPost(BJ_SERVER + "?m=stackTraces");
                        
                        for (int i = 0; i < stackTraces.length; i++) {
                            File f = stackTraces[i];
                            Log.d(TAG, "sending " + f.getAbsolutePath());
                            
                            request.setEntity(new InputStreamEntity(
                                    new FileInputStream(f), f.length()));
                            HttpResponse response = httpClient.execute(request);
                            
                            if (response.getStatusLine().getStatusCode() == 200) {
                                f.delete();
                                readResponse(response);
                            } else {
                                Log.w(TAG, "posting stack traces the Bugjar server responded "
                                        + response.getStatusLine().toString());
                            }
                        }
                    } else {
                        // if no stack traces to report, ping home for stats tracking
                        HttpPost request = new HttpPost(BJ_SERVER + "?m=ping");
                        request.setEntity(new StringEntity(trackingValues.toString()));
                        
                        HttpResponse response = httpClient.execute(request);
                        Log.d(TAG, "pinging home, the Bugjar server responded "
                                + response.getStatusLine().toString());
                        
                        readResponse(response);
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
     * Read the response and log if there's a newer version of the monitor available
     * @param response
     */
    private void readResponse(HttpResponse response)
    {
        try {
            InputStream in = response.getEntity().getContent();
            
            byte[] buffer = new byte[4096];
            int length = in.read(buffer);
            if (length > 0) {
                Log.w(TAG, "length is " + length);
                int currentVersion = Integer.parseInt(new String(buffer, 0,
                        length, "UTF-8"));

                if (currentVersion > VERSION) {
                    Log.w(TAG, "a newer version of the Bugjar jar is available--download it at http://www.getbugjar.com");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getClass().getSimpleName()
                    + " caught reading bugjar response: " + e.getMessage());
        }
    }
    
}
