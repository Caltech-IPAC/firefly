/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.IpacTableExtractor;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.DESC_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;

/**
 * @author tatianag
 */
@SearchProcessorImpl(id = "LSSTCatalogQuery", params=
        {@ParamDoc(name="RequestedDataSet", desc="catalog table to query"),
         @ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees for cone search"),
         @ParamDoc(name="size", desc="size in degrees for box search"),
         @ParamDoc(name = CatalogRequest.SELECTED_COLUMNS, desc = "a comma separated list of columns to return, empty gives the default list"),
         @ParamDoc(name = CatalogRequest.CONSTRAINTS, desc = "a where fragment of the column constrains")
        })

public class QueryLSSTCatalog  extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log= Logger.getLogger();

    private static String QSERV_URI = AppProperties.getProperty("lsst.qserv.uri");
    private static String QSERV_USER = AppProperties.getProperty("lsst.qserv.user");
    private static String QSERV_PASS = AppProperties.getProperty("lsst.qserv.pass");


    private static final String RA_COL = "ra";
    private static final String DEC_COL = "decl";

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        File file = createFile(request);

        WorldPt pt = request.getWorldPtParam(ServerParams.USER_TARGET_WORLD_PT);
        pt = VisUtil.convertToJ2000(pt);
        doGetData(file, request.getParams(), pt);
        return file;
    }

    //qserv_areaspec_circle does not work at the moment, only qserv_areaspec_box works
    void doGetData(File oFile, List<Param> params, WorldPt wpt) throws DataAccessException {
        DbInstance dbinstance = new DbInstance(
                false, //pooled
                "null", //datasourcePath
                QSERV_URI, //dbUrl
                QSERV_USER, StringUtils.isEmpty(QSERV_PASS)?null:QSERV_PASS, "com.mysql.jdbc.Driver",
                "lsstdb");
        DataSource ds = JdbcFactory.getDataSource(dbinstance);

        String searchMethod = null;
        String catTable =  null;
        String selectedColumns = null;
        String[] constraints = null;
        for (Param p : params) {
            String pname = p.getName();
            if (pname.equals(CatalogRequest.SEARCH_METHOD)) {
                searchMethod = p.getValue();
            } else if (pname.equals(ServerParams.REQUESTED_DATA_SET)) {
                catTable = p.getValue();
            } else if (pname.equals(CatalogRequest.SELECTED_COLUMNS)) {
                selectedColumns = p.getValue();
            } else if (pname.equals(CatalogRequest.CONSTRAINTS)) {
                String val = p.getValue();
                if (!StringUtils.isEmpty(val)) {
                    _log.briefDebug("CONSTRAINTS: "+val);
                    constraints = val.split(",");
                }
            }
        }

        String update = getSelectedColumnsUpdate(selectedColumns);
        if (update != null) selectedColumns = update;

        String pname;
        String sql;
        if (searchMethod != null && searchMethod.equals(CatalogRequest.Method.CONE.getDesc())) {
            double radius = 0.0;
            for (Param p : params) {
                pname = p.getName();
                if (pname.equals(CatalogRequest.RADIUS)) {
                    radius = Double.parseDouble(p.getValue());
                }
            }
            //qserv_areaspec_circle(ra, dec, radius)
            //sql="select * from "+catTable+" where qserv_areaspec_circle("+wpt.getLon()+", "+wpt.getLat()+", "+radius+")";
            //Per Serge, the above query only can not be applied to unpartitioned table
            sql="select "+selectedColumns+" from "+catTable+" where scisql_s2PtInCircle(ra, decl, "+wpt.getLon()+", "+wpt.getLat()+", "+radius+")=1";
        } else if (searchMethod != null && searchMethod.equals(CatalogRequest.Method.BOX.getDesc())) {
            double size = 0.0;
            for (Param p : params) {
                pname = p.getName();
                if (pname.equals(CatalogRequest.SIZE)) {
                    size = Double.parseDouble(p.getValue());
                }
            }
            double halfsize = size/2.0;
            double lon1 = wpt.getLon()-halfsize;
            if (lon1<0) lon1 += 360;
            double lon2 = wpt.getLon()+halfsize;
            if (lon2>360) lon1 -= 360;
            double lat1 = wpt.getLat()-halfsize;
            if (lat1<-90) lat1 = -180 - lat1;
            double lat2 = wpt.getLat()+halfsize;
            if (lat2>90) lat2 = 180 - lat2;

            //qserv_areaspec_box(raA, decA, raB, decB)
            //sql="select * from "+catTable+" where qserv_areaspec_box("+lon1+", "+lat1+", "+lon2+", "+lat2+")";
            //Per Serge, the above query only can not be applied to unpartitioned table
            sql="select * from "+catTable+" where scisql_s2PtInBox(ra, decl, "+lon1+", "+lat1+", "+lon2+", "+lat2+")=1";

        } else {
            throw new RuntimeException(searchMethod+" search is not Implemented");
        }

        // add additional constraints
        if (constraints != null) {
            for (String constr : constraints) {
                sql += " and "+constr;
            }
        }

        // workaround for https://jira.lsstcorp.org/browse/DM-1841
        sql += " LIMIT 3000";

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DataSourceUtils.getConnection(ds);
            long cTime = System.currentTimeMillis();
            stmt = conn.createStatement();
            _log.briefDebug ("Executing SQL query: " + sql);
            //ResultSet rs = stmt.executeQuery(sql);
            IpacTableExtractor.query(null, ds, oFile, 10000, sql);
            _log.briefDebug ("SELECT took "+(System.currentTimeMillis()-cTime)+"ms");
        } catch (Exception e) {
            _log.error(e);
            throw new DataAccessException("Query failed: "+e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    closeStatement(stmt);
                } catch (Exception e1) {}
            }
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, ds);
            }
        }
    }


    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);

        TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns(RA_COL, DEC_COL);  //J2000 default
        meta.setLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS, llc);
        meta.setCenterCoordColumns(llc);

        String title = request.getParam(ServerParams.REQUESTED_DATA_SET);
        title = (title == null) ? "LSST" : "LSST "+title;
        meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, title);
        meta.setAttribute(MetaConst.DATA_PRIMARY, "False");

        setColumnTips(meta, request);
    }

    private static String getSelectedColumnsUpdate(String selectedColumns) {
        String update=null;
        if (StringUtils.isEmpty(selectedColumns)) {
            return "*";
        } else {
            boolean hasRa = false, hasDec = false;
            String [] cols = selectedColumns.split(",");
            for (String col : cols) {
                if (col.equalsIgnoreCase(RA_COL)) {
                    hasRa= true;
                    if (hasDec) break;
                } else if (col.equalsIgnoreCase(DEC_COL)) {
                    hasDec= true;
                    if (hasRa) break;
                }
            }
            if (!hasRa) {
                update = selectedColumns+",ra";
            }
            if (!hasDec) {
                update = (update==null?selectedColumns:update)+",decl";
            }
            return update;
        }

    }

    static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Throwable th) {
                // log and ignore
                _log.warn(th, "Failed to close statement: "+th.getMessage());
            }
        }
    }

    protected void setColumnTips(TableMeta meta, ServerRequest request) {

        TableServerRequest req = new TableServerRequest("LSSTCatalogDD");
        req.setPageSize(1000);
        req.setParam(CatalogRequest.CATALOG, request.getParam(ServerParams.REQUESTED_DATA_SET));

        SearchManager sm = new SearchManager();
        DataGroupPart dgp = new DataGroupPart();

        try {
            dgp = sm.getDataGroup(req);
        } catch (Exception e) {
        }

        DataGroup dg = dgp.getData();
        if (dg != null) {
            for (int i = 0; i < dg.size(); i++) {
                DataObject dObj = dg.get(i);
                String tipStr = "";

                String descStr = (String) dObj.getDataElement("description");
                if (!StringUtils.isEmpty(descStr) && !descStr.equalsIgnoreCase("null")) {
                    tipStr += descStr;
                }

                String unitStr = (String) dObj.getDataElement("unit");
                if (!StringUtils.isEmpty(unitStr) && !unitStr.equalsIgnoreCase("null")) {
                    if (tipStr.length() > 0) {
                        tipStr += " ";
                    }
                    tipStr += "(" + unitStr + ")";
                }

                String nameStr = (String) dObj.getDataElement("name");
                meta.setAttribute(makeAttribKey(DESC_TAG, nameStr.toLowerCase()), tipStr);
            }
        }
    }

    public static void main(String[] args) {
        DbInstance dbinstance = new DbInstance(
                false, //pooled
                "null", //datasourcePath
                "<REPLACE_WITH_DB_URL>", //dbUrl
                "qsmaster", null, "com.mysql.jdbc.Driver",
                "lsstdb");
        DataSource ds = JdbcFactory.getDataSource(dbinstance);

        String sql="select * from DeepSource where qserv_areaspec_box(0.4, 1.05, 0.5, 1.15)";


        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(ds);
            long cTime = System.currentTimeMillis();

            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                _log.briefDebug ("Executing SQL query: " + sql);
                //ResultSet rs = stmt.executeQuery(sql);
                //_log.briefDebug ("SELECT took "+(System.currentTimeMillis()-cTime)+"ms");
                File file = new File("/tmp/lsstTest.tbl");
                IpacTableExtractor.query(null, ds, file, 10000, sql);
                _log.briefDebug ("SELECT took "+(System.currentTimeMillis()-cTime)+"ms");
            } catch (Exception e) {
                System.out.println("Exception "+e.getMessage());
            } finally {
                closeStatement(stmt);
            }

        } catch (Exception e) {
            System.out.println("Exception "+e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, ds);
            }
        }
    }


}
