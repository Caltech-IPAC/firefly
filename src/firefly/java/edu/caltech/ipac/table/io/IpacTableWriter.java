/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.TableMeta;
import nom.tam.fits.Data;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static edu.caltech.ipac.table.IpacTableUtil.makeAttributes;

/**
 * This class handles an action to save a catalog in IPAC table format to local file.
 *
 * @author Xiuqin Wu
 * @see DataGroup
 * @see DataObject
 * @see DataType
 * @version $Id: IpacTableWriter.java,v 1.11 2012/08/10 20:58:28 tatianag Exp $
 */
public class IpacTableWriter {

    private static Logger.LoggerImpl LOG = Logger.getLogger();

    /**
     * save the catalogs to a file
     *
     * @param file the file name to be saved
     * @param dataGroup data group
     * @throws IOException on error
     */
    public static void save(File file, DataGroup dataGroup)
        throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(file), IpacTableUtil.FILE_IO_BUFFER_SIZE));
            save(out, dataGroup, false);
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     * @param dataGroup data group
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup)
            throws IOException {
        save(stream, dataGroup, false);
    }

    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     * @param dataGroup data group
     * @param ignoreSysMeta ignore meta use by system.
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup, boolean ignoreSysMeta)
            throws IOException {
        save(new PrintWriter(new BufferedOutputStream(stream, IpacTableUtil.FILE_IO_BUFFER_SIZE)), dataGroup, ignoreSysMeta);
    }

    private static void save(PrintWriter out, DataGroup dataGroup, boolean ignoreSysMeta) throws IOException {
        dataGroup.shrinkToFitData();
        List<DataType> headers = Arrays.asList(dataGroup.getDataDefinitions());
        int totalRow = dataGroup.size();

        if (ignoreSysMeta) {
            // this should return only visible columns
            headers = headers.stream()
                            .filter(dt -> IpacTableUtil.isVisible(dataGroup, dt)
                                    && !dt.getKeyName().equals(DataGroup.ROW_IDX)
                                    && !dt.getKeyName().equals(DataGroup.ROW_NUM))
                            .collect(Collectors.toList());
        }

        List<DataGroup.Attribute> attributes = IpacTableUtil.makeAttributes(dataGroup);  // add column info as attributes

        IpacTableUtil.writeAttributes(out, attributes, ignoreSysMeta);
        IpacTableUtil.writeHeader(out, headers);

        for (int i = 0; i < totalRow; i++) {
            IpacTableUtil.writeRow(out, headers, dataGroup.get(i));
        }
        out.flush();
    }

//====================================================================
//  async option to write in the background after
//====================================================================

    public static void asyncSave(IpacTableHandler handler) throws IOException {
        try {
            WriteTask task = new WriteTask(handler);
            Thread t = new Thread(task);
            t.start();
            handler.onStart();
            task.flush();
        } catch (InterruptedException e) {
            LOG.info("DataGroupWriter interrupted:" + e.getMessage());
        }
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

    public interface Handler {
        void onStart() throws InterruptedException;

        void onComplete();

        File getOutFile();

        List<DataType> getHeaders();

        List<DataGroup.Attribute> getAttributes();

        DataObject next() throws IOException;

    }

    private static class WriteTask implements Runnable {
        private static Logger.LoggerImpl LOG = Logger.getLogger();
        private IpacTableHandler handler;
        private PrintWriter writer = null;

        public WriteTask(IpacTableHandler handler) {
            this.handler = handler;
        }

        public void flush() {
            if (writer != null) writer.flush();
        }

        @Override
        public void run() {

            if (handler == null) {
                return;
            }

            StopWatch.getInstance().start("DataGroupWriter");

            try {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(handler.getOutFile()), IpacTableUtil.FILE_IO_BUFFER_SIZE));
                List<DataGroup.Attribute> attributes = handler.getAttributes();
                writeStatus(writer, DataGroupPart.State.INPROGRESS);
                IpacTableUtil.writeAttributes(writer, attributes, DataGroupPart.LOADING_STATUS);
                List<DataType> headers = handler.getHeaders();
                IpacTableUtil.writeHeader(writer, headers);
                writer.flush();
                DataObject row = handler.next();
                while (row != null) {
                    IpacTableUtil.writeRow(writer, headers, row);
                    row = handler.next();
                }
                StopWatch.getInstance().printLog("DataGroupWriter");
            } catch (IOException e) {
                LOG.error(e);
            } finally {
                if (writer != null) {
                    insertStatus(handler.getOutFile(), DataGroupPart.State.COMPLETED);
                    writer.flush();
                    writer.close();
                    writer = null;
                    handler.onComplete();
                }
            }
        }

    }

    public static class IpacTableHandler implements Handler {
        private final CountDownLatch waitOn = new CountDownLatch(1);
        private File ofile;
        private List<DataType> headers;
        private List<DataGroup.Attribute> attributes;
        private Iterator<DataObject> itr;
        private int rowCount = 0;

        public IpacTableHandler(File ofile, DataGroup source) {
            this(ofile, Arrays.asList(source.getDataDefinitions()), makeAttributes(source), source.iterator());
        }

        public IpacTableHandler(File ofile, List<DataType> headers, List<DataGroup.Attribute> attributes, Iterator<DataObject> itr) {
            this.ofile = ofile;
            this.headers = headers;
            this.attributes = IpacTableUtil.makeAttributes(attributes, headers.toArray(new DataType[0]));
            this.itr = itr;
        }

        public void onStart() throws InterruptedException {
            waitOn.await();
        }
        public void onComplete() {
            waitOn.countDown();
        }
        public File getOutFile() { return ofile; }
        public List<DataType> getHeaders() { return headers; }
        public List<DataGroup.Attribute> getAttributes() { return attributes; }
        public DataObject next() throws IOException {
            DataObject next = itr != null && itr.hasNext() ? itr.next() : null;
            if (next != null) rowCount++;
            return  next;
        }

        public int getRowCount() {
            return rowCount;
        }
    }


    // ============================================================
    // ----------------------------------- Private Methods ---------------------------------------
    // ============================================================

}

