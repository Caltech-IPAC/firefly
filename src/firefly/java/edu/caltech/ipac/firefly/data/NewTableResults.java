/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.ui.table.builder.TableConfig;

/**
 * User: roby
 * Date: Nov 6, 2009
 * Time: 3:05:31 PM
 */


/**
 * @author Trey Roby
*/
public class NewTableResults {
    public static final String BRING_TO_FRONT = "BringToFront";

    private TableConfig config;
    private String _tableType;
    private boolean bringToFront = true;

    public NewTableResults(TableConfig config, String tableType) {
        this.config = config;
        _tableType = tableType;
    }

    public NewTableResults(TableServerRequest req, String basicTable, String title) {
        this(new BaseTableConfig(req, title, title), basicTable);
    }

    public String getTableType() { return _tableType;}

    public TableConfig getConfig() {
        return config;
    }

    public boolean isBringToFront() {
        return bringToFront;
    }

    public void setBringToFront(boolean bringToFront) {
        this.bringToFront = bringToFront;
    }
}

