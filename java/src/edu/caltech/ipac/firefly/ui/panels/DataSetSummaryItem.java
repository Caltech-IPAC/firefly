package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Nov 9, 2010
*
* @author loi
* @version $Id: DataSetSummaryItem.java,v 1.3 2010/11/24 01:55:57 loi Exp $
*/
public class DataSetSummaryItem extends SearchSummaryItem {
    private TableData.Row row;
    private Map<String, TableDataView.Column> cmap;

    public DataSetSummaryItem(TableData.Row row, List<TableDataView.Column> columns) {
        this.row = row;
        this.setLoaded(true);
        cmap = new HashMap<String, TableDataView.Column>();
        for(TableDataView.Column c : columns) {
            cmap.put(c.getName(), c);
        }
    }

    public TableData.Row getRow() {
        return row;
    }

    @Override
    public void renderItem(FlexTable table, int row, String... ignoreCols) {
        super.renderItem(table, row, ignoreCols);
    }

    @Override
    public CellData getCellData(String colname) {
        TableDataView.Align align = cmap.get(colname).getAlign();
        HasHorizontalAlignment.HorizontalAlignmentConstant halign = align.equals(TableDataView.Align.LEFT) ?
                        HasHorizontalAlignment.ALIGN_LEFT : align.equals(TableDataView.Align.RIGHT) ?
                        HasHorizontalAlignment.ALIGN_RIGHT : HasHorizontalAlignment.ALIGN_CENTER;
        return new CellData(String.valueOf(row.getValue(colname)), halign);
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

