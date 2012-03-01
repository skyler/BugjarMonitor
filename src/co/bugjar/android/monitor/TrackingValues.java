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

/**
 * Bugjar tracking values
 * 
 * @author Skyler Slade <jsslade@gmail.com>
 */
class TrackingValues {
    
    final String apiKey;
    final String versionName;
    final String versionCode;
    final String widthPixels;
    final String heightPixels;
    
    TrackingValues(String apiKey, String versionName, String versionCode,
            String widthPixels, String heightPixels) {
        
        this.apiKey = apiKey;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.widthPixels = widthPixels;
        this.heightPixels = heightPixels;
    }
    
    /**
     * Get tracking values as a colon-delimited, separated by newline, value pair
     * @param currentTime
     * @return
     */
    @Override
    public synchronized String toString() {
        StringBuilder values = new StringBuilder()
            .append("version:").append(BugjarMonitor.VERSION).append("\n")
            .append("versionName:").append(versionName).append("\n")
            .append("versionCode:").append(versionCode).append("\n")
            .append("time:").append(System.currentTimeMillis()).append("\n")
            .append("\r\n");
        return values.toString();
    }
}
