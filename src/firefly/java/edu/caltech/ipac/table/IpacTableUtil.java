/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: Jun 25, 2009
 *
 * @author loi
 * @version $Id: IpacTableUtil.java,v 1.5 2012/11/08 23:56:49 tlau Exp $
 */
public class IpacTableUtil {

    public static final int FILE_IO_BUFFER_SIZE = FileUtil.BUFFER_SIZE;


    public static List<DataGroup.Attribute> makeAttributes(DataGroup dataGroup) {
        return makeAttributes(dataGroup.getKeywords(), dataGroup.getDataDefinitions());
    }
    /**
     * Returns the table's attributes in original sorted order, plus additional
     * attributes from column's info is present.
     */
    public static List<DataGroup.Attribute> makeAttributes(List<DataGroup.Attribute> attribs, DataType[] cols) {
        // add column's attributes as table meta
        for(DataType col : cols) {
            ensureKey(attribs, col.getKeyName(), col.getLabel(), TableMeta.LABEL_TAG);
            ensureKey(attribs, col.getKeyName(), col.getDesc(), TableMeta.DESC_TAG);
            ensureKey(attribs, col.getKeyName(), col.getFormat(), TableMeta.FORMAT_TAG);
            ensureKey(attribs, col.getKeyName(), col.getFmtDisp(), TableMeta.FORMAT_DISP_TAG);
            ensureKey(attribs, col.getKeyName(), col.getSortByCols(), TableMeta.SORT_BY_TAG);

            if (col.getVisibility() != DataType.Visibility.show) {
                ensureKey(attribs, col.getKeyName(), col.getVisibility().name(), TableMeta.VISI_TAG);
            }
            if (!col.isSortable()) {
                ensureKey(attribs, col.getKeyName(), "false", TableMeta.SORTABLE_TAG);
            }
            if (!col.isFilterable()) {
                ensureKey(attribs, col.getKeyName(), "false", TableMeta.FILTERABLE_TAG);
            }
            if(col.getWidth() > 0) {
                ensureKey(attribs, col.getKeyName(), String.valueOf(col.getWidth()), TableMeta.WIDTH_TAG);
            }
            if(col.getPrefWidth() >0) {
                ensureKey(attribs, col.getKeyName(), String.valueOf(col.getPrefWidth()), TableMeta.PREF_WIDTH_TAG);
            }
        }
        return attribs;
    }

    private static void ensureKey(List<DataGroup.Attribute> attribs, String name, String value, String tag) {
        if (!StringUtils.isEmpty(value)) {
            String key = TableMeta.makeAttribKey(tag, name);
            attribs.add(new DataGroup.Attribute(key, value));
        }
    }

    /**
     * update column information stored as attributes and then remove the attributes from the table meta.
     * @param table
     */
    public static void consumeColumnInfo(DataGroup table) {
        for (DataType dt : table.getDataDefinitions()) {
            ensureColumn(table.getTableMeta(), dt);
        }
    }

    private static void ensureColumn(TableMeta tableMeta, DataType col) {
        String key = TableMeta.makeAttribKey(TableMeta.LABEL_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setLabel(tableMeta.getAttribute(key));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.VISI_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setVisibility(DataType.Visibility.valueOf(tableMeta.getAttribute(key)));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.WIDTH_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setWidth(tableMeta.getIntMeta(key));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.PREF_WIDTH_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setPrefWidth(tableMeta.getIntMeta((key)));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.DESC_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setDesc(tableMeta.getAttribute(key));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.UNIT_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setUnits(tableMeta.getAttribute(key));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.FORMAT_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setFormat(tableMeta.getAttribute(key));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.FORMAT_DISP_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setFmtDisp(tableMeta.getAttribute(key));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.SORTABLE_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setSortable(Boolean.parseBoolean(tableMeta.getAttribute(key)));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.FILTERABLE_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setFilterable(Boolean.parseBoolean(tableMeta.getAttribute(key)));
            tableMeta.removeAttribute(key);
        }

        key = TableMeta.makeAttribKey(TableMeta.SORT_BY_TAG, col.getKeyName());
        if (tableMeta.contains(key)) {
            col.setSortByCols(tableMeta.getAttribute(key));
            tableMeta.removeAttribute(key);
        }

    }

