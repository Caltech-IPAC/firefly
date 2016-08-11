/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author tatianag
 *         $Id: UserCatalogQuery.java,v 1.1 2011/09/23 23:37:51 tatianag Exp $
 */
@SearchProcessorImpl(id ="userCatalogFromFile")
public class UserCatalogQuery extends DynQueryProcessor {

    private static final String RA = "ra";
    private static final String DEC = "dec";
    private final static String DEFAULT_TNAME_OPTIONS[] = {
            "name",         // generic
            "pscname",      // IRAS
            "target",       // our own table output
            "designation",  // 2MASS
            "objid",        // SPITZER
            "starid"        // PCRS
    };

    private static Pattern raP = Pattern.compile("(^|[-_ ])ra($|[^\\p{Alpha}])");
    private static Pattern decP = Pattern.compile("(^|[-_ ])dec($|[^\\p{Alpha}])");



    protected File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {

        String filePath = req.getParam("filePath");
        if (!StringUtils.isEmpty(filePath)) {
            File f = ServerContext.convertToFile(filePath);
            return convertToIpacTable(f, req);
        } else {
            throw new DataAccessException("filePath parameter is not found");
        }
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {

        addCatalogMeta(meta,columns,request);
        super.prepareTableMeta(meta, columns, request);
    }


    public static void addCatalogMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {


        TableMeta.LonLatColumns llc = findLonLatCols(columns, (TableServerRequest) request);
        if (llc != null) {
            meta.setLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS, llc);
            meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "USER");
            meta.setAttribute(MetaConst.DATA_PRIMARY, "False");
        }

        String name = findTargetName(columns);
        if (name != null) meta.setAttribute(MetaConst.CATALOG_TARGET_COL_NAME, name);
    }

    private static TableMeta.LonLatColumns findLonLatCols(List<DataType> columns, TableServerRequest request) {
        String lonCol = null, latCol = null;
        TableMeta.LonLatColumns llc = TableMeta.LonLatColumns.parse(request.getMeta(MetaConst.CATALOG_COORD_COLS));
        if (llc == null) {
            // guess best ra/dec columns
            for (DataType col : columns) {
                String cname = col.getKeyName();
                if (cname.equalsIgnoreCase(RA)) lonCol = cname;
                if (cname.equalsIgnoreCase(DEC)) latCol = cname;

                if (lonCol == null && raP.matcher(cname).find()) lonCol = cname;
                if (latCol == null && decP.matcher(cname).find()) latCol = cname;
            }
            if (lonCol != null && latCol != null) {
                llc = new TableMeta.LonLatColumns(lonCol, latCol, CoordinateSys.EQ_J2000);
            }
        }
        return llc;
    }

    private static String findTargetName(List<DataType> columns) {
        String cname;
        String finalName = null;
        for (DataType col : columns) {
            cname = col.getKeyName().toLowerCase();
            for (String testName : DEFAULT_TNAME_OPTIONS) {
                if (cname.contains(testName)) {
                    finalName = col.getKeyName();
                    break;
                }

            }
            if (finalName != null) break;
        }
        return finalName;
    }

}
