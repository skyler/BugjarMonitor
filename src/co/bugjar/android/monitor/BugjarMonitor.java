package co.bugjar.android.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
    static final String BM_VERSION = "20120122";
    static final String TAG = BugjarMonitor.class.getSimpleName();
    
    private static final String BJ_SERVER = "http://bugjar.git/server.php";
    
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
    private BugjarMonitor(String packageName, String apiKey,
            String versionName, int versionCode) {
        
        this.filesDir = "/Android/data/" + packageName;
        this.apiKey = apiKey;
        this.versionName = versionName;
        this.versionCode = String.valueOf(versionCode);
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
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            BugjarMonitor m = new BugjarMonitor(packageName, apiKey,
                    info.versionName, info.versionCode);
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
        
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final String widthPixels = String.valueOf(metrics.widthPixels);
        final String heightPixels = String.valueOf(metrics.heightPixels);
 
        Runnable submitter = new Runnable() {

            @Override
            public void run() {
                HttpPost request = new HttpPost(BJ_SERVER);
                
                request.addHeader("version", BM_VERSION);
                request.addHeader("versionName", versionName);
                request.addHeader("versionCode", versionCode);
                request.addHeader("apiKey", apiKey);       
                request.addHeader("widthPixels", widthPixels);
                request.addHeader("heightPixels", heightPixels);
                
                HttpResponse response;
                try {
                    File d = new File(filesDir);
                    File[] stackTraces = d.listFiles(new ExceptionHandler.StackTraceFilter());
                    if (stackTraces.length > 0) {
                        for (int i = 0; i < stackTraces.length; i++) {
                            File f = stackTraces[i];
                            request.setEntity(new InputStreamEntity(
                                    new FileInputStream(f), f.length()));
                            response = httpClient.execute(request);
                            Log.d(TAG, response.getStatusLine().toString());
                        }
                    } else {
                        // say hello
                        response = httpClient.execute(request);
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
}
