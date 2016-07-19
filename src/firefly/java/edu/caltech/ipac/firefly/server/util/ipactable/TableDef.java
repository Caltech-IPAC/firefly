package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
* Date: 6/26/15
*
* @author loi
* @version $Id: $
*/
public class TableDef {
    private List<DataType> cols = new ArrayList<DataType>();
    private HashMap<String, DataGroup.Attribute> attributes = new HashMap<>();
    private int lineWidth;
    private int rowCount;
    private int rowStartOffset;
    private int lineSepLength;

    public void addAttributes(DataGroup.Attribute... attributes) {
        if (attributes != null) {
            for(DataGroup.Attribute a : attributes) {
                this.attributes.put(a.getKey(), a);
            }
        }
    }
    public void setCols(List<DataType> cols) { this.cols = cols; }

    public List<DataType> getCols() {
        return cols;
    }

    public List<DataGroup.Attribute> getAttributes() {
        return  new ArrayList<>(attributes.values());
    }

    public boolean contains(String name) { return attributes.containsKey(name);};

    public void setAttribute(String name, String value) {
        attributes.put(name, new DataGroup.Attribute(name, value));
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void setStatus(DataGroupPart.State status) {
        addAttributes(new DataGroup.Attribute(DataGroupPart.LOADING_STATUS, status.name()));
    }

    DataGroup.Attribute getAttribute(String key) {
        return attributes.get(key);
    }

    public void ensureStatus() {
        DataGroup.Attribute a = getAttribute(DataGroupPart.LOADING_STATUS);
        if (a == null || StringUtils.isEmpty(a.getValue())) {
            setStatus(DataGroupPart.State.COMPLETED);
        }
    }

    public DataGroupPart.State getStatus() {
        ensureStatus();
        DataGroup.Attribute a = getAttribute(DataGroupPart.LOADING_STATUS);
        return DataGroupPart.State.valueOf(String.valueOf(a.getValue()));
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
        DataGroup.Attribute source = getAttribute("source");
        return source != null ? source.getValue() : null;
    }

    public void setSource(String sourceFile) {
        setAttribute("source", sourceFile);
    }

    public void setMetaTo(TableMeta meta) {
        if (meta == null) return;
        if (contains("groupByCols")) {
            meta.setGroupByCols(StringUtils.asList(getAttribute("groupByCols").getValue(), ","));
        }
        if (contains("relatedCols")) {
            meta.setRelatedCols(StringUtils.asList(getAttribute("relatedCols").getValue(), ","));
        }
        if (contains("fileSize")) {
            meta.setFileSize(Long.parseLong(getAttribute("fileSize").getValue()));
        }
        meta.setIsLoaded(Boolean.parseBoolean(getAttribute("isFullyLoaded").getValue()));
        for (String key : meta.getAttributes().keySet()) {
            if (!key.equals("source")) {
                setAttribute(key, meta.getAttribute(key));
            }
        }
    }

    public void getMetaFrom(TableMeta meta) {
        if (meta == null) return;
        if (meta.getGroupByCols().size() > 0) {
            setAttribute("groupByCols", StringUtils.toString(meta.getGroupByCols()));
        }
        if (meta.getRelatedCols().size() > 0) {
            setAttribute("relatedCols", StringUtils.toString(meta.getRelatedCols()));
        }
        if (meta.getFileSize() > 0) {
            setAttribute("fileSize", String.valueOf( meta.getFileSize()) );
        }
        setAttribute("isFullyLoaded", String.valueOf(meta.isLoaded()));
        for (String key : meta.getAttributes().keySet()) {
            if (!key.equals("source")) {
                setAttribute(key, meta.getAttribute(key));
            }
        }
    }
    public TableDef clone() {
        TableDef copy = new TableDef();
        copy.cols = new ArrayList<>(cols);
        copy.attributes = new HashMap<>(attributes);
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
