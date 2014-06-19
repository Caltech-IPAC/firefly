package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitor;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.Notifications;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * User: roby
 * Date: Oct 30, 2008
 * Time: 4:51:52 PM
 */


/**
 * @author Trey Roby
 */

public class PackageTask extends ServerTask<BackgroundStatus> {


    private final DownloadRequest _dataRequest;

    private final int _animationX;
    private final int _animationY;


//======================================================================
//----------------------- Public Static Methods ------------------------
//======================================================================

    public static void preparePackage( Widget maskW,
                                       int animationX,
                                       int animationY,
                                       DownloadRequest dataRequest) {
        PackageTask task= new PackageTask(maskW,animationX,animationY,dataRequest);
        Notifications.requestPermission();
        task.start();
    }




//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    private PackageTask(Widget maskW,
                        int animationX,
                        int animationY,
                        DownloadRequest dataRequest) {
        super(maskW, "Preparing Data...", true);
        _animationX= animationX;
        _animationY= animationY;
        _dataRequest = dataRequest;
    }

//=======================================================================
//-------------- Method from ServerTask class ---------------------------
//=======================================================================

    @Override
    public void onSuccess(BackgroundStatus bgStat) {
        BackgroundMonitor monitor= Application.getInstance().getBackgroundMonitor();
        MonitorItem item= new MonitorItem(_dataRequest.getTitle(), BackgroundUIHint.ZIP);
        item.setImmediately(true);
        item.setStatus(bgStat);
        monitor.addItem(item);
        if (bgStat.getState()!= BackgroundState.SUCCESS) {
            Application.getInstance().getBackgroundManager().animateToManager(_animationX,_animationY,1000);
            if (bgStat.hasAttribute(JobAttributes.LongQueue)) {
                PopupUtil.showInfo(getWidget(),
                                   "Warning: Long Queue",
                                   "The server queue is very long, packaging might take an hour or more before it can start.");

            }
        }
    }

    @Override
    public void doTask(AsyncCallback<BackgroundStatus> passAlong) {
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(_dataRequest, Name.ON_PACKAGE_SUBMIT));
        SearchServices.App.getInstance().packageRequest(_dataRequest, passAlong);
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
