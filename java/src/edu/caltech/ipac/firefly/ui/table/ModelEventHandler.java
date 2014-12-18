package edu.caltech.ipac.firefly.ui.table;

import edu.caltech.ipac.firefly.data.table.TableDataView;

/**
* Date: 6/4/13
*
* @author loi
* @version $Id: $
*/
public interface ModelEventHandler {
    /**
     * is called when there is a problem loading the data
     * @param caught
     */
    public void onFailure(Throwable caught);

    /**
     * is called when a data load complete
     * @param result
     */
    public void onLoad(TableDataView result);

    /**
     * is called when there's an status update on the data loading.
     * this may get called multiple times when the data is loaded in the background
     * @param result
     */
    public void onStatusUpdated(TableDataView result);

    /**
     * is called when the data in this model has changed
     * @param model
     */
    public void onDataStale(DataSetTableModel model);
}
