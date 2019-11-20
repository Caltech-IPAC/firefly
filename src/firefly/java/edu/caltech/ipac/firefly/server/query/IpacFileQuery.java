/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.SqlResultSetUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import org.json.simple.JSONObject;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: IpacFileQuery.java,v 1.22 2011/01/10 19:36:00 tatianag Exp $
 */
abstract public class IpacFileQuery extends EmbeddedDbProcessor implements Query {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public String getTemplateName() {
        return null;
    }

    @Override
    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {
        DataSource ds = getDataSource();
        SqlResultSetUtil ext = new SqlResultSetUtil(ds);
        DataGroup dg = null;
        if (ds == null) throw new DataAccessException("Unable to connect to database");
        try {
            boolean proceed = onBeforeQuery(request, ds);
            String sql = getSql(request);
            if (proceed && sql != null) {
                Object [] sqlParams = getSqlParams(request);
                dg = ext.doQuery(sql,sqlParams);
            } else {
                closeConnection(ds);  // make sure connection is close
            }
        } catch (Exception ex) {
            closeConnection(ds);
            throw new DataAccessException("Unable to get data", ex);
        }
        return dg;
    }

    private void closeConnection(DataSource ds) {
        if (ds != null) {
            try {
                //SingleConnectionDataSource is always used
                Connection conn = ds.getConnection();
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.error(e);
            }
        }
    }

//    private File createEmptyFile(TableServerRequest request) {
//        // TODO: better way to deal with empty results
//        File file = null;
//        try {
//            file = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getPermWorkDir());
//            DataGroupWriter.write(file, new DataGroup("Empty Set", new DataType[0]), request.getPageSize());
//        } catch (IOException e) {
//            LOGGER.error(e);
//        }
//        return file;
//    }

    /**
     * This method is called by loadData before any query related calls
     * @param request server request
     * @param datasource
     * @throws IOException on io problem
     * @throws DataAccessException on any other problem
     */
    @SuppressWarnings("unused")
    protected boolean onBeforeQuery(TableServerRequest request, DataSource datasource) throws IOException, DataAccessException {
        return true;
    }

    /**
     * @return a single connection datasource. this allow sub-routine to share
     * the same connection.
     */
    private DataSource getDataSource() {
        return JdbcFactory.getSingleConnectionDS(getDbInstance());
    }


    public static void main(String[] args){
        Date date = new Date();
        Map<String, Date> m = new HashMap();
        m.put("data",date);
        new JSONObject(m).toJSONString();
    }
}

