package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: Oct 30, 2009
 * Time: 12:29:21 PM
 */


/**
 * @author Trey Roby
 */
public class MasterCatalogRequest extends TableServerRequest {



//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MasterCatalogRequest() {
        this.setRequestId("irsaCatalogMasterTable");
        //this.setRequestId(SearchType.IRSA_CATALOG_MASTER_TABLE.getRequestId());
        this.setPageSize(5000);
    }
}

