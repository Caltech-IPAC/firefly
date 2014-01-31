package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.SingleColDefinition;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Nov 3, 2009
 * Time: 10:33:13 AM
 */


/**
 * @author Trey Roby
 */
public class CatalogItem extends SingleColDefinition.SingleColDef {

    private static final NumberFormat _nf= NumberFormat.getFormat("#,##0");


    public CatalogItem(String name, TableDataView tableDef) {
        super(name, tableDef);
    }

    @Override
    public String getCellValue(TableData.Row row) {
        StringBuffer s = new StringBuffer();
        Catalog workCat= new Catalog((BaseTableData.RowData)row);
        String fPercent= "style= \"font-size:80%;\"";

        s.append("<span style=\"font-weight:bold;\">");
        s.append(workCat.getDesc());
        s.append("</span>");
        s.append("<br>");
        s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        s.append("<span class=\"marked-text\"");
        s.append(fPercent);
        s.append(">Rows: </span>");
        s.append("<span class=\"normal-text\"");
        s.append(fPercent);
        s.append(">");
        s.append( _nf.format(workCat.getRowCnt()));
        s.append("</span>");
        s.append("&nbsp;&nbsp;&nbsp;&nbsp;");

        s.append("<span class=\"marked-text\"");
        s.append(fPercent);
        s.append(">Cols: </span>");
        s.append("<span class=\"normal-text\"");
        s.append(fPercent);
        s.append(">");
        s.append( _nf.format(workCat.getColCnt()));
        s.append("</span>");
        s.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");


        s.append("<span class='href-item font-size-eighty-percent' ");
        s.append(fPercent);
        s.append(">");
        s.append(workCat.getInfoURL());
        s.append("</span>");
        
        if (!StringUtils.isEmpty(workCat.getDdLink())) {
            s.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            s.append("<span class='href-item font-size-eighty-percent' ");
            s.append(fPercent);
            s.append(">");
            s.append(workCat.getDdLink());
            s.append("</span>");
        }


        return s.toString();
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
