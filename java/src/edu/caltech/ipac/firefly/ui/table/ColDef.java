/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.AbstractColumnDefinition;
import com.google.gwt.gen2.table.client.DefaultCellRenderer;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;

/**
 * Date: Jul 23, 2009
*
* @author loi
* @version $Id: ColDef.java,v 1.6 2010/12/13 23:42:01 loi Exp $
*/
public class ColDef extends AbstractColumnDefinition<TableData.Row, String> {
    private TableDataView.Column column;
    private boolean isImmutable = false;

    public ColDef() {

    }

    public ColDef(TableDataView.Column column) {
        this.column = column;
        setMinimumColumnWidth(25);
        setPreferredColumnWidth(column.getPrefWidth()*8 + 15);
        setColumnSortable(column.isSortable());
        setCellRenderer(new DefaultCellRenderer<TableData.Row, String>(true));
    }

    public boolean isImmutable() {
        return isImmutable;
    }

    public void setImmutable(boolean immutable) {
        isImmutable = immutable;
    }

    public TableDataView.Column getColumn() {
        return column;
    }

    public String getCellValue(TableData.Row rowValue) {
        return String.valueOf(rowValue.getValue(column.getName()));
    }

    public void setCellValue(TableData.Row rowValue, String cellValue) {
        rowValue.setValue(column.getName(), cellValue);
    }

    public String getName() {
        return column == null ? null : column.getName();
    }

    public String getTitle() {
        return column == null ? getName() : column.getTitle();
    }

    public String getShortDesc() {
        return column == null ? getTitle() : column.getShortDesc();
    }
}
