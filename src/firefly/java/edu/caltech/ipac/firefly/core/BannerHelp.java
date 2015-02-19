/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;
/**
 * User: roby
 * Date: Dec 7, 2010
 * Time: 11:39:20 AM
 */


/**
 * @author Trey Roby
 */
public class BannerHelp {

    public static void setHelp(String name, String tip, int idx, String id, boolean addSeparator) {
       setHelpInternal(name,tip,idx,id);
       if (addSeparator) addSeparator(idx+1);
    }

    public static void removeHelp(String name, boolean removeSeparatorBelow) {
        removeHelpInternal(name);
    }


    public static void setUrlHelp(String name, String tip, int idx, String url, boolean addSeparator) {
        setHelpLink(name,tip,idx,url);
        if (addSeparator) addSeparator(idx+1);
    }

    private native static void setHelpInternal(String name, String tip, int idx, String id)  /*-{
        var data=  [ {
                    "name" : "Help",
                    "drop" :  [
                        {
                            "name" : name,
                            "href" : "javascript:top.window.ffProcessRequest('id="+ id + "&shortDesc=" + name + "')",
                            "index": idx,
                            "tip"  : tip
                        } ]
                   }];

        if ($wnd.irsaBannerSetData) {
            $wnd.irsaBannerSetData(data);
        }
    }-*/;


    private native static void removeHelpInternal(String name)  /*-{
        var data=  [ {
                         "name" : "Help",
                         "drop" :  [
                             {
                                 "name" : name
                             } ]
                     }];

        if ($wnd.irsaBannerSetData) {
            $wnd.irsaBannerSetData(data);
        }    }-*/;



    private native static void addSeparator(int idx)  /*-{
        var data=  [ {
                    "name" : "Help",
                    "drop" :  [
                        {
                            "separator" : "true",
                            "index": idx
                        } ]
                   }];
        if ($wnd.irsaBannerSetData) {
            $wnd.irsaBannerSetData(data);
        }    }-*/;



    private native static void setHelpLink(String name, String tip, int idx, String url)  /*-{
        var data=  [ {
                         "name" : "Help",
                         "drop" :  [
                             {
                                 "name" : name,
                                 "href" : url,
                                 "index": idx,
                                 "tip"  : tip
                             } ]
                     }];

        if ($wnd.irsaBannerSetData) {
            $wnd.irsaBannerSetData(data);
        }    }-*/;
}

