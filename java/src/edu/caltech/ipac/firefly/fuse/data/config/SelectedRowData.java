package edu.caltech.ipac.firefly.fuse.data.config;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;

/**
 * Date: 7/24/14
 *
 * @author loi
 * @version $Id: $
 */
public class SelectedRowData {
    private TableData.Row<String> selectedRow;
    private TableMeta tableMeta;
    private ServerRequest request;

    public SelectedRowData(TableData.Row<String> selectedRow, TableMeta tableMeta, ServerRequest request) {
        this.selectedRow = selectedRow;
        this.tableMeta = tableMeta;
        this.request= request;
    }

    public TableData.Row<String> getSelectedRow() {
        return selectedRow;
    }

    public void setSelectedRow(TableData.Row<String> selectedRow) {
        this.selectedRow = selectedRow;
    }

    public TableMeta getTableMeta() {
        return tableMeta;
    }

    public void setTableMeta(TableMeta tableMeta) {
        this.tableMeta = tableMeta;
    }

    public ServerRequest getRequest() {
        return request;
    }
}
