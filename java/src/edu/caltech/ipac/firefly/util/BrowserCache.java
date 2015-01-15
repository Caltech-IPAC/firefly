/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;
/**
 * User: roby
 * Date: 5/27/11
 * Time: 2:05 PM
 */


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.storage.client.StorageEvent;
import com.google.gwt.storage.client.StorageMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class BrowserCache {
    private static final String SEP = "___";
    private static final long NO_EXPIRATION= 0;

    private static final Map<String,String> resolveCache= getCacheMap();
    private static List<KeyHandler> _storageHandlerList = new ArrayList<KeyHandler>(10);
    private static boolean _enableHandlers= true;


    public static boolean isCached(String key) { return resolveCache.containsKey(key); }
    public static boolean isPerm() { return Storage.isLocalStorageSupported(); }

    public static String get(String key) {
        String retval= null;
        if (resolveCache.containsKey(key)) {
            String entryStr= resolveCache.get(key);
            if (entryStr!=null) {
                CacheEntry entry= CacheEntry.parse(entryStr);
                if (entry!=null) {
                    long lifeSpanMills= entry.getLifeSpanSecs() * 1000;
                    if (lifeSpanMills==NO_EXPIRATION) {
                        retval= entry.getData();
                    }
                    else {
                        long saveDate= entry.getTime();
                        if ((saveDate+lifeSpanMills) > System.currentTimeMillis()) {
                            retval= entry.getData();
                        }
                        else {
                            resolveCache.remove(key);
                        }
                    }
                }
            }
        }
        return retval;
    }

    public static void put(String key, String data) {
        CacheEntry entry= new CacheEntry(data);
        _enableHandlers= false;
        resolveCache.put(key,entry.toString());
        _enableHandlers= true;
    }

    public static void put(String key, String data, long lifespanInSecs) {
        CacheEntry entry= new CacheEntry(data,lifespanInSecs);
        _enableHandlers= false;
        resolveCache.put(key,entry.toString());
        _enableHandlers= true;
    }


    private static Map<String,String> getCacheMap() {
        Storage storage= Storage.getLocalStorageIfSupported();
        Map<String,String> retval;
        if (storage!=null) {
            retval= new StorageMap(storage);
            try {
                addStorageEventHandler0();
            } catch (Throwable e) {
               // no support for storage events
            }
//            Storage.addStorageEventHandler(new StorageEvent.Handler() {
//                public void onStorageChange(StorageEvent ev) { fireStorageChange(ev); }
//            });
        }
        else {
            retval= new HashMap<String,String>(43);
        }
        return retval;
    }

    public static HandlerRegistration addHandlerForKey(String key, StorageEvent.Handler handler) {
        WebAssert.argTst( (handler!=null && key!=null), "handler and key must not be null" ) ;
        final KeyHandler kh= new KeyHandler(key,handler);
        _storageHandlerList.add(kh);

        return new HandlerRegistration() {
            public void removeHandler() { _storageHandlerList.remove(kh); }
        };
    }


    private static void fireStorageChange(StorageEvent ev) {
        if (_enableHandlers) {
            for(KeyHandler kh : _storageHandlerList) {
                if (kh.getKey().equals(ev.getKey())) {
                    kh.getHandler().onStorageChange(ev);
                }
            }
        }
    }


    private static class KeyHandler {
        final StorageEvent.Handler handler;
        final String key;

        public KeyHandler(String key, StorageEvent.Handler handler) {
            this.key= key;
            this.handler= handler;
        }
        public StorageEvent.Handler getHandler() { return handler; }
        public String getKey() { return key; }

    }

    private static class CacheEntry {
        private final long time;
        private final String data;
        private final long lifeSpanSecs;

        public CacheEntry(String data, long time, long lifeSpanSecs) {
            this.time= time;
            this.data= data;
            this.lifeSpanSecs= lifeSpanSecs;
        }

        public CacheEntry(String data) { this(data, System.currentTimeMillis(), NO_EXPIRATION); }
        public CacheEntry(String data, long lifeSpanSecs) { this(data, System.currentTimeMillis(), lifeSpanSecs); }

        public String getData()       { return data;}
        public long getTime()         { return time;}
        public long getLifeSpanSecs() { return lifeSpanSecs;}

        public String toString() { return time + SEP + lifeSpanSecs + SEP + data; }

        public static CacheEntry parse(String inStr) {
            String s[]= inStr.split(SEP,3);
            CacheEntry entry= null;
            if (s.length==3) {
                try {
                    long time= Long.parseLong(s[0]);
                    long lifeSpanSecs= Long.parseLong(s[1]);
                    String data= s[2];
                    entry= new CacheEntry(data, time, lifeSpanSecs);
                } catch (NumberFormatException e) {
                    entry= null;
                }
            }
            return entry;
        }

    }

    protected static JavaScriptObject jsHandler;

    protected static native void addStorageEventHandler0() /*-{
        @edu.caltech.ipac.firefly.util.BrowserCache::jsHandler = $entry(function(event) {
            @edu.caltech.ipac.firefly.util.BrowserCache::fireStorageChange(Lcom/google/gwt/storage/client/StorageEvent;)(event);
        });
        $wnd.addEventListener("storage", @edu.caltech.ipac.firefly.util.BrowserCache::jsHandler, false);
    }-*/;

}

