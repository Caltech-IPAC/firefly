/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.catalog;

import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.List;
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

    public String getCatalogSearchProcessor() { return _row.getValue("catSearchProcessor"); }
    public String getDDSearchProcessor() { return _row.getValue("ddSearchProcessor"); }

    public String getInfoURL() { return _row.getValue("infourl"); }
    public String getDdLink() { return _row.getValue("ddlink"); }
    public int getMaxArcSec() {
        return StringUtils.getInt(_row.getValue("coneradius"));
    }
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


    public static BaseTableData.RowData makeTableRow( String projectshort,
                                                      String subtitle,
                                                      String description,
                                                      String server,
                                                      String catname,
                                                      String cols,
                                                      String nrows,
                                                      String coneradius,
                                                      String infourl,
                                                      String ddlink,
                                                      String catSearchProcessor,
                                                      String ddSearchProcessor
                                                      ) {

        List<String> cStrList= Arrays.asList(
                     "projectshort", "subtitle", "description",
                     "server", "catname", "cols",
                     "nrows", "coneradius", "infourl", "ddlink",
                     "catSearchProcessor", "ddSearchProcessor"
                     );


        String r1Data[]= {projectshort,  subtitle,  description,
                          server,  catname,  cols,
                          nrows,  coneradius,  infourl,  ddlink,
                          catSearchProcessor, ddSearchProcessor
        };
        return new BaseTableData.RowData(cStrList,r1Data);
    }
}

