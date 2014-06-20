package edu.caltech.ipac.fuse.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
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
        final MonitorItem monItem= new MonitorItem(req, title, BackgroundUIHint.SERVER_TASK);

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
        return !m.isSuccess() || m.getUIHint()!=BackgroundUIHint.RAW_DATA ||
                m.getStatus()==null || m.getStatus().getServerRequest()==null;
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
