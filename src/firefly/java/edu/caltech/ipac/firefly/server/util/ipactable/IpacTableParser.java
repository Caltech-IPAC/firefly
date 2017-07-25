/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.IpacTableUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date: May 14, 2009
 *
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class IpacTableParser {

    public static DataGroup getSelectedData(File inf, Collection<Integer> indices, String... colNames) throws IOException {

        Arrays.sort(colNames);

        TableDef meta = IpacTableUtil.getMetaInfo(inf);

        List<DataType> selCols;
        if (colNames.length == 0) {
            selCols = meta.getCols().stream().map(DataType::copyOf).collect(Collectors.toList());
        } else {
            selCols = Arrays.stream(colNames).map( (s -> {
                if (s.equals(DataGroup.ROWID_NAME)) {
                    return new DataType(s, Long.class);
                } else {
                    DataType dt = meta.getColByName(s);
                    return dt != null ? dt.copyOf() : new DataType(s, String.class);
                }
            })).collect(Collectors.toList());
        }

        DataGroup results = new DataGroup(inf.getName(), selCols);
        results.setAttributes(meta.getAttributes());

        RandomAccessFile reader = new RandomAccessFile(inf,"r");
        try {
            ArrayList<Integer> sortedIndices = new ArrayList<Integer>(indices);
            Collections.sort(sortedIndices);

            DataGroup dg = new DataGroup("dummy", meta.getCols());
            boolean hasRowid = dg.containsKey(DataGroup.ROWID_NAME);
            long cidx = 0, pidx = -1;
            for(int idx : sortedIndices) {
                cidx = idx;
                advanceReaderToNextIdx(pidx, cidx, meta, reader);
                String line = reader.readLine();
                if (line != null) {
                    DataObject row = IpacTableUtil.parseRow(dg, line);
                    DataObject resRow = new DataObject(results);
                    for (String s : colNames) {
                        Object val = null;
                        if (s.equals(DataGroup.ROWID_NAME) && !hasRowid) {
                            val = cidx;
                        } else if (dg.containsKey(s)) {
                            val = row.getDataElement(s);
                        }
                        if (val != null) {
                            resRow.setDataElement(results.getDataDefintion(s), val);
                        }
                    }
                    results.add(resRow);
                }
                pidx = idx;
            }

        } finally {
            FileUtil.silentClose(reader);
        }

        return results;
    }


    public static MappedData getData(File inf, Collection<Integer> indices, String... colNames) throws IOException {

        MappedData results = new MappedData();

        Arrays.sort(colNames);

        TableDef meta = IpacTableUtil.getMetaInfo(inf);

        RandomAccessFile reader = new RandomAccessFile(inf,"r");
        try {
            ArrayList<Integer> sortedIndices = new ArrayList<Integer>(indices);
            Collections.sort(sortedIndices);

            DataGroup dg = new DataGroup("dummy", meta.getCols());
            boolean hasRowid = dg.containsKey(DataGroup.ROWID_NAME);
            long cidx = 0, pidx = -1;
            for(int idx : sortedIndices) {
                cidx = idx;
                advanceReaderToNextIdx(pidx, cidx, meta, reader);
                String line = reader.readLine();
                if (line != null) {
                    DataObject row = IpacTableUtil.parseRow(dg, line);
                    for (String s : colNames) {
                        Object val = null;
                        if (s.equals(DataGroup.ROWID_NAME) && !hasRowid) {
                            val = cidx;
                        } else if (dg.containsKey(s)) {
                            val = row.getDataElement(s);
                        }
                        if (val != null) {
                            results.put(idx, s, val);
                        }
                    }
                }
                pidx = idx;
            }

        } finally {
            reader.close();
        }

        return results;
    }


    public static DataGroupPart getData(File inf, int start, int rows) throws IOException {
        TableDef tableDef = IpacTableUtil.getMetaInfo(inf);

        DataGroup dg = new DataGroup(null, tableDef.getCols());
        dg.setRowIdxOffset(start);

        RandomAccessFile reader = new RandomAccessFile(inf, "r");
        long skip = ((long)start * (long)tableDef.getLineWidth()) + (long)tableDef.getRowStartOffset();
        int count = 0;
        try {
            reader.seek(skip);
            String line = reader.readLine();
            while (line != null && count < rows) {
                DataObject row = IpacTableUtil.parseRow(dg, line);
                if (row != null) {
                    dg.add(row);
                    count++;
                }
                line = reader.readLine();
            }
            dg.shrinkToFitData();
        } finally {
            reader.close();
        }

        // sync attributes in datagroup with tabledef.
        Map<String, DataGroup.Attribute> attribs = dg.getAttributes();
        if (attribs.size() > 0) {
            tableDef.addAttributes(attribs.values().toArray(new DataGroup.Attribute[attribs.size()]));
        }
        dg.setAttributes(tableDef.getAllAttributes());

        long totalRow = tableDef.getLineWidth() == 0 ? 0 :
                        (inf.length()+1 - tableDef.getRowStartOffset())/tableDef.getLineWidth();
        return new DataGroupPart(tableDef, dg, start, (int) totalRow);
    }

//====================================================================
//
//====================================================================


    public static class MappedData {
        private HashMap<String, Object> data = new HashMap<String, Object>();

        public void put(int idx, String colName, Object value) {
            this.data.put(makeKey(idx, colName), value);
        }

        public Object get(int idx, String colName) {
            return this.data.get(makeKey(idx, colName));
        }

        public Collection<Object> values() {
            return data.values();
        }

        /**
         * convenience method to return the values for the given column as a list of string
         * @param colName
         * @return
         */
        public List<String> getValues(String colName) {
            List<String> vals = new ArrayList<String>();
            for(Map.Entry<String, Object> s : data.entrySet()) {
                if (s.getKey().endsWith(colName)) {
                    String v = s.getValue() == null ? "" : s.getValue().toString();
                    vals.add(v);
                }
            }
            return vals;
        }

        private String makeKey(int idx, String colName) {
            return idx + "-" + colName;
        }

    }

    private static void advanceReaderToNextIdx(long pidx, long cidx, TableDef meta, RandomAccessFile reader) throws IOException {
        if (pidx == -1) {
            long skip = (cidx * (long)meta.getLineWidth()) + (long)meta.getRowStartOffset();
            reader.seek(skip);
        } else if (cidx - pidx == 1) {
            // next line.. no skipping
        } else if (cidx - pidx > 10) {
            long skip = (cidx * (long)meta.getLineWidth()) + (long)meta.getRowStartOffset();
            reader.seek(skip);
        } else {
            reader.skipBytes((int) ((cidx-pidx-1) * meta.getLineWidth()));
        }

    }
}
