package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.ActivationFactory;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitor;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIType;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.DefaultWorkingWidget;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.Notifications;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */

/**
 * @author Trey Roby
 */
public class IrsaCatalogTask extends ServerTask<BackgroundStatus> {

    private final CatalogSearchResponse _response;
    private final CatalogRequest      _req;
    private final String              _title;
    private final int _animationX;
    private final int _animationY;


    public static IrsaCatalogTask getCatalog(Widget w,
                                             CatalogRequest req,
                                             CatalogSearchResponse response,
                                             int animationX,
                                             int animationY,
                                             String title) {
        LayoutManager layout= Application.getInstance().getLayoutManager();
        Widget maskW= w;
        if (layout!=null) {
            Region freg= layout.getRegion(LayoutManager.DROPDOWN_REGION);
            if (freg!=null) {
                maskW= freg.getDisplay()!=null ? freg.getDisplay() : w;
            }
        }
        Notifications.requestPermission();
        IrsaCatalogTask task= new IrsaCatalogTask(maskW,req,response,animationX, animationY, title);
        task.start();
        return task;
    }

    private IrsaCatalogTask(Widget w,
                            CatalogRequest req,
                            CatalogSearchResponse response,
                            int animationX,
                            int animationY,
                            String title) {
         super(w, "Retrieving Catalog...", true);
        _response= response;
        _req= req;
        _title= title;
        _animationX= animationX;
        _animationY= animationY;
    }

    @Override
    public void onSuccess(BackgroundStatus bgStat) {
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.CATALOG_SEARCH_IN_PROCESS));
        MonitorItem monItem= new MonitorItem(_title, BackgroundUIType.CATALOG, false);
        monItem.setStatus(bgStat);
        monItem.setActivateOnCompletion(true);
        if (bgStat.isSuccess()) {
            ActivationFactory.getInstance().activate(monItem);
            _response.status(CatalogSearchResponse.RequestStatus.SUCCESS);
        }
        else {
            handleBackgrounding(monItem);
        }
    }


    private void handleBackgrounding(final MonitorItem monItem) {
        DefaultWorkingWidget working= new DefaultWorkingWidget((ClickHandler)null);
        working.setText("Backgrounding...");
        final MaskPane maskPane = new MaskPane(getWidget(), working);
        maskPane.show();
        Timer t= new Timer() {
            @Override
            public void run() {
                BackgroundMonitor monitor= Application.getInstance().getBackgroundMonitor();
                Application.getInstance().getBackgroundManager().animateToManager(_animationX, _animationY, 1300);
                monItem.setWatchable(true);
                monitor.addItem(monItem);
                _response.status(CatalogSearchResponse.RequestStatus.BACKGROUNDING);
                maskPane.hide();
            }
        };
        t.schedule(1000);
    }

    @Override
    protected void onFailure(Throwable caught) {
        super.onFailure(caught);
        _response.status(CatalogSearchResponse.RequestStatus.FAILED);
    }

    @Override
    public void doTask(AsyncCallback<BackgroundStatus> passAlong) {
        SearchServicesAsync  serv= SearchServices.App.getInstance();
        serv.submitBackgroundSearch(_req, null, 5000, passAlong);
    }


    public enum CatalogType implements DataEntry { IRSA, OTHER }


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

