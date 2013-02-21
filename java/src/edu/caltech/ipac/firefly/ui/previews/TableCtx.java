package edu.caltech.ipac.firefly.ui.previews;
/**
 * User: roby
 * Date: 7/3/12
 * Time: 3:37 PM
 */


import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.table.TablePanel;

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
        if (table.getTable()!=null && table.getTable().getHighlightRows()!=null) {
            TableData.Row<String>[] hrows = table.getTable().getHighlightRows();
            row = (hrows.length>0) ? hrows[0] : null;
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
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
