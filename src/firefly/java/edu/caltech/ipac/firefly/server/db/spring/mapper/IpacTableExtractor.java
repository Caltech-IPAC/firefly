/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.spring.mapper;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.table.IpacTableUtil.createMetaFromColumns;

/**
 * This class query the database, and then write results out to the given file as an ipac table.
 * If the results exceed the number of maximum row set by property 'IpacTableExtractor.Max.Rows.Limit',
 * it will return to the caller with a status of 'INPROGRESS', and spin off another thread to
 * completely write out the rest of the results.
 * NOTE:  In the case when the results is very large, there is a good chance that this class is
 *        still writing to the file at the time when a read is requested.  This works fine on
 *        a Unix-like OS.  This may not work on Windows.
 *        If this is a concern, one solution is to use a temp file.
 *        Write the results into the temp file.  Rename the temp to the given file when it is done.
 *        In the case when background processing is needed,
 *        Copy the temp file containing partial results to the given file, continue to write
 *        out the rest of the results into the temp file.
 *        When it's completely finish, rename the temp file to the
 *        given file.
 *
 */
public class IpacTableExtractor {
    private static int minPrefetchSize = AppProperties.getIntProperty("IpacTable.min.prefetch.size", 500);
    private static Logger.LoggerImpl LOG = Logger.getLogger();
    public static final String LINE_SEP = System.getProperty("line.separator");


    private File outf;
    private PrintWriter writer;
    private DataSource datasource;
    private Connection conn;
    private PreparedStatement stmt;
    private ResultSet resultset;
    private boolean doclose = true;
    private int prefetchSize = minPrefetchSize;

    IpacTableExtractor(File outf, DataSource datasource, int prefetchSize) {
        this.outf = outf;
        this.datasource = datasource;
        this.prefetchSize = Math.max(minPrefetchSize, prefetchSize);
    }

    public static void query(DataSource datasource, File outFile, String sql, Object... params) {
        query(null, datasource, outFile, minPrefetchSize, sql, params);
    }

    public static void query(DataGroup template, DataSource datasource, File outFile, int prefetchSize, String sql, Object... params) {
        IpacTableExtractor ext = new IpacTableExtractor(outFile, datasource, prefetchSize);
        ext.doQuery(template, sql, params);
    }

    public void doQuery(DataGroup template, String sql, Object... params) {
        try {
            StopWatch.getInstance().start("IpacTableExtractor:: start query");
            open();
            stmt = conn.prepareStatement(sql);
            stmt.setFetchSize(200);
            if (params != null) {
                for(int i = 1; i <= params.length; i++) {
                    stmt.setObject(i, params[i-1]);
                }
            }
            LOG.info("Executing SQL query: " + sql,
                     "         Parameters: " + "{" + CollectionUtil.toString(params) + "}");
            resultset = stmt.executeQuery();
            extractData(template);
        } catch (SQLException e) {
            LOG.error("Error executing sql:" + sql+"\n"+e.getClass().getName()+": "+e.getMessage());
            throw new RuntimeException("Error executing sql", e);
        } finally {
            close();
        }
    }

    public void extractData(DataGroup template) throws SQLException, DataAccessException {

        writer.println("\\" + DataGroupPart.LOADING_STATUS + " = " + DataGroupPart.State.INPROGRESS + "                           ");
        StopWatch.getInstance().start("IpacTableExtractor");
        if (template == null) {
            template = new DataGroup("", DataGroupUtil.getExtraData(resultset.getMetaData()));
        }
        List<DataType> headers = Arrays.asList(template.getDataDefinitions());
        IpacTableUtil.writeAttributes(writer, createMetaFromColumns(template));
        IpacTableUtil.writeHeader(writer,headers);
        int count = 0;
        while(resultset.next()) {
            count++;
            writerRow(headers, resultset);
            if (count == prefetchSize) {
                processInBackground(headers, resultset);
                IpacTableWriter.insertStatus(outf, DataGroupPart.State.INPROGRESS);
                doclose = false;
                break;
            }
        }
        StopWatch.getInstance().printLog("IpacTableExtractor");
        writer.flush();
    }

    private void open() {
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(this.outf), IpacTableUtil.FILE_IO_BUFFER_SIZE));
            conn = datasource.getConnection();
        } catch (SQLException e) {
            throw new IllegalArgumentException("DataSource not valid");
        } catch (IOException e) {
            LOG.error(e, "Error while writing into output file:" + outf);
        }
    }

    private void close() {
        if (doclose) {
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
                if (writer != null) {
                    IpacTableWriter.insertStatus(outf, DataGroupPart.State.COMPLETED);
                    writer.flush();
                    writer.close();
                }
                StopWatch.getInstance().printLog("IpacTableExtractor:: start query");
            } catch (SQLException e) {
                LOG.warn(e, "Error while cleaning up db resources");
            }
        }
    }

    private void processInBackground(final List<DataType> headers, final ResultSet rs) {
        Runnable r = new Runnable(){
                public void run() {
                    try {
                        while(rs.next()) {
                            writerRow(headers, rs);
                        }
                    } catch (SQLException e) {
                        LOG.error(e, "Error while retrieving db data in background");
                    } finally {
                        doclose = true;
                        close();
                    }
                }
            };
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    public void writerRow(List<DataType> headers, ResultSet rs) {
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            try {
                Object obj = rs.getObject(dt.getKeyName());
                writer.print(" " + dt.format(obj, true, false));
            } catch (SQLException e) {
                LOG.warn(e, "SQLException at col:" + headers.get(i).getKeyName());
                writer.print(" " + dt.format("#ERROR#"));
            }
        }
        writer.println();
    }

}

