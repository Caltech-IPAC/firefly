package edu.caltech.ipac.firefly.ui.catalog;

import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Oct 30, 2009
 * Time: 12:52:28 PM
 */



/**
 * @author Trey Roby
 */
public class Catalog {

    private final BaseTableData.RowData _row;


    public Catalog( BaseTableData.RowData datasetRow) { _row = datasetRow; }

    public String getProjStr() { return _row.getValue("projectshort"); }
    public String getCatagoryStr() { return _row.getValue("subtitle"); }
    public String getServer() { return _row.getValue("server"); }
    public String getQueryCatName() { return _row.getValue("catname"); }
    public String getDesc() { return _row.getValue("description"); }
    public long getColCnt() { return StringUtils.getLong(_row.getValue("cols")); }
    public long getRowCnt() {
        // the column is named "nrows" in Oracle DBs, "rows" in other DBs
        long nrows = StringUtils.getLong(_row.getValue("rows"), -1l);
        if (nrows == -1l) {
            nrows = StringUtils.getLong(_row.getValue("nrows"), -1l);
        }
        return nrows;
    }
    public String getInfoURL() { return _row.getValue("infourl"); }
    public String getDdLink() { return _row.getValue("ddlink"); }
    public int getMaxArcSec() { return StringUtils.getInt(_row.getValue("coneradius")); }
    public BaseTableData.RowData  getDataSetRow() { return _row; }


    @Override
    public String toString() {
        return getProjStr()+"->" +getCatagoryStr()+"->"+getQueryCatName();
    }

    @Override
    public int hashCode() { return toString().hashCode(); }

    @Override
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof Catalog) {
            Catalog c= (Catalog)other;
            if (ComparisonUtil.equals(getQueryCatName(),c.getQueryCatName())) {
                retval= true;
            }
        }
        return retval;
    }
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
