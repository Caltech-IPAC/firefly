/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.ActivationFactory;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitor;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
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
    private boolean doAnimation = true;
    private boolean showImmediately = false;

    public static IrsaCatalogTask getCatalog(Widget w,
                                             CatalogRequest req,
                                             CatalogSearchResponse response,
                                             String title,
                                             boolean showImmediately) {
        IrsaCatalogTask task = getCatalog(w, req, response, -1, -1, title);
        task.showImmediately = showImmediately;
        return task;
    }


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
        doAnimation = _animationX >= 0 && _animationY >= 0;
    }

    @Override
    public void onSuccess(BackgroundStatus bgStat) {
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.CATALOG_SEARCH_IN_PROCESS));
        MonitorItem monItem = new MonitorItem(_req, _title, BackgroundUIHint.CATALOG, false);
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
        final MaskPane maskPane = new MaskPane(getMaskWidget(), working);
        maskPane.show();
        Timer t= new Timer() {
            @Override
            public void run() {
                BackgroundMonitor monitor= Application.getInstance().getBackgroundMonitor();
                if (doAnimation) {
                    Application.getInstance().getBackgroundManager().animateToManager(_animationX, _animationY, 1300);
                }
                monItem.setWatchable(true);
                monItem.setImmediately(showImmediately);
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
