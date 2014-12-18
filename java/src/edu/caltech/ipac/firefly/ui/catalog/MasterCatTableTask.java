package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.MasterCatalogRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */

/**
 * @author Trey Roby
 */
class MasterCatTableTask extends ServerTask<RawDataSet> {

    private final MasterCatResponse _response;
    static private String _projectId;

    public static MasterCatTableTask getMasterCatalog(Widget w, String projectId,
                                                      MasterCatResponse response) {
        _projectId = projectId;
        MasterCatTableTask task = new MasterCatTableTask(w, response);
        task.start();
        return task;
    }

    private MasterCatTableTask(Widget w, MasterCatResponse response) {
        super(w, "Retrieving Catalogs...", false);
        _response = response;
    }

    @Override
    public void onSuccess(RawDataSet rawDataSet) {
        DataSet ds = DataSetParser.parse(rawDataSet);
        _response.newMasterCatalog(ds);

    }

    @Override
    protected void onFailure(Throwable caught) {
        _response.masterCatalogFailed();
    }

    @Override
    public void doTask(AsyncCallback<RawDataSet> passAlong) {
        SearchServicesAsync serv = SearchServices.App.getInstance();
        MasterCatalogRequest req = new MasterCatalogRequest();

        // pass projectId, if applicable
        if (!StringUtils.isEmpty(_projectId)) {
            req.setParam(DynUtils.HYDRA_PROJECT_ID, _projectId);
        }

//        req.setSortInfo(new SortInfo());
        serv.getRawDataSet(req, passAlong);
    }
}

