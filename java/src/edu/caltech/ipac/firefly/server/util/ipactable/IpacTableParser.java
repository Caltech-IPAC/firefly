package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: May 14, 2009
 *
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class IpacTableParser {

    public static MappedData getData(File inf, Collection<Integer> indices, String... colNames) throws IOException {

        MappedData results = new MappedData();

        Arrays.sort(colNames);

        DataGroupPart.TableDef meta = getMetaInfo(inf);

        RandomAccessFile reader = new RandomAccessFile(inf,"r");
        try {
            ArrayList<Integer> sortedIndices = new ArrayList<Integer>(indices);
            Collections.sort(sortedIndices);

            DataGroup dg = new DataGroup("dummy", meta.getCols());
            boolean hasRowid = dg.containsKey(DataGroup.ROWID_NAME);
            long cidx = 0, pidx = -1;
            for(int idx : sortedIndices) {
                cidx = idx;
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
        DataGroupPart.TableDef meta = getMetaInfo(inf);

        DataGroup dg = new DataGroup(null, meta.getCols());
        dg.setRowIdxOffset(start);
        dg.beginBulkUpdate();

        Map<String, DataGroup.Attribute> attribs = meta.getAttributes();
        for(DataGroup.Attribute a : attribs.values()) {
            dg.addAttributes(a);
        }

        RandomAccessFile reader = new RandomAccessFile(inf, "r");
        long skip = ((long)start * (long)meta.getLineWidth()) + (long)meta.getRowStartOffset();
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

        dg.endBulkUpdate();
        long totalRow = meta.getLineWidth() == 0 ? 0 :
                        (inf.length() - meta.getRowStartOffset())/meta.getLineWidth();
        return new DataGroupPart(meta, dg, start, (int) totalRow);
    }

    public static DataGroupPart.TableDef getMetaInfo(File inf) throws IOException {
        DataGroupPart.TableDef meta = new DataGroupPart.TableDef();
        meta.setSource(inf.getAbsolutePath());

            FileReader infReader = new FileReader(inf);
            BufferedReader reader = new BufferedReader(infReader, IpacTableUtil.FILE_IO_BUFFER_SIZE);

        try {
            int nlchar = findLineSepLength(reader);
            meta.setLineSepLength(nlchar);

        //====================================================================
        // parse attributes; identify dataStartOffset, dataLineLenght
            reader.mark(IpacTableUtil.FILE_IO_BUFFER_SIZE);
            List<DataGroup.Attribute> attribs = new ArrayList<DataGroup.Attribute>();
            int dataStartOffset = 0;
            String line = reader.readLine();
            while (line != null) {
                String nline = line.trim();
                if (nline.length() == 0) {
                    // blanks
                } else if (nline.startsWith("\\")) {
                    // attributes
                    DataGroup.Attribute attrib = IpacTableUtil.parseAttribute(line);
                    if (attrib != null) {
                        attribs.add(attrib);
                    }
                } else if (nline.startsWith("|")) {
                    // headers
                } else {
                    // data row begins.
                    meta.setLineWidth(line.length() + nlchar);
                    break;
                }
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
            }
            meta.setRowStartOffset(dataStartOffset);
            if (attribs != null && attribs.size() > 0) {
                meta.addAttribute(attribs.toArray(new DataGroup.Attribute[attribs.size()]));
            }
            reader.reset();
        //====================================================================

            List<DataType> cols = IpacTableUtil.readColumns(reader);
            for(DataType c : cols) {
                meta.addCols(c);
            }
            meta.setColCount(cols.size());

            long totalRow = meta.getLineWidth() == 0 ? 0 :
                            (inf.length() - (long)meta.getRowStartOffset())/meta.getLineWidth();
            meta.setRowCount((int) totalRow);
        } finally {
            infReader.close();
        }
        return meta;
    }

    private static int findLineSepLength(Reader reader) throws IOException {
        reader.mark(IpacTableUtil.FILE_IO_BUFFER_SIZE);
        int rval = 0;
        int pc = -1;
        int c = reader.read();
        while (c != -1) {
            if (c == '\n') {
                if (pc == '\r') {
                    rval = 2;
                    break;
                } else {
                    rval = 1;
                    break;
                }
            }
            pc = c;
            c = reader.read();
        }
        reader.reset();
        return rval;
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
         * convienvence method to return the values for the given column as a list of string
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
