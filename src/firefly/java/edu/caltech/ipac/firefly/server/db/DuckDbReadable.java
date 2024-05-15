/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;

import java.io.File;
import java.util.List;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public abstract class DuckDbReadable extends DuckDbAdapter {

    public DuckDbReadable(File dbFile) {
        super(dbFile);
    }

    /**
     * This adapter store the data file as dbFile.  This data file can be directly imported into DuckDB.
     * @return the .duckdb file associated with this database
     */
    public File getDuckDbFile() {
        String fname =  getDbFile().getName()+ ".duckdb";
        return new File(getDbFile().getParentFile(), fname);
    }

    public DataGroup getInfo() throws DataAccessException {
        StopWatch.getInstance().start("getInfo: " + getDbFile().getAbsolutePath());
        int count = JdbcFactory.getSimpleTemplate(getDbInstance()).queryForInt(String.format("SELECT count(*) from '%s'", getDbFile()));
        DataGroup table = execQuery(String.format("SELECT * from '%s' LIMIT 0", getDbFile()), null);
        table.setSize(count);
        StopWatch.getInstance().printLog("getInfo: " + getDbFile().getAbsolutePath());
        return table;
    }

    public FileInfo ingestData(DataGroupSupplier dataGroupSupplier, String forTable) throws DataAccessException {

        if (!forTable.equals(getDataTable())) return super.ingestData(dataGroupSupplier, forTable);

        StopWatch.getInstance().start(String.format("%s:ingestData for %s", getName(), forTable));

        // import data directly from source file.
        // no need to get data from dataGroupSupplier.  query the data directly from the dbFile
        var jdbc = JdbcFactory.getSimpleTemplate(getDbInstance());

        StopWatch.getInstance().start("  ingestData: load data for " + forTable);

        // create data table
        String dataSqlWithIdx = String.format("select b.*, (%s - 1) as %s, (%s - 1) as %s from '%s' as b", rownumSql(), DataGroup.ROW_IDX, rownumSql(), DataGroup.ROW_NUM, getDbFile().getAbsolutePath());
        String sql = createTableFromSelect(forTable, dataSqlWithIdx);
        jdbc.update(sql);

        // copy dd
        jdbc.update(createDDSql(forTable));
        String insert = String.format("INSERT INTO %s %s", forTable + "_DD", ddSql());
        jdbc.update(insert);


        // create META table
        jdbc.update(createMetaSql(forTable));
        String metaSql = metaSql();
        if (metaSql != null) {
            String meta = String.format("INSERT INTO %s %s", forTable + "_META", metaSql());
            jdbc.update(meta);
        }

        // copy aux
        jdbc.update(createAuxDataSql(forTable));
        String auxSql = auxSql();
        if (auxSql != null) {
            String insertAux = String.format("INSERT INTO %s %s", forTable + "_AUX", auxSql);
            jdbc.update(insertAux);
        }

        StopWatch.getInstance().printLog("  ingestData: load data for " + forTable);

        StopWatch.getInstance().printLog(String.format("%s:ingestData for %s", getName(), forTable));
        return new FileInfo(getDbFile());
    }

    protected String ddSql() {
        String dd = """
           SELECT
           column_name as cname,
           NULL as label,
           NULL as type,
           NULL as units,
           NULL as null_str,
           NULL as format,
           NULL as fmtDisp,
           NULL as width,
           NULL as visibility,
           true as sortable,
           true as filterable,
           NULL as fixed,
           NULL as description,
           NULL as enumVals,
           NULL as ID,
           NULL as precision,
           NULL as ucd,
           NULL as utype,
           NULL as ref,
           NULL as maxValue,
           NULL as minValue,
           NULL as links,
           NULL as dataOptions,
           NULL as arraySize,
           NULL as cellRenderer,
           NULL as sortByCols,
           row_number() over() as order_index
           FROM (DESCRIBE table '%s')
           """;
        return String.format(dd, getDbFile().getAbsolutePath());
    }

    protected String metaSql() {
        return null;
    }

    protected String auxSql() {
        return null;
    }

//====================================================================
//  Supported file types
//====================================================================

    public static class Parquet extends DuckDbReadable implements DbAdapterCreator {
        public static final String NAME = "parquet";
        private static final List<String> SUPPORTS = List.of(NAME, "parq");

        /**
         * used by DbAdapterCreator only
         */
        Parquet() {
            super(null);
        }
        public Parquet(File dbFile) {
            super(dbFile);
        }

        public DbAdapter create(File dbFile) {
            return canHandle(dbFile) ? new Parquet(dbFile) : null;
        }

        List<String> getSupportedExts() {
            return  SUPPORTS;
        }

        public String getName() { return NAME; }
        protected String metaSql() {
            return String.format("select decode(key) as key, decode(value) as value, true as isKeyword from parquet_kv_metadata('%s')", getDbFile().getAbsolutePath());
        }
    }

    public static class Csv extends DuckDbReadable implements DbAdapterCreator{
        public static final String NAME = "csv";
        private static final List<String> SUPPORTS = List.of(NAME, "tsv");

        /**
         * used by DbAdapterCreator only
         */
        Csv() {
            super(null);
        }
        public Csv(File dbFile) { super(dbFile); }

        public DbAdapter create(File dbFile) {
            return canHandle(dbFile) ? new Csv(dbFile) : null;
        }

        List<String> getSupportedExts() {
            return  SUPPORTS;
        }

        public String getName() { return NAME; }
        protected static List<String> supportFileExtensions() { return SUPPORTS; }
    }

}
