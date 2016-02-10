/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.*;


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
public class DataGroupWriter {
    private static int minPrefetchSize = DataGroupReader.MIN_PREFETCH_SIZE;
    private static Logger.LoggerImpl LOG = Logger.getLogger();

    private File outf;
    private PrintWriter writer;
    private DataGroup source;
    private boolean doclose = true;
    private int prefetchSize = minPrefetchSize;
    private int rowCount;
    private Map<String, String> meta;


    DataGroupWriter(File outf, DataGroup source, int prefetchSize, Map<String, String> meta) {
        this.outf = outf;
        this.source = source;
        this.prefetchSize = Math.max(minPrefetchSize, prefetchSize);
        this.meta = meta;
    }

    public static void write(File outFile, DataGroup source, int prefetchSize) throws IOException {
        write(outFile, source, prefetchSize, null);
    }

    public static void write(File outFile, DataGroup source, int prefetchSize, Map<String, String> meta) throws IOException {
        DataGroupWriter dgw = new DataGroupWriter(outFile, source, prefetchSize, meta);
        try {
            dgw.start();
        } finally {
            dgw.close();
        }
    }

    public void start() throws IOException {

        if (source == null) {
            return;
        }

        StopWatch.getInstance().start("DataGroupWriter");

        writer = new PrintWriter(new BufferedWriter(new FileWriter(this.outf), IpacTableUtil.FILE_IO_BUFFER_SIZE));

        // combine meta with file attributes
        ArrayList<DataGroup.Attribute> attributes = new ArrayList<>(source.getKeywords());
        if (meta == null) {
            meta = new HashMap<>(attributes.size());
        } else {
            for (String k : meta.keySet()) {
                attributes.add(new DataGroup.Attribute(k, meta.get(k)));
            }
        }
        for (DataGroup.Attribute at : attributes) {
            meta.put(at.getKey(), at.getValue());
        }


        writeStatus(writer, DataGroupPart.State.INPROGRESS);
        IpacTableUtil.writeAttributes(writer, attributes, DataGroupPart.LOADING_STATUS);
        List<DataType> headers = Arrays.asList(source.getDataDefinitions());
        IpacTableUtil.writeHeader(writer, headers);
        rowCount = 0;
        for(Iterator<DataObject> itr = source.iterator(); itr.hasNext(); rowCount++) {
            DataObject row = itr.next();
            IpacTableUtil.writeRow(writer, headers, row);
            if (rowCount == prefetchSize) {
                processInBackground(headers, itr);
                doclose = false;
                break;
            }
        }
        StopWatch.getInstance().printLog("DataGroupWriter");
        writer.flush();
    }

    private void close() {
        if (doclose) {
            if (writer != null) {
                insertStatus(outf, DataGroupPart.State.COMPLETED);
                writer.flush();
                writer.close();
                IpacTableUtil.sendLoadStatusEvents(meta, outf, rowCount, DataGroupPart.State.COMPLETED);
            }
        }
    }

    private void processInBackground(final List<DataType> headers, final Iterator<DataObject> itr) {
        Runnable r = new Runnable(){
                public void run() {
                    try {
                        while(itr.hasNext()) {
                            DataObject row = itr.next();
                            IpacTableUtil.writeRow(writer, headers, row);
                            if (++rowCount % 5000 == 0) {
                                IpacTableUtil.sendLoadStatusEvents(meta, outf, rowCount, DataGroupPart.State.INPROGRESS);
                            }
                        }
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

    public static void insertStatus(File outf, DataGroupPart.State state) {
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

    public static void writeStatus(PrintWriter writer, DataGroupPart.State status) {
        writer.println("\\" + DataGroupPart.LOADING_STATUS + " = " + status + "                           ");
    }

}

