package edu.caltech.ipac.firefly.server.db.spring.mapper;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private static int minPrefetchSize = DataGroupReader.MIN_PREFETCH_SIZE;
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
        writeHeader(template);
        ArrayList<DataType> cols = new ArrayList<DataType>(Arrays.asList(template.getDataDefinitions()));
        int count = 0;
        while(resultset.next()) {
            count++;
            writerRow(cols, resultset);
            if (count == prefetchSize) {
                processInBackground(cols, resultset);
                insertCompleteStatus(DataGroupPart.State.INPROGRESS);
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
                    insertCompleteStatus(DataGroupPart.State.COMPLETED);
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

    private void insertCompleteStatus(DataGroupPart.State state) {
        RandomAccessFile rdf = null;
        try {
             rdf = new RandomAccessFile(outf, "rw");
            String status = "\\" + DataGroupPart.LOADING_STATUS + " = " + state;
            rdf.writeBytes(status);
        } catch (FileNotFoundException e) {
            LOG.error(e, "Error openning output file:" + outf);
        } catch (IOException e) {
            LOG.error(e, "Error writing status to output file:" + outf);
        } finally {
            if (rdf != null) {
                try {
                    rdf.close();
                } catch (IOException e) {
                    LOG.warn(e, "Exception while closing output file:" + outf);
                }
            }
        }
    }

    public void writeAttributes(DataGroup.Attribute... attribs) {
        for (int i = 0; i < attribs.length; i++) {
            DataGroup.Attribute attrib = attribs[i];
            String type = attrib.hasType() ? attrib.getType() + " " : "";
            String line = "\\" + type + attrib.getKey() + " = " + attrib.formatValue(Locale.US);
            writer.println(line);
        }
    }

    public void writeHeader(DataGroup template) {

        Set<String> keys = template.getAttributeKeys();
        for (String key : keys) {
            writer.println("\\" + key + " = " + template.getAttribute(key).getValue());
        }

        DataType[] headers = template.getDataDefinitions();
        for (int i = 0; i < headers.length; i++) {
            DataType dt = headers[i];
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getKeyName()));
            if (i == headers.length-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.length; i++) {
            DataType dt = headers[i];
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getTypeDesc()));
            if (i == headers.length-1) {
                writer.print("|");
            }
        }
        writer.println();
    }

    public void writerRow(List<DataType> headers, ResultSet rs) {
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            try {
                Object obj = rs.getObject(dt.getKeyName());
                if (obj instanceof String && ((String)obj).indexOf("\r")>=0) {
                    obj = ((String)obj).replaceAll("\r", "");
                }
                writer.print(" " + dt.getFormatInfo().formatData(obj));
            } catch (SQLException e) {
                LOG.warn(e, "SQLException at col:" + headers.get(i).getKeyName());
                writer.print(" " + dt.getFormatInfo().formatData("#ERROR#"));
            }
        }
        writer.println();
    }

}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
