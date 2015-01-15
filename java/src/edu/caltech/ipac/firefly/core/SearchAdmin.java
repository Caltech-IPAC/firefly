/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;

/**
 * Date: 9/12/13
 *
 * @author loi
 * @version $Id: $
 */
public class SearchAdmin {

    private final SearchServicesAsync serv= SearchServices.App.getInstance();
    private static SearchAdmin instance= null;

    private SearchAdmin() {}

    public static SearchAdmin getInstance() {
        if (instance==null) instance= new SearchAdmin();
        return instance;
    }

    public void cancelSearch(MonitorItem item) {
        if (item!=null) item.cancel();
    }

    public MonitorItem submitSearch(TableServerRequest req) {
        return submitSearch(req,req.getParam(Request.SHORT_DESC));
    }

    public MonitorItem submitSearch(TableServerRequest req, String title) {
        final MonitorItem monItem= new MonitorItem(req, title, BackgroundUIHint.RAW_DATA_SET);

        serv.submitBackgroundSearch(req, null, 10, new AsyncCallback<BackgroundStatus>() {
            public void onFailure(Throwable caught) {
                monItem.setStatus(BackgroundStatus.createUnknownFailStat());
            }

            public void onSuccess(BackgroundStatus bgStat) {
                monItem.setStatus(bgStat);
                Application.getInstance().getBackgroundMonitor().addItem(monItem);
            }
        });

        return monItem;
    }


    public void retrieveSearchResults(final MonitorItem monitorItem, final AsyncCallback<RawDataSet> cb) {
        if (incompleteRawDataItem(monitorItem)) {
            cb.onFailure(new IllegalArgumentException("monitor item in incorrect state"));
            return;
        }

        serv.getRawDataSet(monitorItem.getStatus().getServerRequest(), new AsyncCallback<RawDataSet>() {
            public void onFailure(Throwable caught)      { cb.onFailure(caught); }
            public void onSuccess(RawDataSet rawDataSet) { cb.onSuccess(rawDataSet); }
        });
    }

    private boolean incompleteRawDataItem(MonitorItem m) {
        return !m.isSuccess() || m.getUIHint()!=BackgroundUIHint.RAW_DATA_SET ||
                m.getStatus()==null || m.getStatus().getServerRequest()==null;
    }
}
