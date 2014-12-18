package edu.caltech.ipac.firefly.ui.catalog;

import edu.caltech.ipac.firefly.data.table.DataSet;
/**
 * User: roby
 * Date: May 13, 2010
 * Time: 12:35:30 PM
 */


    public interface MasterCatResponse {
        void masterCatalogFailed();
        void newMasterCatalog(DataSet ds);
    }

