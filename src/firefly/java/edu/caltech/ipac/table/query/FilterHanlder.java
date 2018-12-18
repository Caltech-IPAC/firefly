/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.table.io.BgIpacTableHandler;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.util.CollectionUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class FilterHanlder extends BgIpacTableHandler {
    private static Logger.LoggerImpl LOG = Logger.getLogger();
    private BufferedReader reader;
    private List<CollectionUtil.Filter<DataObject>> filters;
    private int cRowNum = -1;
    private boolean hasRowIdFilter = false;
    private int rowsFound;
    private List<DataGroup.Attribute> attributes;
    private List<DataType> headers;
    private DataGroup dg;
    private IpacTableDef tableDef;

    public FilterHanlder(File ofile, File source, CollectionUtil.Filter<DataObject>[] filters, TableServerRequest request) throws IOException {
        super(ofile, null, null, null, request);
        tableDef = IpacTableUtil.getMetaInfo(source);
        attributes = IpacTableUtil.createMetaFromColumns(tableDef.getKeywords(), tableDef.getCols().toArray(new DataType[0]));
        headers = tableDef.getCols();
        reader = new BufferedReader(new FileReader(source), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        this.filters = Arrays.asList(filters);
        if (filters != null) {
            for (CollectionUtil.Filter<DataObject> f : filters) {
                if (f.isRowIndexBased()) {
                    hasRowIdFilter = true;
                    break;
                }
            }
        }
        // if this file does not contain ROWID, add it.
        if (!headers.stream().anyMatch(dt -> dt.getKeyName().equals(DataGroup.ROW_IDX))) {
            headers.add(DataGroup.makeRowIdx());
        }
        dg = new DataGroup(null, headers);
    }

    public static void main(String[] args) {
        try {
            File in = new File(args[0]);
            CollectionUtil.Filter<DataObject> filter = DataGroupQueryStatement.parseFilter(args[1]);
            int prefetchSize = Integer.parseInt(args[2]);
            TableServerRequest request = new TableServerRequest("test-id");
            request.setPageSize(prefetchSize);

            CollectionUtil.Filter<DataObject> filters [] = new CollectionUtil.Filter[1];
            filters[1] = filter;
            IpacTableWriter.asyncSave(new FilterHanlder(new File(in.getParent(), in.getName() + ".out"), in, filters, request));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<DataType> getHeaders() {
        return headers;
    }

    @Override
    public List<DataGroup.Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public int getRowCount () {
        return rowsFound;
    }

    @Override
    public DataObject getNextRow() throws IOException {
        String line = null;
        try {
            DataObject next = null;
            boolean eof = false;
            while (next == null && !eof) {
                line = reader.readLine();
                eof = line == null;
                DataObject row = eof ? null:  IpacTableUtil.parseRow(dg, line, tableDef);
                if (row != null) {
                    int rowIdx = ++cRowNum;
                    if (hasRowIdFilter) {
                        rowIdx = row.getIntData(DataGroup.ROW_IDX);
                        rowIdx = rowIdx < 0 ? cRowNum : rowIdx;
                    }

                    if (CollectionUtil.matches(rowIdx, row, filters)) {
                        row.setDataElement(DataGroup.ROW_IDX, rowIdx);
                        ++rowsFound;
                        next = row;
                    }
                }
            }
            return next;
        } catch (Exception e) {
            LOG.error(e, "Unable to parse row:" + line);
            throw new IOException("Unable to parse row:" + line, e);
        }
    }
}
