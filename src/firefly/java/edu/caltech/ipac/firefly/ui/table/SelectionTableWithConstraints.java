/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.CellRenderer;
import com.google.gwt.http.client.URL;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.TextBoxInputField;
import edu.caltech.ipac.firefly.ui.table.renderer.InputFieldRenderer;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.StringFieldDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author tatianag
 */
public class SelectionTableWithConstraints extends SelectionTable {

    public final static String CONSTRAINTS_SEPARATOR = CatalogRequest.CONSTRAINTS_SEPARATOR;

    SelectionTableWithConstraintsDef tableDef;

    /**
     * Construct a new {@link SelectionTable}.
     * @param name          table name
     * @param tableModel    the underlying table model
     * @param tableDataView the column definitions
     */
    public SelectionTableWithConstraints(String name, DataSetTableModel tableModel,
                              TableDataView tableDataView) {
        super(name, tableModel, new SelectionTableWithConstraintsDef(tableDataView), tableDataView.getTotalRows());
        tableDef = (SelectionTableWithConstraintsDef) this.getTableDefinition();
    }

    public void setConstraints(String constraints) {
        tableDef.setConstraints(constraints);
    }

    public String getConstraints() {
        return CollectionUtil.toString(tableDef.getConstraints(), CONSTRAINTS_SEPARATOR);
    }

    public static class SelectionTableWithConstraintsDef extends SelectionTableDef {

        private HashMap<String, InputField> inputFieldMap;

        public SelectionTableWithConstraintsDef(TableDataView def) {
            super(def);
            TableData<TableData.Row> model = def.getModel();
            inputFieldMap = new HashMap<String, InputField>(def.getTotalRows());
            for (TableData.Row row : model.getRows()) {
                String name = row.getValue("name").toString();
                inputFieldMap.put(name, new TextBoxInputField(new StringFieldDef(name, name, "", "enter semicolon separated constraints\nusing LIKE,=,>,>=,<,<=", Integer.MAX_VALUE, 15, "", true, null)));
            }
            final CellRenderer<TableData.Row, String> renderer = new InputFieldRenderer(inputFieldMap);
            TableDataView.Column col = new BaseTableColumn("constraints", TableDataView.Align.CENTER, 15, false);
            col.setShortDesc("Semicolon separated constraints");
            addColumnDefinition(2, new ColDef(col) {
                //{
                    //setMinimumColumnWidth(90);
                    //setPreferredColumnWidth(90);
                    //setMaximumColumnWidth(90);
                    //setColumnSortable(false);
                    //setImmutable(true);
                //}

                @Override
                public CellRenderer<TableData.Row, String> getCellRenderer() {
                    return renderer;
                }

                public String getCellValue(TableData.Row rowValue) {
                    return inputFieldMap.get(rowValue.getValue("name").toString()).getValue();
                }

                public void setCellValue(TableData.Row rowValue, String cellValue) {
                    inputFieldMap.get(rowValue.getValue("name").toString()).setValue(cellValue);
                }

                public String getTitle() {
                    return "constraints";
                }
            });
        }

        private void clearConstraints() {
            for (String key : inputFieldMap.keySet()) {
                inputFieldMap.get(key).setValue("");
            }
        }

        public void setConstraints(String constraintsStr) {
            List<String> constraints = Arrays.asList(constraintsStr.split(CONSTRAINTS_SEPARATOR));
            String currVal,val;
            clearConstraints();
            for (String constr : constraints) {
                for (String key : inputFieldMap.keySet()) {
                    if (constr.startsWith(key+" ")) {
                        val = constr.replace(key+" ","");
                        currVal = inputFieldMap.get(key).getValue();
                        if (!StringUtils.isEmpty(currVal)) {
                            val += currVal+";"+val;
                        }
                        inputFieldMap.get(key).setValue(val);

                    }
                }
            }
        }


        public List<String> getConstraints() {
            List<String> constraints = new ArrayList<String>();
            String val;
            for (String key : inputFieldMap.keySet()) {
                val = inputFieldMap.get(key).getValue();
                if (!StringUtils.isEmpty(val)) {
                    String [] parts = val.split(";");
                    for (String p : parts) {
                        p = p.trim();
                        if (p.startsWith(">")||p.startsWith("<")||p.startsWith("=")||p.startsWith("LIKE")) {
                            constraints.add(URL.encode(key+" "+p));
                        } else {
                            constraints.add(URL.encode(key+" LIKE \'"+p+"\'"));
                        }
                    }
                }
            }
            return constraints;
        }
    }

}
