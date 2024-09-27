/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableUtil;

import java.io.File;
import java.io.OutputStream;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public abstract class DuckDbReadable extends DuckDbAdapter {

    public DuckDbReadable(DbFileCreator dbFileCreator) { super(dbFileCreator); }
    DuckDbReadable(File dbFile) { super(dbFile); }

    public static TableUtil.Format guessFileFormat(String srcFile) {
        DataGroup info = getInfoOrNull(new Parquet(), srcFile);
        if (info != null && info.getDataDefinitions() != null) {
            return TableUtil.Format.PARQUET;
        }

        DataGroup csv = getInfoOrNull(new Csv(), srcFile);
        DataGroup tsv = getInfoOrNull(new Tsv(), srcFile);

        if (csv != null && tsv != null) {
            return csv.getDataDefinitions().length > tsv.getDataDefinitions().length ? TableUtil.Format.CSV : TableUtil.Format.TSV;
        }

        if (csv != null) return TableUtil.Format.CSV;
        if (tsv != null) return TableUtil.Format.TSV;
        return null;
    }
    private static DataGroup getInfoOrNull(DuckDbReadable duckReadable, String srcFile) {
        try {
            return duckReadable.getInfo(srcFile);
        } catch (Exception ignored) {
            return null;
        }
    }

    String sqlReadSource(String srcFile) {
        return "'%s'".formatted(srcFile);
    }

    /**
     * @param format
     * @return returns a DuckDbReadable that can read this format. It is not attached to a dbFile and therefore will not persist.
     * @throws DataAccessException
     */
    public static DuckDbReadable getDetachedAdapter(TableUtil.Format format) throws DataAccessException {
        return castInto(format, null);
    }

    public static DuckDbReadable castInto(TableUtil.Format format, DuckDbAdapter dbAdapter) throws DataAccessException {
        File dbFile = dbAdapter == null ? null : dbAdapter.getDbFile();
        return   switch (format) {
            case TSV -> new Tsv(dbFile);
            case CSV -> new Csv(dbFile);
            case PARQUET -> new Parquet(dbFile);
            default -> null;
        };
    }

    public static DataGroup getInfo(TableUtil.Format format, String source) throws DataAccessException {
        var adapter = getDetachedAdapter(format);
        return adapter == null ? null : adapter.getInfo(source);
    }

    public DataGroup getInfo(String source) throws DataAccessException {
        StopWatch.getInstance().start("getInfo: " + source);
        String readSource = sqlReadSource(source);
        int count = JdbcFactory.getSimpleTemplate(getDbInstance()).queryForInt("SELECT count(*) from %s".formatted(readSource));
        DataGroup table = execQuery("SELECT * from %s LIMIT 0".formatted(readSource), null);
        table.setSize(count);
        StopWatch.getInstance().printLog("getInfo: " + readSource);
        return table;
    }

    /**
     * Ingest data directly from a source file.  This file can be local or remote.
     * @param source can be a local file path or a URL
     * @return FileInfo on the dbFile
     * @throws DataAccessException
     */
    public FileInfo ingestDataDirectly(String source) throws DataAccessException {

        String forTable = getDataTable();
        String sqlReadSource = sqlReadSource(source);

        StopWatch.getInstance().start("%s:ingestData for %s".formatted(getName(), forTable));

        // import data directly from source file.
        // no need to get data from dataGroupSupplier.  query the data directly from the dbFile
        var jdbc = JdbcFactory.getSimpleTemplate(getDbInstance());

        StopWatch.getInstance().start("  ingestData: load data for " + forTable);

        // create data table
        String dataSqlWithIdx = "select b.*, (%s - 1) as %s, (%s - 1) as %s from %s as b".formatted(rowNumSql(), DataGroup.ROW_IDX, rowNumSql(), DataGroup.ROW_NUM, sqlReadSource);
        String sql = createTableFromSelect(forTable, dataSqlWithIdx);
        jdbc.update(sql);

        // copy dd
        jdbc.update(createDDSql(forTable));
        String insert = "INSERT INTO %s %s".formatted(forTable + "_DD", ddSql(sqlReadSource));
        jdbc.update(insert);


        // create META table
        jdbc.update(createMetaSql(forTable));
        String metaSql = metaSql(sqlReadSource);
        if (metaSql != null) {
            String meta = "INSERT INTO %s %s".formatted(forTable + "_META", metaSql(sqlReadSource));
            jdbc.update(meta);
        }

        // copy aux
        jdbc.update(createAuxDataSql(forTable));
        String auxSql = auxSql(sqlReadSource);
        if (auxSql != null) {
            String insertAux = "INSERT INTO %s %s".formatted(forTable + "_AUX", auxSql);
            jdbc.update(insertAux);
        }

        StopWatch.getInstance().printLog("  ingestData: load data for " + forTable);

        StopWatch.getInstance().printLog("%s:ingestData for %s".formatted(getName(), forTable));
        return new FileInfo(getDbFile());
    };

    protected String ddSql(String sqlReadSource) {
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
           FROM (DESCRIBE select * from %s)
           """;
        return dd.formatted(sqlReadSource);
    }

    protected String metaSql(String sqlReadSource) { return null; }
    protected String auxSql(String sqlReadSource) { return null; }

    public void export(TableServerRequest treq, OutputStream out) throws DataAccessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

//====================================================================
//  Supported file types
//====================================================================

    public static class Parquet extends DuckDbReadable {
        public static final String NAME = "parquet";

        public Parquet(DbFileCreator dbFileCreator) { this(dbFileCreator.create(NAME)); }
        public Parquet(File dbFile) { super(dbFile); }
        Parquet() { this((File)null);}

//        protected String metaSql() {
//            return "select decode(key) as key, decode(value) as value, true as isKeyword from parquet_kv_metadata('%s')".formatted(getSourceFile().getAbsolutePath());
//        }

        String sqlReadSource(String srcFile) {
            return "read_parquet('%s')".formatted(srcFile);
        }
    }

    public static class Csv extends DuckDbReadable {
        public static final String NAME = "csv";

        public Csv(DbFileCreator dbFileCreator) { this(dbFileCreator.create(NAME)); }
        public Csv(File dbFile) { super(dbFile); }
        Csv() { this((File)null);}

        Character getDelimiter() { return ','; }

        String sqlReadSource(String srcFile) {
            return "read_csv('%s', delim='%c')".formatted(srcFile, getDelimiter());
        }

        public void export(TableServerRequest treq, OutputStream out) throws DataAccessException {
            String sql = buildSqlFrom(treq, getDataTable());
            try {
                File tmp = File.createTempFile("duck-", ".csv", ServerContext.getTempWorkDir());
                execUpdate("COPY (%s) TO '%s' (HEADER, DELIMITER '%c')".formatted(sql, tmp.getAbsolutePath(), getDelimiter()));
            } catch (Exception e) {
                throw new DataAccessException(e);
            }
        }
    }

    public static class Tsv extends Csv {
        public static final String NAME = "tsv";

        public Tsv(DbFileCreator dbFileCreator) { this(dbFileCreator.create(NAME)); }
        public Tsv(File dbFile) { super(dbFile); }
        Tsv() { this((File)null);}

        Character getDelimiter() { return '\t'; }
    }


}
