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
