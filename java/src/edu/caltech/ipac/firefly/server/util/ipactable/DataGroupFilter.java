package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;


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
public class DataGroupFilter {
    private static int minPrefetchSize = DataGroupReader.MIN_PREFETCH_SIZE;
    private static Logger.LoggerImpl LOG = Logger.getLogger();

    private File outf;
    private PrintWriter writer;
    private File source;
    private BufferedReader reader;
    private List<CollectionUtil.Filter<DataObject>> filters;
    private boolean doclose = true;
    private int prefetchSize = minPrefetchSize;
    private int cRowNum;

    DataGroupFilter(File outf, File source, CollectionUtil.Filter<DataObject>[] filters, int prefetchSize) {
        this.outf = outf;
        this.source = source;
        this.filters = CollectionUtil.asList(filters);
        this.prefetchSize = Math.max(minPrefetchSize, prefetchSize);
    }

    public static void filter(File outFile, File source, CollectionUtil.Filter<DataObject>[] filters, int prefetchSize) throws IOException {
        DataGroupFilter dgw = new DataGroupFilter(outFile, source, filters, prefetchSize);
        try {
            dgw.start();
        } finally {
            dgw.close();
        }
    }


    void start() throws IOException {

        StopWatch.getInstance().start("DataGroupFilter");

        writer = new PrintWriter(new BufferedWriter(new FileWriter(this.outf), IpacTableUtil.FILE_IO_BUFFER_SIZE));
        reader = new BufferedReader(new FileReader(source), IpacTableUtil.FILE_IO_BUFFER_SIZE);

        List<DataGroup.Attribute> attributes = IpacTableUtil.readAttributes(reader);
        List<DataType> headers = IpacTableUtil.readColumns(reader);

        // if this file does not contain ROWID, add it.
        if (!DataGroup.containsKey(headers.toArray(new DataType[headers.size()]), DataGroup.ROWID_NAME)) {
            headers.add(DataGroup.ROWID);
            attributes.add(new DataGroup.Attribute("col." + DataGroup.ROWID_NAME + ".Visibility", "hidden"));
        }

        DataGroupWriter.writeStatus(writer, DataGroupPart.State.INPROGRESS);
        IpacTableUtil.writeAttributes(writer, attributes, DataGroupPart.LOADING_STATUS);
        IpacTableUtil.writeHeader(writer, headers);

        DataGroup dg = new DataGroup(null, headers);
        dg.beginBulkUpdate();

        int found = 0;
        cRowNum = -1;
        String line = reader.readLine();
        while (line != null) {
            try {
                DataObject row = IpacTableUtil.parseRow(dg, line);
                if (row != null) {
                    cRowNum++;
                    if (CollectionUtil.matches(cRowNum, row, filters)) {
                        row.setRowIdx(cRowNum);
                        IpacTableUtil.writeRow(writer, headers, row);
                        if (++found == prefetchSize) {
                            processInBackground(dg);
                            doclose = false;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error(e, "Unable to parse row:" + line);
                throw new IOException("Unable to parse row:" + line, e);
            }
            line = reader.readLine();
        }

        StopWatch.getInstance().printLog("DataGroupFilter");
        writer.flush();
    }

    private void close() {
        if (doclose) {
            if (writer != null) {
                DataGroupWriter.insertStatus(outf, DataGroupPart.State.COMPLETED);
                writer.flush();
                writer.close();
            }
        }
    }

    private void processInBackground(final DataGroup dg) {
        Runnable r = new Runnable(){
                public void run() {
                    String line="";
                    try {
                        List<DataType> headers = Arrays.asList(dg.getDataDefinitions());
                        line = reader.readLine();
                        while (line != null) {
                            DataObject row = IpacTableUtil.parseRow(dg, line);
                            cRowNum++;
                            if (CollectionUtil.matches(cRowNum, row, filters)) {
                                row.setRowIdx(cRowNum);
                                IpacTableUtil.writeRow(writer, headers, row);
                            }
                            line = reader.readLine();
                        }

                    } catch (Exception e) {
                        LOG.error(e, "Unable to parse row:" + line);
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

    public static void main(String[] args) {
        try {
            File in = new File(args[0]);
            DataGroupQuery.DataFilter filter = DataGroupQueryStatement.parseFilter(args[1]);
            int prefetchSize = Integer.parseInt(args[2]);

            DataGroupFilter.filter(new File(in.getParent(), in.getName() + ".out"), in,
                    new DataGroupQuery.DataFilter[]{filter}, prefetchSize);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
