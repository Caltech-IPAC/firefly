/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 2/2/15
 * Time: 3:43 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.rpc.SearchServices;

/**
 * @author Trey Roby
 */
public class ActionReporter {

    private MonitorItem monItem= null;

    public void setMonitorItem(MonitorItem monItem) {
        this.monItem= monItem;
    }

    public boolean isReporting() {
        return monItem!=null;
    }

    public void report(String desc, String data) {
        SearchServices.App.getInstance().reportUserAction(monItem.getID(),desc, data, new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) { }
            @Override
            public void onSuccess(Boolean result) {  }
        });
    }



}
