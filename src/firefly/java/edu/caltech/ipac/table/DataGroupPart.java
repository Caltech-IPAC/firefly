/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.HasAccessInfos;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: May 14, 2009
 *
 * @author loi
 * @version $Id: DataGroupPart.java,v 1.7 2011/06/14 18:15:51 loi Exp $
 */
public class DataGroupPart implements HasAccessInfos {

    public static final String LOADING_STATUS = "Loading-Status";

    public static enum State {COMPLETED, INPROGRESS, FAILED;
                            public String toString() {
                                return StringUtils.pad(20, name());
                            }
    }

    private TableDef tableDef;
    private DataGroup data;
    private int startRow;
    private int rowCount;
    private String hasAccessCName = null;
    private String errorMsg;

    public DataGroupPart() {
    }

    public DataGroupPart(TableDef tableDef, DataGroup data, int startRow, int rowCount) {
        this.tableDef = tableDef;
        this.data = data;
        this.startRow = startRow;
        setRowCount(rowCount);
    }

    public String getHasAccessCName() {
        return hasAccessCName;
    }

    public void setHasAccessCName(String hasAccessCName) {
        this.hasAccessCName = hasAccessCName;
    }

    public int getRowCount() {
        return rowCount;
    }

    public TableDef getTableDef() {
        return tableDef;
    }

    public void setTableDef(TableDef tableDef) {
        this.tableDef = tableDef;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount < 0 ? 0 : rowCount;
    }

    public DataGroup getData() {
        return data;
    }

    public void setData(DataGroup data) {
        this.data = data;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public String getErrorMsg() { return errorMsg; }

    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

//====================================================================
//  implements HasAccessInfos
//====================================================================
    public int getSize() {
        return data == null ? 0 : data.size();
    }

    public boolean hasAccess(int index) {
        if (index < 0 ||getHasAccessCName() == null ||
            data == null || index >= data.size()) {
            return false;
        }

        return Boolean.parseBoolean(data.get(index).getDataElement(getHasAccessCName()).toString());
    }

//====================================================================
//
//====================================================================

}
