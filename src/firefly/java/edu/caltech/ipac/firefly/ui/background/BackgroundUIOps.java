/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.background;
/**
 * User: roby
 * Date: 6/16/11
 * Time: 10:17 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.ui.GwtUtil;

/**
* @author Trey Roby
*/
public class BackgroundUIOps {
    private BackgroundGroupsDialog dialog;
    private final Widget p;
    private final MonitorGroup group;

    private static BackgroundUIOps instance= null;

    BackgroundUIOps(Widget p) {
        this.p= p;
        this.group= new MonitorGroup();
    }


    void update(MonitorItem item) {
        getDialog().update(item);
        getDialog().adjustHidingOnFinishedDownloads();
    }
    void update() { getDialog().update();}
    void insertPanel(MonitorItem item, DownloadGroupPanel panel) { getDialog().insertPanel(item,panel); }
    void remove(String id) { getDialog().remove(id); }
    void setVisible(boolean v) { getDialog().setVisible(v);}

    MonitorGroup getGroup() { return group; }

    private BackgroundGroupsDialog getDialog() {
        if (dialog ==null) dialog = new BackgroundGroupsDialog(p,group);
        return dialog;
    }



    public static void getOps(final Widget p,
                              final BackgroundGroupsDialog.OpsHandler opsHandler) {
        GWT.runAsync(new GwtUtil.DefAsync() {
            public void onSuccess() {
                if (instance==null) instance= new BackgroundUIOps(p);
                opsHandler.dialogOps(instance);
            }
        });
    }


}

