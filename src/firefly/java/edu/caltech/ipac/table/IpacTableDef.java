package edu.caltech.ipac.table;

import edu.caltech.ipac.util.KeyVal;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains information needed to read a portion of an ipac table file into a DataGroup.
 *
 * @author loi
 * @version $Id: $
 */
public class IpacTableDef extends TableMeta {
    private List<DataType> cols = new ArrayList<>();
    private List<Integer> colOffsets = new ArrayList<>();
    private int lineWidth;
    private int rowCount;
    private int rowStartOffset;
    private transient KeyVal<Integer, String> extras;     // used by IpacTableUtil to store extras data while parsing an ipac table via input stream
    private transient TableUtil.ParsedInfo parsedInfo = new TableUtil.ParsedInfo();      // used by IpacTableUtil to store check logic on a variety of things.

    public KeyVal<Integer, String> getExtras() {
        return extras;
    }

    public TableUtil.ParsedColInfo getParsedInfo(String cname) {
        return parsedInfo.getInfo(cname);
    }

    public int getColOffset(int idx) {
        return colOffsets.get(idx);
    }

    public void setColOffsets(int idx, int offset) {
        colOffsets.add(idx, offset);
    }

    public void setExtras(Integer numHeaderLines, String unreadLine) {
        this.extras = new KeyVal<>(numHeaderLines, unreadLine);
    }

    public List<DataType> getCols() {
        return cols;
    }

    public void setCols(List<DataType> cols) {
        this.cols = cols;
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

    public IpacTableDef clone() {
        IpacTableDef copy = (IpacTableDef) super.clone();
        copy.cols = new ArrayList<>(cols);
        copy.colOffsets = new ArrayList<>(colOffsets);
        copy.lineWidth = lineWidth;
        copy.rowCount = rowCount;
        copy.rowStartOffset = rowStartOffset;
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
