/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import edu.caltech.ipac.firefly.data.table.TableDataView;

/**
 * @author tatianag
 *         $Id: $
 */
public class SelectableTableWithConstraintsPanel extends SelectableTablePanel {

    public SelectableTableWithConstraintsPanel(Loader<TableDataView> loader) {
        super(loader);
    }


    @Override
    protected BasicPagingTable newTable(DataSetTableModel model, TableDataView dataset) {
        table = new SelectionTableWithConstraints(getName(), model, dataset);
        return table;
    }

}
