/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

