package edu.caltech.ipac.firefly.core;

import com.google.gwt.storage.client.Storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to get/set user preferences
 * @author tatianag
 * @version $Id: Preferences.java,v 1.6 2012/10/25 23:40:36 loi Exp $
 */
public class Preferences {

    private static final String PREFIX = "pref.";
    private static final HashMap<String,String> localPrefMap = new HashMap<String,String>(20);

    private static LoginManager loginManager =  Application.getInstance().getLoginManager();
    private static boolean notBulkUpdate = true;

    public static String get(String prefname) {
        String value = null;
        // check browser session storage
        Storage sessionStorage = Storage.getSessionStorageIfSupported();
        if (sessionStorage != null) {
            value = sessionStorage.getItem(PREFIX+prefname);
        }

        // check browser local storage
        if (value == null) {
            Storage localStorage = Storage.getLocalStorageIfSupported();
            if (localStorage != null) {
                value = localStorage.getItem(PREFIX+prefname);
            }
        }

        // check map
        if (value==null) {
            value = localPrefMap.get(prefname);
        }
        return value;
    }

    public static String get(String prefname, String def) {
        String retval = get(prefname);
        return (retval==null ? def : retval);
    }

    public static float  getFloat(String prefname, float def) {
        String val = get(prefname);
        float  retval;
        try {
            if (val != null) retval= Float.parseFloat(val);
            else             retval= def;
        } catch (NumberFormatException e) {
            retval= def;
        }
        return retval;
    }

    public static double getDouble(String prefname, double def) {
        String val = get(prefname);
        double retval;
        try {
            if (val != null) retval= Double.parseDouble(val);
            else             retval= def;
        } catch (NumberFormatException e) {
            retval= def;
        }
        return retval;
    }

    public static int getInt(String prefname, int def) {
        String val = get(prefname);
        int    retval;
        try {
            if (val != null) retval= Integer.parseInt(val);
            else             retval= def;
        } catch (NumberFormatException e) {
            retval= def;
        }
        return retval;
    }

    public static long getLong(String prefname, long def) {
        String val = get(prefname);
        long    retval;
        try {
            if (val != null) retval= Long.parseLong(val);
            else             retval= def;
        } catch (NumberFormatException e) {
            retval= def;
        }
        return retval;
    }

    public static Set<String> getPrefNames() {
        Set<String> names = new HashSet<String>(20);
        {
            // check session storage for preferences
            Storage sessionStorage = Storage.getSessionStorageIfSupported();
            if (sessionStorage != null) {
                String key;
                for (int i = 0; i<sessionStorage.getLength(); i++) {
                    key = sessionStorage.key(i);
                    if (key.startsWith(PREFIX)) {
                        names.add(key.substring(PREFIX.length()));
                    }
                }
            }
        }

        {
            // check browser local storage
            Storage localStorage = Storage.getLocalStorageIfSupported();
            if (localStorage != null) {
                String key;
                for (int i = 0; i<localStorage.getLength(); i++) {
                    key = localStorage.key(i);
                    if (key.startsWith(PREFIX)) {
                        names.add(key.substring(PREFIX.length()));
                    }
                }
            }
        }

        // preferences in the local map
        names.addAll(localPrefMap.keySet());

        return names;
    }

    public static boolean getBoolean(String prefname, boolean def) {
        String val = get(prefname);
        return (val == null ? def : Boolean.valueOf(val));
    }

    public static void set(String prefname, String prefvalue, boolean sessionScope) {

       if (sessionScope) {
            // put preference into session storage, if available
            // otherwise into local static map
            Storage sessionStorage = Storage.getSessionStorageIfSupported();
            if (sessionStorage != null) {
                //oldvalue = sessionStorage.getItem(PREFIX+prefname);
                if (prefvalue == null) {
                    sessionStorage.removeItem(PREFIX+prefname);
                } else {
                    sessionStorage.setItem(PREFIX+prefname, prefvalue);
                }
            } else {
                //oldvalue =  localPrefMap.get(prefname);
                localPrefMap.put(prefname, prefvalue);
            }
        }

        else {
            // check browser local storage
            Storage localStorage = Storage.getLocalStorageIfSupported();
            String oldvalue;
            if (localStorage != null) {
                oldvalue = localStorage.getItem(PREFIX+prefname);
                if (prefvalue == null) {
                    localStorage.removeItem(PREFIX+prefname);
                } else {
                    localStorage.setItem(PREFIX+prefname, prefvalue);
                }
            } else {
                oldvalue =  localPrefMap.get(prefname);
                localPrefMap.put(prefname, prefvalue);
            }
           if (notBulkUpdate && loginManager != null && loginManager.isLoggedIn()) {
               if ((prefvalue != null && !prefvalue.equals(oldvalue)) ||
                       (oldvalue != null && !oldvalue.equals(prefvalue))) {
                   // logged in user - also put preferences to db
                   loginManager.setPreference(prefname, prefvalue);
               }
           }
       }

        // no need to fire event
        // fire event if preference value has changed 
        //if ((prefvalue != null && !prefvalue.equals(oldvalue)) ||
        //        (oldvalue != null && !oldvalue.equals(prefvalue))) {
        //    HashMap<String,String> prefmap = new HashMap<String,String>(1);
        //    prefmap.put(prefname,prefvalue);
        //    WebEventManager.getAppEvManager().fireEvent(new WebEvent(Preferences.class, Name.PREFERENCE_UPDATE, prefmap));
        //}
    }

    public static void set(String prefname, String prefvalue) {
        set(prefname, prefvalue, false);
    }


    public static void setFloat(String prefname, float value) {
        set(prefname, Float.toString(value));
    }

    public static void setDoublePreference(String prefname, double value) {
        set(prefname, Double.toString(value));
    }

    public static void setInt(String prefname, int value) {
        set(prefname, Integer.toString(value));
    }

    public static void setLong(String prefname, long value) {
        set(prefname, Long.toString(value));
    }

    public static void setBooleanPreference(String prefname, boolean value) {
        set(prefname, Boolean.toString(value));
    }

    // set prefs from the given map to local storage
    public static void bulkSet(Map<String, String> map, boolean sessionScope) {
        if (map != null && map.size() > 0) {
            Preferences.setBulkUpdate(true);
            try {
                for (String key : map.keySet()) {
                    Preferences.set(key, map.get(key), sessionScope);
                }
            } finally {
                Preferences.setBulkUpdate(false);
            }
        }
    }

    private static void setBulkUpdate(boolean isBulkUpdate) {
        notBulkUpdate = !isBulkUpdate;
    }
    
}
