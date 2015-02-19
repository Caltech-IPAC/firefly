/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table.renderer;

import com.google.gwt.gen2.table.client.ColumnDefinition;
import com.google.gwt.gen2.table.client.TableDefinition;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import edu.caltech.ipac.firefly.data.table.TableData;

/**
 * Date: Feb 27, 2009
 *
 * @author loi
 * @version $Id: NumericRenderer.java,v 1.1 2009/03/02 18:22:16 loi Exp $
 */
public class NumericRenderer extends AlignRenderer {
    NumberFormat formatter;

    public NumericRenderer() {
        this(HasHorizontalAlignment.ALIGN_RIGHT, "#,##0.0#");
    }

    public NumericRenderer(String formatString) {
        this(HasHorizontalAlignment.ALIGN_RIGHT, formatString);
    }

    public NumericRenderer(HasHorizontalAlignment.HorizontalAlignmentConstant align, String formatString) {
        super(align);
        formatter = NumberFormat.getFormat(formatString);
    }

    @Override
    public void renderRowValue(TableData.Row rowValue, ColumnDefinition<TableData.Row, String> columnDef,
                               TableDefinition.AbstractCellView<TableData.Row> view) {
        super.renderRowValue(rowValue, columnDef, view);

        try {
            double v = Double.parseDouble(columnDef.getCellValue(rowValue));
            view.setHTML(formatter.format(v));
        } catch(Exception ex) {
            view.setHTML(columnDef.getCellValue(rowValue));
        }
    }
}
