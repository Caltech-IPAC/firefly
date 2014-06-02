package edu.caltech.ipac.firefly.ui.background;
/**
 * User: roby
 * Date: 5/30/14
 * Time: 12:09 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;

/**
 * @author Trey Roby
 */
public class BackgroundTask {

    private static SearchServicesAsync serv= SearchServices.App.getInstance();

    public static void setEmail(List<String> idList, String email) {
        if (!StringUtils.isEmpty(email)) {
            serv.setEmail(idList, email, new AsyncCallback<Boolean>() {
                public void onFailure(Throwable caught) { /* ignore */ }
                public void onSuccess(Boolean result) { /* ignore */ }
            });
        }
    }

    public static void setEmail(String id, String email) {
        if (!StringUtils.isEmpty(email)) {
            serv.setEmail(id, email, new AsyncCallback<Boolean>() {
                public void onFailure(Throwable caught) { /* ignore */ }
                public void onSuccess(Boolean result) { /* ignore */ }
            });
        }
    }

    public static void resendEmail(List<String> idList, String email) {
        if (!StringUtils.isEmpty(email)) {
            serv.resendEmail(idList, email, new AsyncCallback<Boolean>() {
                public void onFailure(Throwable caught) { /* ignore */ }
                public void onSuccess(Boolean result) { /* ignore */ }
            });
        }
    }


    public static void cancel(String packageID) {
        if (!StringUtils.isEmpty(packageID)) {
            serv.cancel(packageID, new AsyncCallback<Boolean>() {
                public void onFailure(Throwable caught) { }
                public void onSuccess(Boolean ignore) { }
            });
        }
    }

    public static void cleanup(String packageID) {
        if (!StringUtils.isEmpty(packageID)) {
            serv.cleanup(packageID, new AsyncCallback<Boolean>() {
                public void onFailure(Throwable caught) { }
                public void onSuccess(Boolean ignore) { }
            });
        }
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