    public static void writeAttributes(PrintWriter writer, Collection<DataGroup.Attribute> attribs, String... ignoreList) {
        writeAttributes(writer, attribs, false, ignoreList);
    }

    public static void writeAttributes(PrintWriter writer, Collection<DataGroup.Attribute> attribs, boolean ignoreSysMeta, String... ignoreList) {
        if (attribs == null) return;
        // write attributes first
        for (DataGroup.Attribute kw : attribs) {
            if (ignoreList == null || !CollectionUtil.exists(ignoreList, kw.getKey())) {
                if ( !(kw.isComment() || (ignoreSysMeta && isSysMeta(kw.getKey()))) ) {
                    writer.println(kw.toString());
                }
            }
        }
        // then write comments
        for (DataGroup.Attribute kw : attribs) {
            if (ignoreList == null || !CollectionUtil.exists(ignoreList, kw.getKey())) {
                if (kw.isComment()) {
                    writer.println(kw.toString());
                }
            }
        }
    }

    public static boolean isSysMeta(String m) {
        return m != null && (
                m.startsWith("col.") ||
                m.startsWith("Loading")
            );
    }

    public static void writeHeader(PrintWriter writer, List<DataType> headers) {
        writer.println("\\");
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.formatHeader(dt.getKeyName()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.formatHeader(dt.getTypeDesc()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        // check to see if we need to print the other headers.
        boolean hasMoreHeaders = false;
        for (DataType dt : headers) {
            if (dt.getUnits() != null || dt.getNullString() != null) {
                hasMoreHeaders = true;
                break;
            }
        }

        if (hasMoreHeaders) {
            for (int i = 0; i < headers.size(); i++) {
                DataType dt = headers.get(i);
                writer.print("|" + dt.formatHeader(dt.getUnits()));
                if (i == headers.size()-1) {
                    writer.print("|");
                }
            }
            writer.println();
            for (int i = 0; i < headers.size(); i++) {
                DataType dt = headers.get(i);
                writer.print("|" + dt.formatHeader(dt.getNullString()));
                if (i == headers.size()-1) {
                    writer.print("|");
                }
            }
            writer.println();
        }
    }

    public static void writeRow(PrintWriter writer, List<DataType> headers, DataObject row) {
        for (DataType dt : headers) {
            String v = row.getFixedFormatedData(dt);
            // when writing out the IPAC table.. if ROWID is given, and data is not found. use the getRowId() value instead.
            if (v == null && dt.getKeyName().equals(DataGroup.ROW_IDX)) {
                v = dt.formatData(row.getRowNum());
            }
            writer.print(" " + v);
        }
        writer.println(" ");
    }


    public static  TableDef createColumnDefs(String line) {
        TableDef tableDef = new TableDef();
        ArrayList<DataType> cols = new ArrayList<DataType>();
        if (line != null && line.startsWith("|")) {
            String[] names = parseHeadings(line.trim());
            int cursor = 1;
            String cname;
            for (int idx = 0; idx < names.length; idx++) {
                cname = names[idx];
                DataType dt = new DataType(cname.trim(), Object.class);
                cols.add(dt);
                tableDef.setColOffsets(idx, cursor);
                cursor += cname.length() + 1;
            }
        }
        tableDef.setCols(cols);
        return tableDef;
    }

    /**
     * return the all meta info for the given cname column if it exists.
     * @param metas  a list of meta from the table
     * @param cname  the column name to search
     * @return
     */
    public static List<DataGroup.Attribute> getAllColMeta(Collection<DataGroup.Attribute> metas, String cname) {
        return metas.stream()
                    .filter(m -> String.valueOf(m.getKey()).startsWith("col." + cname))
                    .collect(Collectors.toList());
    }

    public static  void setDataType(List<DataType> cols, String line) {
        if (line != null && line.startsWith("|")) {
            String[] types = parseHeadings(line.trim());
            for (int i = 0; i < types.length; i++) {
                String typeDesc = types[i].trim();
                cols.get(i).setTypeDesc(typeDesc);
                cols.get(i).setDataType(DataType.parseDataType(typeDesc));
            }
        }
    }

    public static  void setDataUnit(List<DataType> cols, String line) {
        if (line != null && line.startsWith("|")) {
            String[] units = parseHeadings(line.trim());
            for (int i = 0; i < units.length; i++) {
                cols.get(i).setUnits(units[i].trim());
            }
        }
    }

    public static  void setDataNullStr(List<DataType> cols, String line) {
        if (line != null && line.startsWith("|")) {
            String[] nullables = parseHeadings(line.trim());
            for(int i = 0; i < nullables.length; i++) {
                String nullString = nullables[i].trim();
                cols.get(i).setNullString(nullString);
            }
        }
    }
    public static void guessFormatInfo(DataType dataType, String value) {
       guessFormatInfo(dataType, value, 0);
    }

    public static void guessFormatInfo(DataType dataType, String value, int precision) {

        if (dataType.getFormat() != null ||
                dataType.getFormat() != null ||
                StringUtils.isEmpty(value)) {
            return;     // format exists
        }

        String formatStr = guessFormatStr(dataType, value, precision);
        if (formatStr != null) {
            dataType.setFormat(formatStr);
        }
    }

    public static void guessDataType(DataType type, String rval) {
        if (StringUtils.isEmpty(rval)) return;

        try {
            Long.parseLong(rval);
            type.setDataType(Long.class);
            return;
        }catch (Exception e){}

        try {
            Double.parseDouble(rval);
            type.setDataType(Double.class);
            return;
        }catch (Exception e){}

        type.setDataType(String.class);
    }

    /**
     *
     * @param source
     * @param line
     * @return
     */
    public static DataObject parseRow(DataGroup source, String line, TableDef tableDef) {
        if (line==null) return null;
        DataType[] headers = source.getDataDefinitions();
        int offset=0, endOfLine=0, endoffset=0;
        try {
            if (line.startsWith(" ") && line.trim().length() > 0) {
                DataObject row = new DataObject(source);
                endOfLine = line.length();

                DataType dt;
                for (int idx = 0; idx < headers.length; idx++) {
                    dt = headers[idx];
                    offset = tableDef.getColOffset(idx);
                    if (offset > endOfLine) {
                        // it's okay.  we'll take what's given and treat the rest as null.
                        break;
                    }
                    endoffset = idx < headers.length-1 ? tableDef.getColOffset(idx+1) : endOfLine;

                    // if ending spaces are missing... just ignore it.
                    if (endoffset > endOfLine) {
                        endoffset = endOfLine;
                    }

                    String val = line.substring(offset, endoffset).trim();
                    if (!dt.isKnownType()) {
                        IpacTableUtil.guessDataType(dt, val);
                    }

                    if (dt.getFormat() == null) {
                        IpacTableUtil.guessFormatInfo(dt, val);

                        // disable sorting if value is HTML, or unit is 'html'
                        // this block should only be executed once, when formatInfo is not set.
                        if (dt.getDataType().isAssignableFrom(String.class)) {
                            if (String.valueOf(dt.getUnits()).equalsIgnoreCase("html") ||
                                    val.matches("<[^>]+>.*")) {
                                dt.setSortable(false);
                                dt.setFilterable(false);
                            }
                        }
                    }

                    row.setDataElement(dt, dt.convertStringToData(val));
                    dt.ensureMaxDataWidth(val.length());

                    offset = endoffset;
                }
                return row;
            } else if (line.trim().length() > 0 && !line.startsWith("\\") && !line.startsWith("|")) {
                throw new RuntimeException("Data row must start with a space.");
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException("offset="+offset+",endoffset="+endoffset+
                    ",line.length()="+line.length()+",line="+line);
        }
        return null;
    }

    public static DataGroup.Attribute parseAttribute(String line) {
        return DataGroup.Attribute.parse(line);
    }

    public static Map<String, String> asMap(DataObject row) {
        HashMap<String, String> retval = new HashMap<String, String>();
        if (row != null) {
            for (DataType dt : row.getDataDefinitions()) {
                Object v = row.getDataElement(dt);
                if (v != null) {
                    retval.put(dt.getKeyName(), String.valueOf(v));
                }
            }
        }
        return retval;
    }

    public static TableDef getMetaInfo(File inf) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inf), FILE_IO_BUFFER_SIZE);
            return doGetMetaInfo(reader, inf);
        } finally {
            FileUtil.silentClose(reader);
        }
    }

    public static TableDef getMetaInfo(BufferedReader reader) throws IOException {
        return doGetMetaInfo(reader, null);
    }

    private static TableDef doGetMetaInfo(BufferedReader reader, File src) throws IOException {
        int nlchar = findLineSepLength(reader);

        List<DataGroup.Attribute> attribs = new ArrayList<>();
        int dataStartOffset = 0;

        int lineNum = 0;

        TableDef tableDef = new TableDef();
        String line = reader.readLine();
        // skip to column desc
        while (line != null) {
            lineNum++;
            if (line.length() == 0) {
                //  skip empty line
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
            } else if (line.startsWith("\\")) {
                DataGroup.Attribute attrib = parseAttribute(line);
                if (attrib != null) {
                    attribs.add(attrib);
                }
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
            } else if (line.startsWith("|")) {
                // column name
                tableDef = createColumnDefs(line);

                // column type
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
                if (line != null && line.startsWith("|")) {
                    setDataType(tableDef.getCols(), line);

                    // column unit
                    dataStartOffset += line.length() + nlchar;
                    line = reader.readLine();
                    if (line != null && line.startsWith("|")) {
                        setDataUnit(tableDef.getCols(), line);

                        // column null string identifier
                        dataStartOffset += line.length() + nlchar;
                        line = reader.readLine();
                        if (line != null && line.startsWith("|")) {
                            setDataNullStr(tableDef.getCols(), line);
                            dataStartOffset += line.length() + nlchar;
                            line = reader.readLine();
                        }
                    }
                }
            } else {
                // data row begins.
                tableDef.setLineWidth(line.length() + nlchar);
                if (src == null) {
                    // reading from stream, store the line read for later processing.
                    tableDef.setExtras(lineNum-1, line);
                }
                break;
            }
        }

        tableDef.setRowStartOffset(dataStartOffset);
        if (attribs.size() > 0) {
            tableDef.setKeywords(attribs);
        }
        if (tableDef.getCols() != null) {
            for(DataType dt: tableDef.getCols()) {
                String[] headers = {dt.getKeyName(), dt.getTypeDesc(), dt.getUnits(), dt.getNullString()};
                int hWidth = Arrays.stream(headers).mapToInt(s -> s == null ? 0 : s.length()).max().getAsInt();
                dt.ensureMaxDataWidth(hWidth);
            }
        }
        if (src != null) {
            long totalRow = tableDef.getLineWidth() == 0 ? 0 :
                    (src.length()+1 - (long) tableDef.getRowStartOffset()) / tableDef.getLineWidth();
            tableDef.setRowCount((int) totalRow);
        }
        if (src != null) {
            tableDef.setSource(src.getAbsolutePath());
        }

        return tableDef;
    }

    private static String guessFormatStr(DataType type, String val, int precision) {
        if (String.class.isAssignableFrom(type.getDataType())) {
            return "%s";
        }

        String formatStr = null;
        try {
            //first check to see if it's numeric
            double numval = Double.parseDouble(val);

            if (Double.isNaN(numval)) {
                return null;
            } else {
                if (val.matches(".+[e|E].+")) {
                    // scientific notation
                    String convStr = val.indexOf("E") >= 0 ? "E" : "e";
                    String[] ary = val.split("e|E");
                    if (ary.length == 2) {
                        int prec = ary[0].length() - ary[0].indexOf(".") - 1;
                        return "%." + prec + convStr;
                    }
                } else  if (val.indexOf(".") >= 0) {
                    // decimal format
                    int idx = val.indexOf(".");
                    int prec = val.length() - idx - 1;
                    return "%." + Math.max(prec, precision) + "f";
                } else {
                    boolean isFloat= (type.getDataType()==Float.class || type.getDataType()==Double.class);
                    formatStr = isFloat ?  "%.0f" : "%d";
                }
            }
        } catch (NumberFormatException e) {
            formatStr = "%s";
        }
        return formatStr;
    }

    //====================================================================
    //
    //====================================================================

    private static int findLineSepLength(Reader reader) throws IOException {
        reader.mark(FILE_IO_BUFFER_SIZE);
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

    private static String[] parseHeadings(String line) {
        return line.substring(1).split("\\|");
    }


    public static boolean isVisible(DataGroup dataGroup, DataType dt) {
        return dataGroup.getAttribute(TableMeta.makeAttribKey(TableMeta.VISI_TAG, dt.getKeyName()), DataType.Visibility.show.name()).equals(DataType.Visibility.show.name());
    }

}

