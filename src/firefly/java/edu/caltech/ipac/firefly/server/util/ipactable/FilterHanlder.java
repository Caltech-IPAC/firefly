/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.firefly.data.TableServerRequest;
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

    public FilterHanlder(File ofile, File source, CollectionUtil.Filter<DataObject>[] filters, TableServerRequest request) throws IOException {
        super(ofile, null, null, null, request);
        TableDef tableMeta = IpacTableUtil.getMetaInfo(source);
        attributes = tableMeta.getAllAttributes();
        headers = tableMeta.getCols();
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
        if (!DataGroup.containsKey(headers.toArray(new DataType[headers.size()]), DataGroup.ROWID_NAME)) {
            headers.add(DataGroup.ROWID);
            attributes.add(new DataGroup.Attribute("col." + DataGroup.ROWID_NAME + ".Visibility", "hidden"));
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
            DataGroupWriter.write(new FilterHanlder(new File(in.getParent(), in.getName() + ".out"), in, filters, request));
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
                DataObject row = eof ? null:  IpacTableUtil.parseRow(dg, line, true, true);
                if (row != null) {
                    int rowIdx = ++cRowNum;
                    if (hasRowIdFilter) {
                        rowIdx = row.getRowIdx();
                        rowIdx = rowIdx < 0 ? cRowNum : rowIdx;
                    }

                    if (CollectionUtil.matches(rowIdx, row, filters)) {
                        row.setRowIdx(rowIdx);
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
