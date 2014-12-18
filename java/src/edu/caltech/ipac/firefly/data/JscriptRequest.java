package edu.caltech.ipac.firefly.data;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: roby
 * Date: Jun 18, 2010
 * Time: 3:27:03 PM
 */


/**
 * @author Trey Roby
 */
public class JscriptRequest extends JavaScriptObject {

    protected JscriptRequest() {}

    public final native String getParam(String param) /*-{
          if (param in this) {
              return this[param];
          }
          else {
              return null;
          }
    }-*/;

    public final native boolean containsKey(String param) /*-{
        return param in this;
    }-*/;

    public final Set<String> keySet() {
        HashSet<String> s = new HashSet<String>();
        addKeys(s);
        return s;
    }

    public final Map<String,String> asMap() {
        HashSet<String> set = new HashSet<String>();
        addKeys(set);
        HashMap<String,String> map= new HashMap<String, String>(33);
        for(String key : set) {
            map.put(key,getParam(key));
        }
        return map;
    }



    private final native void addKeys(HashSet<String> s) /*-{
        for (var key in this) {
            if (this.hasOwnProperty(key)) {
                s.@java.util.HashSet::add(Ljava/lang/Object;)(key);
            }
        }
    }-*/;

}

