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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
