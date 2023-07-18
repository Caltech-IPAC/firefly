/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.*;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static edu.caltech.ipac.table.DataType.typeToDesc;
import static edu.caltech.ipac.table.IpacTableUtil.*;
import static edu.caltech.ipac.table.TableMeta.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

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
            save(out, dataGroup);
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
    public static void save(OutputStream stream, DataGroup dataGroup) throws IOException {
        save(new PrintWriter(new BufferedOutputStream(stream, IpacTableUtil.FILE_IO_BUFFER_SIZE)), dataGroup);
    }

    private static void save(PrintWriter out, DataGroup dataGroup) throws IOException {


        shrinkToFitData(dataGroup);
        List<DataType> headers = Arrays.asList(dataGroup.getDataDefinitions());
        int totalRow = dataGroup.size();

        // this should return only visible columns
        headers = headers.stream()
                        .filter(dt -> IpacTableUtil.isVisible(dataGroup, dt)
                                && !dt.getKeyName().equals(DataGroup.ROW_IDX)
                                && !dt.getKeyName().equals(DataGroup.ROW_NUM))
                        .collect(Collectors.toList());

        // print table meta
        List<DataGroup.Attribute> attributes = dataGroup.getTableMeta().getKeywords();
        IpacTableUtil.writeAttributes(out, attributes, true);


        // due to format conversion, IPAC column headers may need to be fixed before writing
        List<DataType> modHeaders = headers.stream().map(DataType::newCopyOf).collect(Collectors.toList());

        // fix column with array type
        modHeaders.stream()
            .filter(dt -> dt.getArraySize() != null && dt.getDataType() != String.class)        // save original type and arraySize because ipac table will serialize it to string
            .forEach(dt -> {
                List<DataGroup.Attribute> atts = Arrays.asList(
                        TableMeta.makeAttribute(TableMeta.ARY_SIZE_TAG, dt.getKeyName(), dt.getArraySize()),
                        TableMeta.makeAttribute(TableMeta.TYPE_TAG, dt.getKeyName(), dt.getTypeDesc())
                );
                IpacTableUtil.writeAttributes(out, atts, false);

                dt.setDataType(String.class);
                dt.setTypeDesc("char");
            });

        // make sure derived columns are marked
        modHeaders.stream()
                .filter(dt -> dt.getDerivedFrom() != null)        // save derived column info
                .forEach(dt -> {
                    List<DataGroup.Attribute> atts = Arrays.asList(TableMeta.makeAttribute(DESC_TAG, dt.getKeyName(), dt.getDesc()));
                    IpacTableUtil.writeAttributes(out, atts, false);
                });

        // fix type if format changes value to another type
        modHeaders.stream()
            .filter(dt -> !isEmpty(dt.getPrecision()))
            .forEach( col -> {
                String prec = col.getPrecision().toUpperCase();
                if (prec.startsWith("HMS") || prec.startsWith("DMS")) {
                    col.setDataType(String.class);
                    col.setTypeDesc("char");
                }
            });

        // fix unsupported types
        modHeaders.stream()
                .filter(dt -> !isKnownType(dt.getDataType()))
                .forEach( dt -> {
                    List<DataGroup.Attribute> atts = Arrays.asList(TableMeta.makeAttribute(TYPE_TAG, dt.getKeyName(), dt.getTypeDesc()));
                    IpacTableUtil.writeAttributes(out, atts, false);
                    dt.setDataType(mapToIpac(dt.getDataType()));
                    dt.setTypeDesc(typeToDesc(dt.getDataType()));
                });

        // print column headers
        IpacTableUtil.writeHeader(out, modHeaders);

        for (int i = 0; i < totalRow; i++) {
            IpacTableUtil.writeRow(out, headers, dataGroup.get(i));
        }
        out.flush();
    }

    /**
     * this method will shrink the Column's width to fit the maximum's width of the data
     */
    private static void shrinkToFitData(DataGroup dataGroup) {
        for (DataType dt : dataGroup.getDataDefinitions()) {
            String[] headers = {dt.getKeyName(), dt.getTypeDesc(), dt.getUnits(), dt.getNullString()};
            int maxWidth = Arrays.stream(headers).mapToInt(s -> s == null ? 0 : s.length()).max().getAsInt();
            for (int i=0; i<dataGroup.size(); i++) {
                Object val = dataGroup.getData(dt.getKeyName(), i);
                int dWidth = val == null ? 0 : dt.format(val, true, false).length();
                if (dWidth > maxWidth) maxWidth = dWidth;
            }
            dt.setWidth(maxWidth);
        }
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
            this(ofile, Arrays.asList(source.getDataDefinitions()), createMetaFromColumns(source), source.iterator());
        }

        public IpacTableHandler(File ofile, List<DataType> headers, List<DataGroup.Attribute> attributes, Iterator<DataObject> itr) {
            this.ofile = ofile;
            this.headers = headers;
            this.attributes = IpacTableUtil.createMetaFromColumns(attributes, headers.toArray(new DataType[0]));
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

