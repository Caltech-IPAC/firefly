/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.Downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public abstract class DuckDbReadable extends DuckDbAdapter {

    public DuckDbReadable(DbFileCreator dbFileCreator) { super(dbFileCreator); }
    DuckDbReadable(File dbFile) { super(dbFile); }

    public static TableUtil.Format guessFileFormat(String srcFile) {

        // based on file extension
        String fExt = FileUtil.getExtension(srcFile).toLowerCase();
        switch (fExt) {
            case Parquet.NAME, "parq" -> {
                return TableUtil.Format.PARQUET;
            }
            case Csv.NAME -> {
                return TableUtil.Format.CSV;
            }
            case Tsv.NAME -> {
                return TableUtil.Format.TSV;
            }
        }

        DataGroup info = getInfoOrNull(new Parquet(), srcFile);
        if (info != null && info.getDataDefinitions() != null) {
            return TableUtil.Format.PARQUET;
        }
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
     * @param treq the initiating table request
     * @return FileInfo on the dbFile
     * @throws DataAccessException
     */
    public FileInfo ingestDataDirectly(String source, TableServerRequest treq) throws DataAccessException {

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

        DataGroup tableMeta = getTableMeta(source, treq);     //collect all meta, then update the database with this information.
        if (tableMeta != null) {
            ddToDb(tableMeta, getDataTable());
            metaToDb(tableMeta, getDataTable());
            auxDataToDb(tableMeta, getDataTable());
        }

        StopWatch.getInstance().printLog("  ingestData: load data for " + forTable);

        StopWatch.getInstance().printLog("%s:ingestData for %s".formatted(getName(), forTable));
        return new FileInfo(getDbFile());
    }

    protected DataGroup getTableMeta(String source, TableServerRequest req) throws DataAccessException {
        return execQuery("SELECT * from %s LIMIT 0".formatted(sqlReadSource(source)), null);
    }

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

        public String getName() { return NAME;}
        String sqlReadSource(String srcFile) {
            return "read_parquet('%s')".formatted(srcFile);
        }

        @Override
        protected DataGroup getTableMeta(String source, TableServerRequest treq) throws DataAccessException {
            var jdbc = JdbcFactory.getSimpleTemplate(getDbInstance());
            try {
                var votable = jdbc.queryForObject(
                        "SELECT decode(value) FROM parquet_kv_metadata('%s') where key = 'IVOA.VOTable-Parquet.content'".formatted(source),
                        String.class);
                if (votable != null) {
                    return VoTableReader.voToDataGroups(new ByteArrayInputStream(votable.getBytes()), false)[0];
                }
            } catch (Exception ignored) {}        // ignored if it can't read
            return super.getTableMeta(source, treq);
        }

        public void export(TableServerRequest treq, OutputStream out) throws DataAccessException {
            String selectSql = buildSqlFrom(treq, getDataTable());
            try {
                DataGroup headers = getHeaders(getDataTable(), StringUtils.split(treq.getInclColumns(), ","));
                var voTable = new ByteArrayOutputStream();
                VoTableWriter.save(voTable, headers, TableUtil.Format.VO_TABLE);
                // FileUtil.writeStringToFile(File.createTempFile("votable-", ".vot", QueryUtil.getTempDir(treq)), voTable.toString(StandardCharsets.UTF_8));       // for testing only.  removed once done.
                String exportSql = """
                        COPY (%s) TO '%s' ( FORMAT PARQUET,
                            KV_METADATA {
                                'IVOA.VOTable-Parquet.type': 'Parquet-local-XML',
                                'IVOA.VOTable-Parquet.encoding': 'utf-8',
                                'IVOA.VOTable-Parquet.content': '%s',
                                'IVOA.VOTable-Parquet.version': '1.0'
                            }
                        );""";

                File tmp = File.createTempFile("duck-", "."+getName(), QueryUtil.getTempDir(treq));
                execUpdate(exportSql.formatted(selectSql, tmp.getAbsolutePath(), voTable.toString(StandardCharsets.UTF_8).replaceAll("'", "''")));
                if (tmp.canRead()) {
                    Downloader downloader = new Downloader(new DataInputStream(new FileInputStream(tmp)), out, tmp.length());
                    downloader.download();
                    tmp.delete();
                }
            } catch (Exception e) {
                throw new DataAccessException(e);
            }
        }
    }

    public static class Csv extends DuckDbReadable {
        public static final String NAME = "csv";

        public Csv(DbFileCreator dbFileCreator) { this(dbFileCreator.create(NAME)); }
        public Csv(File dbFile) { super(dbFile); }
        Csv() { this((File)null);}

        public String getName() { return NAME;}
        Character getDelimiter() { return ','; }

        String sqlReadSource(String srcFile) {
            return "read_csv('%s', delim='%c')".formatted(srcFile, getDelimiter());
        }

        public void export(TableServerRequest treq, OutputStream out) throws DataAccessException {
            String sql = buildSqlFrom(treq, getDataTable());
            try {
                File tmp = File.createTempFile("export-", "."+getName(), ServerContext.getTempWorkDir());
                execUpdate("COPY (%s) TO '%s' (HEADER, DELIMITER '%c')".formatted(sql, tmp.getAbsolutePath(), getDelimiter()));
                if (tmp.canRead()) {
                    Downloader downloader = new Downloader(new DataInputStream(new FileInputStream(tmp)), out, tmp.length());
                    downloader.download();
                    tmp.delete();
                }
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

        public String getName() { return NAME;}
        Character getDelimiter() { return '\t'; }
    }


}
