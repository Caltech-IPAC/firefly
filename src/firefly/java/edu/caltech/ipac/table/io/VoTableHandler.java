/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.ResourceInfo;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static edu.caltech.ipac.firefly.server.db.DuckDbAdapter.addRow;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.colIdxWithArrayData;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.serialize;

/**
 * Date: 10/23/24
 *
 * @author loi
 * @version : $
 */
public interface VoTableHandler {
    default void start() {};
    default void end() {};
    default void startTable(int url) throws DataAccessException {};
    default void endTable(int url) throws DataAccessException {};
    boolean headerOnly();
    void resources(List<ResourceInfo> resourceInfo) throws DataAccessException;
    void header(DataGroup header) throws DataAccessException;
    void data(Object[] row ) throws DataAccessException;

    abstract class Base implements VoTableHandler {
        protected List<ResourceInfo> resourceInfo;
        DataGroup meta;     // additional meta to include with along with the data
        protected boolean headerOnly;
        protected boolean searchForSpectrum;

        public Base(DataGroup meta, boolean headerOnly, boolean searchForSpectrum) {
            this.meta = meta;
            this.headerOnly = headerOnly;
            this.searchForSpectrum = searchForSpectrum;
        }

        public void resources(List<ResourceInfo> resourceInfo) throws DataAccessException {
            this.resourceInfo = resourceInfo;
        }

        public void header(DataGroup header) throws DataAccessException {
            header.addMetaFrom(meta);
            header.setResourceInfos(resourceInfo);
            if (searchForSpectrum) SpectrumMetaInspector.searchForSpectrum(header,true);
        }

        public boolean headerOnly() {
            return headerOnly;
        }
    }

    class DataGroups extends Base {
        List<DataGroup> allTables = new ArrayList<>();

        public DataGroups(boolean headerOnly, boolean searchForSpectrum) {
            super(null, headerOnly, searchForSpectrum);
        }

        public void header(DataGroup header) throws DataAccessException {
            super.header(header);
            allTables.add(header);
        }

        public void data(Object[] row) throws DataAccessException {
            allTables.getLast().add(row);
        }

        public DataGroup[] getAllTable() {
            return allTables.toArray(new DataGroup[0]);
        }

        public DataGroup getTable(int index) {
            return allTables.get(index);
        }
    }

    class DbIngest extends Base {
        DbAdapter dbAdapter;
        DataGroup header;
        DataType[] cols;
        int rowCnt;
        DuckDBAppender appender;
        DuckDBConnection conn;
        List<Integer> aryIdx;

        public DbIngest(DbAdapter dbAdapter, DataGroup meta) {
            super(meta, false, true);
            this.dbAdapter = dbAdapter;
        }

        public void header(DataGroup header) throws DataAccessException {
            super.header(header);
            this.header = header;
            cols = EmbeddedDbUtil.makeDbCols(header);
            aryIdx = colIdxWithArrayData(cols);

            dbAdapter.ingestData(() -> header, dbAdapter.getDataTable());

            try {
                // prepare to ingest data into database
                conn = (DuckDBConnection) JdbcFactory.getDataSource(dbAdapter.getDbInstance()).getConnection();
                conn.setAutoCommit(false);
                appender = conn.createAppender(DuckDBConnection.DEFAULT_SCHEMA, dbAdapter.getDataTable());
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }

        public void data(Object[] row) throws DataAccessException {
            try {
                aryIdx.forEach(idx -> row[idx] = serialize(row[idx]));      // serialize array data if necessary
                addRow(appender, row, ++rowCnt);
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }

        public void end() {
            try {
                if(appender != null) {
                    appender.flush();
                    appender.close();
                }
                if(conn != null) {
                    conn.commit();
                    conn.close();
                }
            } catch (SQLException e) {
                Logger.getLogger().warn(e);
            }
        }
    }
}

