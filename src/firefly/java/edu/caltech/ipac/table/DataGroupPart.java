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

    public enum State {COMPLETED, INPROGRESS, FAILED;
                            public String toString() {
                                return StringUtils.pad(20, name());
                            }
    }

    private DataGroup data;
    private int startRow;
    private int rowCount;
    private String hasAccessCName = null;
    private Status status;

    public DataGroupPart() {
    }

    public DataGroupPart(DataGroup data, int startRow, int rowCount) {
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

    public String getErrorMsg() { return status != null && status.isError() ? status.message : null; }

    public void setErrorMsg(String errorMsg) { setStatus(new Status(500, errorMsg)); }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

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
    public record Status (int code, String message) {
        public boolean isError() { return code < 200 || code >= 400; }
    }
}
