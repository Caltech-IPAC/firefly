package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.SelectableTablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
/**
 * User: roby
 * Date: Apr 19, 2010
 * Time: 4:26:06 PM
 */


/**
 * @author Trey Roby
 */
public class SelectableTablePanelCreator extends TablePanelCreator {

    protected TablePanel makeTable(String name, Loader<TableDataView> loader) {
        return new SelectableTablePanel(name, loader);
    }
}

