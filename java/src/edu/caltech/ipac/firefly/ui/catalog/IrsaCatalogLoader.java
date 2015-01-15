/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.table.PagingDataSetLoader;
/**
 * User: roby
 * Date: Nov 6, 2009
 * Time: 9:14:52 AM
 */


/**
 * @author Trey Roby
 */
@Deprecated
public class IrsaCatalogLoader extends PagingDataSetLoader {

    private IrsaCatalogTask.CatalogType _type;

    public IrsaCatalogLoader(TableServerRequest req, IrsaCatalogTask.CatalogType type) {
        super(req);
        _type= type;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    @Override
    public void onLoad(TableDataView data) {
        super.onLoad(data);
        data.getMeta().setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, _type.toString() );
    }

    @Override
    protected void doLoadData(TableServerRequest req, AsyncCallback<RawDataSet> callback) {
        SearchServicesAsync serv= SearchServices.App.getInstance();
        serv.getRawDataSet(req, callback);
    }



}

