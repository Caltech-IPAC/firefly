/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

