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
