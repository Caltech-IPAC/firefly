/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 6/10/11
 * Time: 1:47 PM
 */


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Event;
import edu.caltech.ipac.firefly.util.BrowserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class JSLoad {

    private List<Loaded> _callbackList= new ArrayList<Loaded>(5);
    public Map<JavaScriptObject,Boolean> _map= new HashMap<JavaScriptObject,Boolean>(10);
    public boolean _allLoaded= false;


    public JSLoad(Loaded loaded, String... url) {
        _callbackList.add(loaded);
        Document document= Document.get();

        for(String u : url) {
            ScriptElement script= document.createScriptElement();
            script.setSrc(u);
            script.setType("text/javascript");
            _map.put(script,false);
            if (BrowserUtil.isIE() && BrowserUtil.getMajorVersion()<9) {
                addIEListener(this,script,loaded);
            }
            else {
                addListener(this,script,loaded);
            }
            document.getBody().appendChild(script);
        }

    }

    public void addCallback(Loaded loaded) {
        if (!isAllLoaded()) {
            _callbackList.add(loaded);
        }
        else {
            loaded.allLoaded();
        }
    }

//    public native static  void addIEListener(ScriptElement script, Loaded loaded) /*-{
//        script.onreadystatechange= function() {
//            GwtUtil.showDebugMsg("ie to not yet complete");
//            if (this.readyState=='complete') {
//                GwtUtil.showDebugMsg("ie to complete");
//                loaded.@edu.caltech.ipac.firefly.ui.JSLoad.Loaded::allLoaded()();
//            }
//        }
//
//    }-*/;

    public native void addIEListener(JSLoad jsload, ScriptElement script, Loaded loaded) /*-{
        var listener= @edu.caltech.ipac.firefly.ui.JSLoad::doneListenerStatic(Ledu/caltech/ipac/firefly/ui/JSLoad;Lcom/google/gwt/user/client/Event;);
        script.attachEvent(
                "onload",
                $entry(
                        function(event) {
                            listener(jsload, event);
                        })
                );
    }-*/;


    public native void addListener(JSLoad jsload, ScriptElement script, Loaded loaded) /*-{
        var listener= @edu.caltech.ipac.firefly.ui.JSLoad::doneListenerStatic(Ledu/caltech/ipac/firefly/ui/JSLoad;Lcom/google/gwt/user/client/Event;);
        script.addEventListener(
                "load",
                $entry(
                function(event) { listener(jsload, event); }),
                 false);
    }-*/;

    public void doneListener(Event ev) {
        JavaScriptObject source= ev.getEventTarget();
        if (_map.containsKey(source) && ScriptElement.is(source)) {
            _map.put(source,true);
        }
        else {
            // some error here
        }
        if (isAllLoaded()) allCompleteNotify();
    }

    public boolean isAllLoaded() {
        if (!_allLoaded) {
            _allLoaded= true;
            for(Boolean done : _map.values()) {
                _allLoaded= done;
                if (!_allLoaded) break;
            }
        }
        return _allLoaded;
    }

    private void allCompleteNotify()  {
        if (_map.size()>0) {
            for(Loaded cb : _callbackList) cb.allLoaded();
        }

    }

    public static void doneListenerStatic(JSLoad jsload, Event ev) {
        jsload.doneListener(ev);
    }



    public interface Loaded {
        void allLoaded();
    }
}

