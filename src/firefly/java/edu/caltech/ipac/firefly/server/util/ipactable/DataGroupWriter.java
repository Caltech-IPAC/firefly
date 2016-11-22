/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;


/**
 * Writes a DataGroup into an IPAC table file.  This class support different handlers.
 * IpacTableHandler: This default handler works like a typical synchronous method where the control is returned after
 * the whole file is written to disk.
 * BgIpacTableHandler:  add the ability to background the write process after a page of data is written.
 * FilterHandler: subclass from BgIpacTableHandler; in-place filtering with background option.
 */
public class DataGroupWriter {
    private static Logger.LoggerImpl LOG = Logger.getLogger();


    public static void write(File outFile, DataGroup source) throws IOException {
        write(new IpacTableHandler(outFile, source));
    }

    public static void write(IpacTableHandler handler) throws IOException {
        try {
            Thread t = new Thread(new WriteTask(handler));
            t.start();
            handler.onStart();
        } catch (InterruptedException e) {
            LOG.info("DataGroupWriter interrupted:" + e.getMessage());
        }
    }

    public static void writeStatus(PrintWriter writer, DataGroupPart.State status) {
        writer.println("\\" + DataGroupPart.LOADING_STATUS + " = " + status + "                           ");
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

        public WriteTask(IpacTableHandler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {

            if (handler == null) {
                return;
            }

            StopWatch.getInstance().start("DataGroupWriter");

            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(handler.getOutFile()), IpacTableUtil.FILE_IO_BUFFER_SIZE));
                List<DataGroup.Attribute> attributes = handler.getAttributes();
                writeStatus(writer, DataGroupPart.State.INPROGRESS);
                IpacTableUtil.writeAttributes(writer, attributes, DataGroupPart.LOADING_STATUS);
                List<DataType> headers = handler.getHeaders();
                IpacTableUtil.writeHeader(writer, headers);
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
            this(ofile, Arrays.asList(source.getDataDefinitions()), source.getKeywords(), source.iterator());
        }

        public IpacTableHandler(File ofile, List<DataType> headers, List<DataGroup.Attribute> attributes, Iterator<DataObject> itr) {
            this.ofile = ofile;
            this.headers = headers;
            this.attributes = attributes;
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

}


