package edu.caltech.ipac.fuse.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */

/**
 * @author Trey Roby
 */
public class FuseSearchResultsTask extends ServerTask<RawDataSet> {

    private final MonitorItem monitorItem;
    private final AsyncCallback<RawDataSet> cb;


    public static void getResults(MonitorItem monitorItem, AsyncCallback<RawDataSet> cb) {
        if (!monitorItem.isSuccess() || monitorItem.getUIHint()!=BackgroundUIHint.RAW_DATA ||
                monitorItem.getStatus()==null || monitorItem.getStatus().getServerRequest()==null) {
            cb.onFailure(new IllegalArgumentException("monitor item in incorrect state"));
            return;
        }
        FuseSearchResultsTask task= new FuseSearchResultsTask(monitorItem,cb);
        task.start();
    }


    private FuseSearchResultsTask(MonitorItem monitorItem, AsyncCallback<RawDataSet> cb) {
        super();
        this.monitorItem= monitorItem;
        this.cb= cb;
    }

    @Override
    public void onSuccess(RawDataSet rawDataSet) {
        cb.onSuccess(rawDataSet);
    }



    @Override
    protected void onFailure(Throwable caught) {
        super.onFailure(caught);
        cb.onFailure(caught);
    }

    @Override
    public void doTask(AsyncCallback<RawDataSet> passAlong) {
        SearchServicesAsync  serv= SearchServices.App.getInstance();
        serv.getRawDataSet(monitorItem.getStatus().getServerRequest(), passAlong);
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

