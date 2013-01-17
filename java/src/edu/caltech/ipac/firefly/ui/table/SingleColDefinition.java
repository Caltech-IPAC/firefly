package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.CellRenderer;
import com.google.gwt.gen2.table.client.DefaultTableDefinition;
import com.google.gwt.gen2.table.client.DefaultCellRenderer;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.renderer.AlignRenderer;

/**
 * Date: Jul 23, 2009
 *
 * @author loi
 * @version $Id: SingleColDefinition.java,v 1.4 2010/01/25 18:23:20 loi Exp $
 */
public class SingleColDefinition extends DefaultTableDefinition<TableData.Row> {
    public static final String SELECTED = "SELECTED";
    static final CellRenderer<TableData.Row, String> alignRenderer = new AlignRenderer(HasHorizontalAlignment.ALIGN_CENTER);

    public SingleColDefinition(ColDef def) {
        addColumnDefinition(def);
        setColumnVisible(def, true);
    }

    public SingleColDefinition(String title, TableDataView def) {
        SingleColDef cd = new SingleColDef(title, def);
        addColumnDefinition(cd);
        setColumnVisible(cd, true);
    }

//====================================================================
//app
//====================================================================
    public static class SingleColDef extends ColDef {
        private TableDataView tableDef;
        private String name;

        public SingleColDef(String name, TableDataView tableDef) {
            this.name = name;
            this.tableDef = tableDef;
            setMinimumColumnWidth(100);
            setPreferredColumnWidth(200);
            setColumnSortable(false);
            setCellRenderer(new DefaultCellRenderer<TableData.Row, String>(true));
        }

        public String getCellValue(TableData.Row rowValue) {
            StringBuffer rval = new StringBuffer();

            for(TableDataView.Column c : tableDef.getColumns()) {
                if (rval.length() > 0) {
                    rval.append("<br>");
                }
                rval.append("<font color='brown'>").append(c.getName()).append(": </font>").append(rowValue.getValue(c.getName()));
            }
            return rval.toString();
        }

        public void setCellValue(TableData.Row rowValue, String cellValue) {
            throw new IllegalArgumentException("This table is immutable");
        }

        protected TableDataView getTableDef() {
            return tableDef;
        }

        protected String makeEntry(String key, Object val) {
            return "<font color='brown'>" + key + ": </font>" + String.valueOf(val);
        }

        @Override
        public String getTitle() {
            return name;
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
