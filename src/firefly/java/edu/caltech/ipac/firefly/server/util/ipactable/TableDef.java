package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 6/26/15
 *
 * @author loi
 * @version $Id: $
 */
public class TableDef extends TableMeta {
    private List<DataType> cols = new ArrayList<>();
    private List<Integer> colOffsets = new ArrayList<>();
    private int lineWidth;
    private int rowCount;
    private int rowStartOffset;
    private int lineSepLength;
    private SelectionInfo selectInfo;
    private boolean saveFormattedData;
    private transient Pair<Integer, String> extras;     // used by IpacTableUtil to store extras data while parsing an ipac table via input stream

    public Pair<Integer, String> getExtras() {
        return extras;
    }

    public int getColOffset(int idx) {
        return colOffsets.get(idx);
    }

    public void setColOffsets(int idx, int offset) {
        colOffsets.add(idx, offset);
    }

    public boolean isSaveFormattedData() {
        return saveFormattedData;
    }

    public void setSaveFormattedData(boolean saveFormattedData) {
        this.saveFormattedData = saveFormattedData;
    }

    public void setExtras(Integer numHeaderLines, String unreadLine) {
        this.extras = new Pair<>(numHeaderLines, unreadLine);
    }

    public List<DataType> getCols() {
        return cols;
    }

    public DataType getColByName(String name) {
        name = name == null ? "" : name;
        for (DataType dt : cols) {
            if (dt.getKeyName().equals(name)) return dt;
        }
        return null;
    }

    public void setCols(List<DataType> cols) {
        this.cols = cols;
    }

    public SelectionInfo getSelectInfo() {
        return selectInfo;
    }

    public void setSelectInfo(SelectionInfo selectInfo) {
        this.selectInfo = selectInfo;
    }

    public void ensureStatus() {
        String status = getAttribute(DataGroupPart.LOADING_STATUS);
        if (StringUtils.isEmpty(status)) {
            setStatus(DataGroupPart.State.COMPLETED);
        }
    }

    public DataGroupPart.State getStatus() {
        ensureStatus();
        String status = getAttribute(DataGroupPart.LOADING_STATUS);
        return DataGroupPart.State.valueOf(String.valueOf(status));
    }

    public void setStatus(DataGroupPart.State status) {
        setAttribute(DataGroupPart.LOADING_STATUS, status.name());
    }

    public int getLineSepLength() {
        return lineSepLength;
    }

    public void setLineSepLength(int lineSepLength) {
        this.lineSepLength = lineSepLength;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount < 0 ? 0 : rowCount;
    }

    public int getRowStartOffset() {
        return rowStartOffset;
    }

    public void setRowStartOffset(int rowStartOffset) {
        this.rowStartOffset = rowStartOffset;
    }

    public String getSource() {
        return getAttribute("source");
    }

    public void setSource(String sourceFile) {
        setAttribute("source", sourceFile);
    }

    public void getMetaFrom(TableMeta meta) {
        if (meta == null) return;
        for (String key : meta.getAttributes().keySet()) {
            if (!key.equals("source")) {
                meta.setAttribute(key, meta.getAttribute(key));
            }
        }
    }

    public TableDef clone() {
        TableDef copy = (TableDef) super.clone();
        copy.cols = new ArrayList<>(cols);
        copy.colOffsets = colOffsets;
        copy.lineWidth = lineWidth;
        copy.rowCount = rowCount;
        copy.rowStartOffset = rowStartOffset;
        copy.lineSepLength = lineSepLength;
        return copy;
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
