/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
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
import java.util.Map;


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
    private boolean hasRowIdFilter = false;
    private int rowsFound;
    private Map<String, String> meta;

    DataGroupFilter(File outf, File source, CollectionUtil.Filter<DataObject>[] filters, int prefetchSize, Map<String, String> meta) {
        this.outf = outf;
        this.source = source;
        this.filters = CollectionUtil.asList(filters);
        this.prefetchSize = Math.max(minPrefetchSize, prefetchSize);
        this.meta = meta;
        if (filters != null) {
            for (CollectionUtil.Filter<DataObject> f : filters) {
                if (f.isRowIndexBased()) {
                    hasRowIdFilter = true;
                    break;
                }
            }
        }

    }

    public static void filter(File outFile, File source, CollectionUtil.Filter<DataObject>[] filters, int prefetchSize) throws IOException {
        filter(outFile, source, filters, prefetchSize, null);
    }

    public static void filter(File outFile, File source, CollectionUtil.Filter<DataObject>[] filters, int prefetchSize, Map<String, String> meta) throws IOException {
        DataGroupFilter dgw = new DataGroupFilter(outFile, source, filters, prefetchSize, meta);
        try {
            dgw.start();
        } finally {
            dgw.close();
        }
    }


    void start() throws IOException {

        StopWatch.getInstance().start("DataGroupFilter");

        TableDef tableMeta = IpacTableUtil.getMetaInfo(source);
        List<DataGroup.Attribute> attributes = tableMeta.getAllAttributes();
        List<DataType> headers = tableMeta.getCols();

        writer = new PrintWriter(new BufferedWriter(new FileWriter(this.outf), IpacTableUtil.FILE_IO_BUFFER_SIZE));
        reader = new BufferedReader(new FileReader(source), IpacTableUtil.FILE_IO_BUFFER_SIZE);

        // if this file does not contain ROWID, add it.
        if (!DataGroup.containsKey(headers.toArray(new DataType[headers.size()]), DataGroup.ROWID_NAME)) {
            headers.add(DataGroup.ROWID);
            attributes.add(new DataGroup.Attribute("col." + DataGroup.ROWID_NAME + ".Visibility", "hidden"));
        }

        DataGroup dg = new DataGroup(null, headers);

        boolean needToWriteHeader = true;
        rowsFound = 0;
        cRowNum = -1;
        String line = reader.readLine();
        while (line != null) {
            try {
                DataObject row = IpacTableUtil.parseRow(dg, line, true, true);
                if (row != null) {
                    int rowIdx = ++cRowNum;
                    if (hasRowIdFilter) {
                        rowIdx = row.getRowIdx();
                        rowIdx = rowIdx < 0 ? cRowNum : rowIdx;
                    }

                    if (needToWriteHeader) {
                        needToWriteHeader = false;
                        DataGroupWriter.writeStatus(writer, DataGroupPart.State.INPROGRESS);
                        IpacTableUtil.writeAttributes(writer, attributes, DataGroupPart.LOADING_STATUS);
                        IpacTableUtil.writeHeader(writer, headers);
                    }

                    if (CollectionUtil.matches(rowIdx, row, filters)) {
                        row.setRowIdx(rowIdx);

                        IpacTableUtil.writeRow(writer, headers, row);
                        if (++rowsFound == prefetchSize) {
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
                            DataObject row = IpacTableUtil.parseRow(dg, line, true, true);
                            int rowIdx = ++cRowNum;
                            if (hasRowIdFilter) {
                                rowIdx = row.getRowIdx();
                                rowIdx = rowIdx < 0 ? cRowNum : rowIdx;
                            }
                            if (CollectionUtil.matches(rowIdx, row, filters)) {
                                row.setRowIdx(rowIdx);
                                IpacTableUtil.writeRow(writer, headers, row);
                                if (++rowsFound % 5000 == 0) {
                                    IpacTableUtil.sendLoadStatusEvents(meta, outf, rowsFound, DataGroupPart.State.INPROGRESS);
                                }
                            }
                            line = reader.readLine();
                        }

                    } catch (Exception e) {
                        LOG.error(e, "Unable to parse row:" + line);
                    } finally {
                        IpacTableUtil.sendLoadStatusEvents(meta, outf, rowsFound, DataGroupPart.State.COMPLETED);
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
            CollectionUtil.Filter<DataObject> filter = DataGroupQueryStatement.parseFilter(args[1]);
            int prefetchSize = Integer.parseInt(args[2]);

            CollectionUtil.Filter<DataObject> filters [] = new CollectionUtil.Filter[1];
            filters[1] = filter;
            DataGroupFilter.filter(new File(in.getParent(), in.getName() + ".out"), in,
                    filters, prefetchSize);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

