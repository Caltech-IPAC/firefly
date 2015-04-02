package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.IpacTableExtractor;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataType;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * Temp way of getting dd tables:
 *   mysql -h *HOSTNAME* -u *USERNAME* -p *PWD*
 *
 *   select name, description, type, unit from md_Column
 *      where tableId in
 *          (select tableId from md_Table
 *              where name='Science_Ccd_Exposure')
 * @author tatianag
 */
@SearchProcessorImpl(id = "LSSTCatalogDD", params =
        {@ParamDoc(name=CatalogRequest.CATALOG, desc="catalog table to query")
        })
public class QueryLSSTCatalogDD extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log= Logger.getLogger();

    private static String DD_URI = AppProperties.getProperty("lsst.schema.uri");
    private static String DD_USER = AppProperties.getProperty("lsst.schema.user");
    private static String DD_PASS = AppProperties.getProperty("lsst.schema.pass");

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        File file = createFile(request);

        DbInstance dbinstance = new DbInstance(
                false, //pooled
                "null", //datasourcePath
                DD_URI, //dbUrl
                DD_USER, DD_PASS, "com.mysql.jdbc.Driver",
                "lsstdd");
        DataSource ds = JdbcFactory.getDataSource(dbinstance);
        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            throw new RuntimeException(CatalogRequest.CATALOG + " parameter is required");
        }

        String sql = "select name, description, type, unit, IF(notNull>0,\"y\",\"n\") as sel from md_Column where tableId in (select tableId from md_Table where name='"+catTable+"')";

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DataSourceUtils.getConnection(ds);
            long cTime = System.currentTimeMillis();
            stmt = conn.createStatement();
            _log.briefDebug ("Executing SQL query: " + sql);
            //ResultSet rs = stmt.executeQuery(sql);
            IpacTableExtractor.query(null, ds, file, 10000, sql);
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

        return file;
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


    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        meta.setAttribute("col.dbtype.Visibility", "hide");
    }

}
