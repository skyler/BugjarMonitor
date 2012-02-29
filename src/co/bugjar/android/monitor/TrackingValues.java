package co.bugjar.android.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

class TrackingValues
{
    Map<String, String> values = new HashMap<String, String>();
    
    public synchronized void put(String key, String value)
    {
        values.put(key, value);
    }
    
    public synchronized List<NameValuePair> getNameValuePairs()
    {
        List<NameValuePair> nvp = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            nvp.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return nvp;
    }
}
