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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
