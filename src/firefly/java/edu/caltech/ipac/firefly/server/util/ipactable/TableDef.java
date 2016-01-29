package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
* Date: 6/26/15
*
* @author loi
* @version $Id: $
*/
public class TableDef {
    private List<DataType> cols = new ArrayList<DataType>();
    private ArrayList<DataGroup.Attribute> attributes = new ArrayList<DataGroup.Attribute>();
    private int lineWidth;
    private int rowCount;
    private int colCount;
    private int rowStartOffset;
    private String sourceFile;
    private int lineSepLength;

    public void addAttributes(DataGroup.Attribute... attributes) {
        if (attributes != null) {
            for(DataGroup.Attribute a : attributes) {
                this.attributes.add(a);
            }
        }
    }

    public List<DataType> getCols() {
        return cols;
    }

    public void addCols(DataType col) {
        cols.add(col);
    }

    public List<DataGroup.Attribute> getAttributes() {
        return attributes;
    }

    public void setAttribute(String name, String value) {
        DataGroup.Attribute att = getAttribute(name);
        if (att != null) {
            attributes.remove(att);
        }
        attributes.add(new DataGroup.Attribute(name, value));
    }

    public void setStatus(DataGroupPart.State status) {
        addAttributes(new DataGroup.Attribute(DataGroupPart.LOADING_STATUS, status.name()));
    }

    DataGroup.Attribute getAttribute(String key) {
        for (DataGroup.Attribute at : attributes) {
            if (at.getKey().equals(key)) return at;
        }
        return null;
    }

    public DataGroupPart.State getStatus() {
        DataGroup.Attribute a = getAttribute(DataGroupPart.LOADING_STATUS);
        if (a != null && !StringUtils.isEmpty(a.getValue())) {
            return DataGroupPart.State.valueOf(String.valueOf(a.getValue()));
        } else {
            return DataGroupPart.State.COMPLETED;
        }
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

    public int getColCount() {
        return colCount;
    }

    public void setColCount(int colCount) {
        this.colCount = colCount;
    }

    public int getRowStartOffset() {
        return rowStartOffset;
    }

    public void setRowStartOffset(int rowStartOffset) {
        this.rowStartOffset = rowStartOffset;
    }

    public String getSource() {
        return sourceFile;
    }

    public void setSource(String sourceFile) {
        this.sourceFile = sourceFile;
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
