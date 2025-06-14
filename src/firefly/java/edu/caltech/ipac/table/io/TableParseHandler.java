/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.ResourceInfo;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static edu.caltech.ipac.firefly.server.db.DuckDbAdapter.addRow;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.colIdxWithArrayData;
import static edu.caltech.ipac.firefly.core.Util.serialize;
import static edu.caltech.ipac.firefly.core.Util.Try;
/**
 * Date: 10/23/24
 *
 * @author loi
 * @version : $
 */
public interface TableParseHandler {
    default void start() {};
    default void end() {};
    default void startTable(int url) throws IOException {};
    default void endTable(int url) throws IOException {};
    boolean headerOnly();
    void resources(List<ResourceInfo> resourceInfo) throws IOException;
    void header(DataGroup header) throws IOException;
    void data(Object[] row ) throws IOException;

    abstract class Base implements TableParseHandler {
        protected List<ResourceInfo> resourceInfo;
        Consumer<DataGroup> extraMetaSetter;     // additional meta to include with along with the data
        protected boolean headerOnly;
        protected boolean searchForSpectrum;

        public Base(Consumer<DataGroup> extraMetaSetter, boolean headerOnly, boolean searchForSpectrum) {
            this.extraMetaSetter = extraMetaSetter;
            this.headerOnly = headerOnly;
            this.searchForSpectrum = searchForSpectrum;
        }

        public void resources(List<ResourceInfo> resourceInfo) throws IOException {
            this.resourceInfo = resourceInfo;
        }

        public void header(DataGroup header) throws IOException {
            if (extraMetaSetter != null)    extraMetaSetter.accept(header);
            header.setResourceInfos(resourceInfo);
            if (searchForSpectrum) SpectrumMetaInspector.searchForSpectrum(header,true);
        }

        public boolean headerOnly() {
            return headerOnly;
        }
    }

    class Memory extends Base {
        List<DataGroup> allTables = new ArrayList<>();

        public Memory(boolean headerOnly, boolean searchForSpectrum) {
            super(null, headerOnly, searchForSpectrum);
        }

        public void header(DataGroup header) throws IOException {
            super.header(header);
            allTables.add(header);
        }

        public void data(Object[] row) throws IOException {
            allTables.getLast().add(row);
        }

        public void end() {
            super.end();
            allTables.forEach(DataGroup::trimToSize);
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
        DataGroup table;
        DataType[] cols;
        int rowCnt;
        DuckDBAppender appender;
        DuckDBConnection conn;
        List<Integer> aryIdx;

        public DbIngest(DbAdapter dbAdapter, Consumer<DataGroup> extraMetaSetter, boolean searchForSpectrum) {
            super(extraMetaSetter, false, searchForSpectrum);
            this.dbAdapter = dbAdapter;
        }

        public void header(DataGroup header) throws IOException {
            super.header(header);
            table = header;
            cols = EmbeddedDbUtil.makeDbCols(table);
            aryIdx = colIdxWithArrayData(cols);

            try {
                dbAdapter.ingestData(() -> table, dbAdapter.getDataTable());

                // prepare to ingest data into database
                conn = (DuckDBConnection) JdbcFactory.getDataSource(dbAdapter.getDbInstance()).getConnection();
                conn.setAutoCommit(false);
                appender = conn.createAppender(DuckDBConnection.DEFAULT_SCHEMA, dbAdapter.getDataTable());
            } catch (SQLException | DataAccessException e) {
                throw new IOException(e);
            }
        }

        public void data(Object[] row) throws IOException {
            try {
                aryIdx.forEach(idx -> row[idx] = serialize(row[idx]));      // serialize array data if necessary
                addRow(appender, row, rowCnt++);
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }

        public void end() {
            if (conn != null)       Try.it(() -> conn.commit());
            if (appender != null)   Try.it(() -> appender.close());
            if (conn != null)       Try.it(() -> conn.close());
        }
    }
}

