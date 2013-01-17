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
