/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.List;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public abstract class DuckDbReadable extends DuckDbAdapter {
    File sourceFile;
    File dbFile;
    String name;
    List<String> supports;

    public DuckDbReadable(File dbFile, String name, List<String> supports) {
        this.name = name;
        this.supports = supports;
        if (canHandle(dbFile)) {
            this.sourceFile = dbFile;
            useDbFileFrom(sourceFile);
        } else {
            this.dbFile = dbFile;
        }
    }

    @Override // to allow dbFile to be saved elsewhere
    public File getDbFile() {
        return dbFile;
    }

    /**
     * Convert this file name into duckdb file extension, then set it as this Adapter's dbFile
     * @param srcFile
     */
    public void useDbFileFrom(File srcFile) {
        var ext = StringUtils.groupMatch("(.+)\\.(.+)$", srcFile.getName());
        String fname = (ext == null ? srcFile.getName() : ext[0]) + "." + DuckDbAdapter.NAME;
        this.dbFile = new File(srcFile.getParentFile(), fname);
    }

    public static TableUtil.Format guessFileFormat(File srcFile) {
        if (new Parquet().canHandle(srcFile)) {
            return TableUtil.Format.PARQUET;
        }
        var csv = new Csv().canHandle(srcFile);
        var tsv = new Tsv().canHandle(srcFile);
        if (csv && tsv) {
            return  new Csv(srcFile).canRead() > new Tsv(srcFile).canRead() ? TableUtil.Format.CSV : TableUtil.Format.TSV;
        }
        if (csv) return TableUtil.Format.CSV;
        if (tsv) return TableUtil.Format.TSV;
        return null;
    }

    public File getSourceFile() { return sourceFile; }

    String getSrcFileSql() {
        return "'%s'".formatted(getSourceFile().getAbsolutePath());
    }

    List<String> getSupportedExts() {
        return  supports;
    }

    public String getName() { return name; }

    public int canRead() {
        try {
            var jdbc = JdbcFactory.getSimpleTemplate(createDbInstance());
            var rows = jdbc.queryForList("select column_name from ( describe from %s )".formatted(getSrcFileSql()));
            return rows.size();
        } catch (Exception ignored){}
        return -1;
    }

    public DataGroup getInfo() throws DataAccessException {
        StopWatch.getInstance().start("getInfo: " + getSourceFile());
        int count = JdbcFactory.getSimpleTemplate(getDbInstance()).queryForInt("SELECT count(*) from %s".formatted(getSrcFileSql()));
        DataGroup table = execQuery("SELECT * from %s LIMIT 0".formatted(getSrcFileSql()), null);
        table.setSize(count);
        StopWatch.getInstance().printLog("getInfo: " + getSourceFile());
        return table;
    }

    public FileInfo ingestData(DataGroupSupplier dataGroupSupplier, String forTable) throws DataAccessException {

        if (!forTable.equals(getDataTable())) return super.ingestData(dataGroupSupplier, forTable);

        StopWatch.getInstance().start("%s:ingestData for %s".formatted(getName(), forTable));

        // import data directly from source file.
        // no need to get data from dataGroupSupplier.  query the data directly from the dbFile
        var jdbc = JdbcFactory.getSimpleTemplate(getDbInstance());

        StopWatch.getInstance().start("  ingestData: load data for " + forTable);

        // create data table
        String dataSqlWithIdx = "select b.*, (%s - 1) as %s, (%s - 1) as %s from %s as b".formatted(rowNumSql(), DataGroup.ROW_IDX, rowNumSql(), DataGroup.ROW_NUM, getSrcFileSql());
        String sql = createTableFromSelect(forTable, dataSqlWithIdx);
        jdbc.update(sql);

        // copy dd
        jdbc.update(createDDSql(forTable));
        String insert = "INSERT INTO %s %s".formatted(forTable + "_DD", ddSql());
        jdbc.update(insert);


        // create META table
        jdbc.update(createMetaSql(forTable));
        String metaSql = metaSql();
        if (metaSql != null) {
            String meta = "INSERT INTO %s %s".formatted(forTable + "_META", metaSql());
            jdbc.update(meta);
        }

        // copy aux
        jdbc.update(createAuxDataSql(forTable));
        String auxSql = auxSql();
        if (auxSql != null) {
            String insertAux = "INSERT INTO %s %s".formatted(forTable + "_AUX", auxSql);
            jdbc.update(insertAux);
        }

        StopWatch.getInstance().printLog("  ingestData: load data for " + forTable);

        StopWatch.getInstance().printLog("%s:ingestData for %s".formatted(getName(), forTable));
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
        return dd.formatted(getSourceFile().getAbsolutePath());
    }

    protected String metaSql() { return null; }
    protected String auxSql() { return null; }

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
            this(null);
        }   // used by DbAdapterCreator only
        public Parquet(File dbFile) {
            super(dbFile, NAME, SUPPORTS);
        }

        @Override
        boolean canHandle(File dbFile) {
            boolean can = super.canHandle(dbFile);
            if (can) return true;
            sourceFile = dbFile;
            return canRead() > 0;
        }

        public DbAdapter create(File dbFile) {
            return canHandle(dbFile) ? new Parquet(dbFile) : null;
        }
//        protected String metaSql() {
//            return "select decode(key) as key, decode(value) as value, true as isKeyword from parquet_kv_metadata('%s')".formatted(getSourceFile().getAbsolutePath());
//        }

        String getSrcFileSql() {
            return "read_parquet('%s')".formatted(getSourceFile().getAbsolutePath());
        }
    }

    public static class Csv extends DuckDbReadable implements DbAdapterCreator{
        public static final String NAME = "csv";
        private static final List<String> SUPPORTS = List.of(NAME, "tsv");

        /**
         * used by DbAdapterCreator only
         */
        Csv() {
            this(null);
        }       // used by DbAdapterCreator only
        public Csv(File dbFile) { super(dbFile, NAME, SUPPORTS); }

        public DbAdapter create(File dbFile) {
            return canHandle(dbFile) ? new Csv(dbFile) : null;
        }

        boolean canHandle(File dbFile) {
            boolean can = super.canHandle(dbFile);
            if (can) return true;
            sourceFile = dbFile;
            return canRead() > 1;       // unless filename is in the supported list, file need to have at least 2 columns
        }

        String getSrcFileSql() {
            return "read_csv('%s', delim=',')".formatted(getSourceFile().getAbsolutePath());
        }
    }

    public static class Tsv extends DuckDbReadable implements DbAdapterCreator{
        public static final String NAME = "tsv";
        private static final List<String> SUPPORTS = List.of(NAME);

        Tsv() {
            this(null);
        }           // used by DbAdapterCreator only
        public Tsv(File dbFile) { super(dbFile, NAME, SUPPORTS); }

        public DbAdapter create(File dbFile) {
            return canHandle(dbFile) ? new Tsv(dbFile) : null;
        }

        List<String> getSupportedExts() {
            return  SUPPORTS;
        }

        public String getName() { return NAME; }

        boolean canHandle(File dbFile) {
            boolean can = super.canHandle(dbFile);
            if (can) return true;
            sourceFile = dbFile;
            return canRead() > 1;       // unless file extension is in the supported list, file need to have at least 2 columns
        }

        String getSrcFileSql() {
                return "read_csv('%s', delim='\\t')".formatted(getSourceFile().getAbsolutePath());
        }
    }

}
