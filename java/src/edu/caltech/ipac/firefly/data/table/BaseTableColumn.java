package edu.caltech.ipac.firefly.data.table;


/**
 * Date: Nov 14, 2007
 *
 * @author loi
 * @version $Id: BaseTableColumn.java,v 1.13 2011/11/11 20:51:10 loi Exp $
 */
public class BaseTableColumn implements TableDataView.Column {
    private String title;
    private String name;
    private String type;
    private TableDataView.Align align;
    private int width;
    private int prefWidth = 0;
    private String units;
    private boolean isSortable = true;
    private boolean isHidden = false;
    private boolean isVisible = true;
    private String shortDesc;
    private String[] enumVals;
    private String[] sortByCols;
    private boolean requiresQuotes = false;

    public BaseTableColumn() {}

    public BaseTableColumn(String header) {
        this(header, TableDataView.Align.LEFT, 10, true);
    }

    public BaseTableColumn(String name, TableDataView.Align align, int width, boolean sortable) {
        this.name = name;
        this.title = name;
        this.shortDesc = name;
        this.align = align;
        this.width = width;
        this.isSortable = sortable;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getShortDesc() {
        return this.shortDesc;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TableDataView.Align getAlign() {
        return align;
    }

    public void setAlign(TableDataView.Align align) {
        this.align = align;
    }

    /**
     * returns recommnended width for this columns.
     */
    public int getWidth() {
        return width;
    }

    /**
     * set the recommnended width for this columns.
     * @param width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    public int getPrefWidth() {
        return prefWidth > 0 ? prefWidth : width;
    }

    public void setPrefWidth(int prefWidth) {
        this.prefWidth = prefWidth;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSortable() {
        return isSortable;
    }

    public void setSortable(boolean sortable) {
        isSortable = sortable;
    }

    public boolean isHidden() {
        return isHidden || !isVisible();
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public void setEnums(String[] enumVals) {
        this.enumVals = enumVals;
    }

    public String[] getEnums() {
        return enumVals;
    }

    public String[] getSortByCols() {
        return sortByCols;
    }

    public void setSortByCols(String[] secondarySortCols) {
        this.sortByCols = secondarySortCols;
    }

    public boolean isRequiresQuotes() {
        return requiresQuotes;
    }

    public void setRequiresQuotes(boolean requiresQuotes) {
        this.requiresQuotes = requiresQuotes;
    }


//====================================================================
//  private methods
//====================================================================

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
