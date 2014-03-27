package edu.caltech.ipac.firefly.ui.table.renderer;

import com.google.gwt.gen2.table.client.CellRenderer;
import com.google.gwt.gen2.table.client.ColumnDefinition;
import com.google.gwt.gen2.table.client.TableDefinition;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.input.InputField;

import java.util.HashMap;
import java.util.Hashtable;

/**
 * @author tatianag
 *         $Id: $
 */
public class InputFieldRenderer implements CellRenderer<TableData.Row, String> {

    private HashMap<String, InputField> inputFieldMap;

    /*
    *
    */
    public InputFieldRenderer(HashMap<String, InputField> inputFieldMap) {

        this.inputFieldMap = inputFieldMap;
    }

    public void renderRowValue(TableData.Row rowValue, ColumnDefinition<TableData.Row, String> columnDef,
                               TableDefinition.AbstractCellView<TableData.Row> view) {
        view.setWidget(inputFieldMap.get(rowValue.getValue("name")));
    }

}

