package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Nov 9, 2010
*
* @author loi
* @version $Id: DataSetSummaryItem.java,v 1.3 2010/11/24 01:55:57 loi Exp $
*/
public class DataSetSummaryItem extends SearchSummaryItem {
    private TableData.Row row;
    private Map<String, TableDataView.Column> cmap;

    public DataSetSummaryItem(TableData.Row row, List<TableDataView.Column> columns) {
        this.row = row;
        this.setLoaded(true);
        cmap = new HashMap<String, TableDataView.Column>();
        for(TableDataView.Column c : columns) {
            cmap.put(c.getName(), c);
        }
    }

    public TableData.Row getRow() {
        return row;
    }

    @Override
    public void renderItem(FlexTable table, int row, String... ignoreCols) {
        super.renderItem(table, row, ignoreCols);
    }

    @Override
    public CellData getCellData(String colname) {
        TableDataView.Align align = cmap.get(colname).getAlign();
        HasHorizontalAlignment.HorizontalAlignmentConstant halign = align.equals(TableDataView.Align.LEFT) ?
                        HasHorizontalAlignment.ALIGN_LEFT : align.equals(TableDataView.Align.RIGHT) ?
                        HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_CENTER;
        return new CellData(String.valueOf(row.getValue(colname)), halign);
    }
}

