package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.DefaultTableDefinition;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;

/**
 * Date: 1/28/13
 *
 * @author loi
 * @version $Id: $
 */
public class DatasetTableDef extends DefaultTableDefinition<TableData.Row> {
    boolean showUnits;
    TableDataView tableDataView;

    public DatasetTableDef(TableDataView def) {
        tableDataView = def;
        if (def.getMeta() != null) {
            showUnits = Boolean.parseBoolean(def.getMeta().getAttribute(TableMeta.SHOW_UNITS));
        }
        for(TableDataView.Column c : def.getColumns()) {
            if (!c.isHidden()) {
                ColDef cd = new ColDef(c);
                addColumnDefinition(cd);
                setColumnVisible(cd, c.isVisible());
            }
        }
    }

    public TableDataView getTableDataView() {
        return tableDataView;
    }

    public boolean isShowUnits() {
        return showUnits;
    }

}
