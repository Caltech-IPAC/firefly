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
