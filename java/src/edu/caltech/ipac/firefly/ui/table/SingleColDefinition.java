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
public class SingleColDefinition extends DatasetTableDef {
    public static final String SELECTED = "SELECTED";
    static final CellRenderer<TableData.Row, String> alignRenderer = new AlignRenderer(HasHorizontalAlignment.ALIGN_CENTER);

    public SingleColDefinition(SingleColDef def) {
        super(def.getTableDef());
        addColumnDefinition(def);
        setColumnVisible(def, true);
    }

    public SingleColDefinition(String title, TableDataView def) {
        super(def);
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
            TableDataView.Column col = tableDef.findColumn(name);
            String cname = col == null ? "" : col.getName();
            for (TableDataView.Column cc : tableDef.getColumns()) {
                if (cc.getName() != null &&
                        !cc.getName().equalsIgnoreCase(cname)) {
                    tableDef.removeColumn(cc);
                }
            }
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
