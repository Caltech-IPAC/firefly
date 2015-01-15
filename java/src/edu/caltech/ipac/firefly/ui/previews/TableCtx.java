/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;
/**
 * User: roby
 * Date: 7/3/12
 * Time: 3:37 PM
 */

import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Trey Roby
*/
public class TableCtx {
    private TableData.Row<String> row= null;
    private Map<String,String> meta;
    private List<String> columns;
    private final boolean hasData;
    private final String id;

    public TableCtx(TablePanel table) {
        id= table.getName();
        if (table.getTable()!=null && table.getTable().getHighlightedRow()!=null) {
            row = table.getTable().getHighlightedRow();
        }
        if (table.getDataset() != null) {
            meta= table.getDataset().getMeta().getAttributes();
            columns= table.getDataset().getModel().getColumnNames();
        }
        hasData= table.getDataset()!=null;

    }

    public TableCtx(String id,
                    TableData.Row<String> row,
                    Map<String, String> meta,
                    List<String> columns) {
        this.id= id;
        this.row = row;
        this.meta= meta;
        this.columns= columns;
        this.hasData= true;
    }

    public TableCtx(String id) { this(id,null,new HashMap<String, String>(10),new ArrayList<String>(10)); }


    public void setRow(TableData.Row<String> row) { this.row= row; }

    public TableData.Row<String> getRow() { return row; }

    public Map<String, String> getMeta() { return meta; }
    public void setMeta(Map<String, String> meta)  { this.meta= meta; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns)  { this.columns= columns; }

    public boolean hasData() { return this.hasData; }

    public String getId() { return id; }

    public WorldPt getOverlayPosition() {
        WorldPt wp= null;
        if (meta.containsKey(WebPlotRequest.OVERLAY_POSITION)) {
            String wpStr= meta.get(WebPlotRequest.OVERLAY_POSITION);
            wp= WorldPt.parse(wpStr);
        }
        return wp;
    }
}

