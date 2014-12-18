package edu.caltech.ipac.firefly.ui.table.renderer;

import com.google.gwt.gen2.table.client.CellRenderer;
import com.google.gwt.gen2.table.client.ColumnDefinition;
import com.google.gwt.gen2.table.client.TableDefinition;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import edu.caltech.ipac.firefly.data.table.TableData;

/**
 * Date: Feb 27, 2009
 *
 * @author loi
 * @version $Id: AlignRenderer.java,v 1.1 2009/03/02 18:22:16 loi Exp $
 */
public class AlignRenderer implements CellRenderer<TableData.Row, String> {
    HasHorizontalAlignment.HorizontalAlignmentConstant align;

    public AlignRenderer(HasHorizontalAlignment.HorizontalAlignmentConstant align) {
        this.align = align;
    }

    public void renderRowValue(TableData.Row rowValue, ColumnDefinition<TableData.Row, String> columnDef,
                               TableDefinition.AbstractCellView<TableData.Row> view) {
        view.setHorizontalAlignment(align);
        view.setHTML(columnDef.getCellValue(rowValue));
    }
}
