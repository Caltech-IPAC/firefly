package edu.caltech.ipac.firefly.server.db.spring.mapper;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.CollectionUtil;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class SqlResultSetUtil {
    private static Logger.LoggerImpl LOG = Logger.getLogger();
    private final DataSource datasource;
    private ResultSet resultset;
    private Connection conn;
    private PreparedStatement stmt;

    public SqlResultSetUtil(DataSource ds) {
        this.datasource = ds;
    }

    public DataSource getDataSource() {
        return this.datasource;
    }
    public DataGroup doQuery(String sql, Object... params) {
        DataGroup dg = null;
        try {
            open();
            stmt = conn.prepareStatement(sql);
            stmt.setFetchSize(200);
            if (params != null) {
                for (int i = 1; i <= params.length; i++) {
                    stmt.setObject(i, params[i - 1]);
                }
            }
            LOG.info("Executing SQL query: " + sql,
                    "         Parameters: " + "{" + CollectionUtil.toString(params) + "}");
            resultset = stmt.executeQuery();
            dg = DataGroupUtil.processResults(resultset,dg);
        } catch (SQLException e) {
            LOG.error("Error executing sql:" + sql + "\n" + e.getClass().getName() + ": " + e.getMessage());
            throw new RuntimeException("Error executing sql", e);
        } finally {
            close();
        }
        return dg;
    }
    private void open() {
        try {
            conn = datasource.getConnection();
        } catch (SQLException e) {
            throw new IllegalArgumentException("DataSource not valid");
        }
    }
    private void close() {
        try {
            if (resultset != null) {
                resultset.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null && !conn.isClosed()) {
                //SingleConnectionDataSource is used, connection must be released
                conn.close();
            }
        } catch (SQLException e) {
            LOG.warn(e, "Error while cleaning up db resources");
        }
    }
    public void populate(DataGroup dg, ResultSet rs) {
        DataType[] headers = dg.getDataDefinitions();
        DataObject dObj = new DataObject(dg);
        for (int i = 0; i < headers.length; i++) {
            DataType dt = headers[i];
            try {
                Object obj = rs.getObject(dt.getKeyName());
                dObj.setDataElement(dt, obj);
            } catch (SQLException e) {
                LOG.warn(e, "SQLException at col:" + dt.getKeyName());
            }
        }
        dg.add(dObj);
    }

}
