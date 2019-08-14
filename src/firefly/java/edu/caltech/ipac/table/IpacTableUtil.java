/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.caltech.ipac.table.JsonTableUtil.toLinkInfos;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.table.TableMeta.*;

/**
 * Date: Jun 25, 2009
 *
 * @author loi
 * @version $Id: IpacTableUtil.java,v 1.5 2012/11/08 23:56:49 tlau Exp $
 */
public class IpacTableUtil {

    public static final int FILE_IO_BUFFER_SIZE = FileUtil.BUFFER_SIZE;
    private static final Pattern SCIENTIFIC = Pattern.compile("[+-]?\\d\\.(\\d+)[Ee].*");    // scientific format
    private static final Pattern FLOATING = Pattern.compile("[+-]?\\d*\\.(\\d+)");            // decimal format

    public static List<DataGroup.Attribute> createMetaFromColumns(DataGroup dataGroup) {
        return createMetaFromColumns(dataGroup.getAttributeList(), dataGroup.getDataDefinitions());
    }
    /**
     * Returns the table's attributes in original sorted order, plus additional
     * attributes from column's info if not already exits.
     * This is inverse of #consumeColumnInfo.  Changes to here should reflect there as well.
     */
    public static List<DataGroup.Attribute> createMetaFromColumns(List<DataGroup.Attribute> attribs, DataType[] cols) {
        // add column's attributes as table meta
        for(DataType col : cols) {
            ensureKey(attribs, col.getKeyName(), col.getLabel(), LABEL_TAG);
            if (col.getVisibility() != DataType.Visibility.show) {
                ensureKey(attribs, col.getKeyName(), col.getVisibility().name(), VISI_TAG);
            }
            if (col.getWidth() > 0) {
                ensureKey(attribs, col.getKeyName(), String.valueOf(col.getWidth()), WIDTH_TAG);
            }
            if (col.getPrefWidth() > 0) {
                ensureKey(attribs, col.getKeyName(), String.valueOf(col.getPrefWidth()), PREF_WIDTH_TAG);
            }
            ensureKey(attribs, col.getKeyName(), col.getDesc(), SDESC_TAG);
            ensureKey(attribs, col.getKeyName(), col.getDesc(), DESC_TAG);
            ensureKey(attribs, col.getKeyName(), col.getFormat(), FORMAT_TAG);
            ensureKey(attribs, col.getKeyName(), col.getFmtDisp(), FORMAT_DISP_TAG);
            if (!col.isSortable()) {
                ensureKey(attribs, col.getKeyName(), "false", SORTABLE_TAG);
            }
            if (!col.isFilterable()) {
                ensureKey(attribs, col.getKeyName(), "false", FILTERABLE_TAG);
            }
            ensureKey(attribs, col.getKeyName(), col.getSortByCols(), SORT_BY_TAG);
            ensureKey(attribs, col.getKeyName(), col.getEnumVals(), ENUM_VALS_TAG);
            ensureKey(attribs, col.getKeyName(), col.getPrecision(), PRECISION_TAG);
            ensureKey(attribs, col.getKeyName(), col.getUCD(), UCD_TAG);
            ensureKey(attribs, col.getKeyName(), col.getUType(), UTYPE_TAG);
            ensureKey(attribs, col.getKeyName(), col.getRef(), REF_TAG);
            ensureKey(attribs, col.getKeyName(), col.getMinValue(), MIN_VALUE_TAG);
            ensureKey(attribs, col.getKeyName(), col.getMaxValue(), MAX_VALUE_TAG);
            if (col instanceof ParamInfo) {
                ensureKey(attribs, col.getKeyName(), ((ParamInfo)col).getValue(), VALUE_TAG);
            }

            List<LinkInfo> links = col.getLinkInfos();
            if (links != null && links.size() > 0) {
                String json = JSONValue.toJSONString(JsonTableUtil.toJsonLinkInfos(links));
                ensureKey(attribs, col.getKeyName(), json, LINKS_TAG);
            }
        }
        return attribs;
    }

    private static void ensureKey(List<DataGroup.Attribute> attribs, String name, String value, String tag) {
        if (!isEmpty(value)) {
            String key = TableMeta.makeAttribKey(tag, name);
            attribs.add(new DataGroup.Attribute(key, value));
        }
    }

    /**
     * update column information stored as attributes and then remove the attributes from the table meta.
     * This is the inverse of #createMetaFromColumns.  Changes to here should reflect there as well.
     * @param table
     */
    public static void consumeColumnInfo(DataGroup table) {
        consumeColumnInfo(table.getDataDefinitions(), table.getTableMeta());
    }

    public static void consumeColumnInfo(DataType[] cols, TableMeta meta) {
        for (DataType dt : cols) {
            consumeMeta(LABEL_TAG, meta, dt, (v, c) -> c.setLabel(v));
            consumeMeta(VISI_TAG, meta, dt, (v, c) -> c.setVisibility(DataType.Visibility.valueOf(v)));
            consumeMeta(WIDTH_TAG, meta, dt, (v, c) -> c.setWidth(StringUtils.getInt(v, 0)));
            consumeMeta(PREF_WIDTH_TAG, meta, dt, (v, c) -> c.setPrefWidth(StringUtils.getInt(v, 0)));
            consumeMeta(DESC_TAG, meta, dt, (v, c) -> c.setDesc(v));
            consumeMeta(SDESC_TAG, meta, dt, (v, c) -> c.setDesc(v));
            consumeMeta(NULL_STR_TAG, meta, dt, (v, c) -> c.setNullString(v));
            consumeMeta(UNIT_TAG, meta, dt, (v, c) -> c.setUnits(v));
            consumeMeta(FORMAT_TAG, meta, dt, (v, c) -> c.setFormat(v));
            consumeMeta(FORMAT_DISP_TAG, meta, dt, (v, c) -> c.setFmtDisp(v));
            consumeMeta(SORTABLE_TAG, meta, dt, (v, c) -> c.setSortable(StringUtils.getBoolean(v, true)));
            consumeMeta(FILTERABLE_TAG, meta, dt, (v, c) -> c.setFilterable(StringUtils.getBoolean(v, true)));
            consumeMeta(SORT_BY_TAG, meta, dt, (v, c) -> c.setSortByCols(v));
            consumeMeta(ENUM_VALS_TAG, meta, dt, (v, c) -> c.setEnumVals(v));
            consumeMeta(PRECISION_TAG, meta, dt, (v, c) -> c.setPrecision(v));
            consumeMeta(UCD_TAG, meta, dt, (v, c) -> c.setUCD(v));
            consumeMeta(UTYPE_TAG, meta, dt, (v, c) -> c.setUType(v));
            consumeMeta(REF_TAG, meta, dt, (v, c) -> c.setRef(v));
            consumeMeta(MIN_VALUE_TAG, meta, dt, (v, c) -> c.setMinValue(v));
            consumeMeta(MAX_VALUE_TAG, meta, dt, (v, c) -> c.setMaxValue(v));

            consumeMeta(LINKS_TAG, meta, dt, (json, c) -> applyIfNotEmpty(toLinkInfos(json), infos -> c.setLinkInfos(infos)));

            if (dt instanceof ParamInfo)
                consumeMeta(VALUE_TAG, meta, dt, (v, c) -> ((ParamInfo)c).setValue(v));

        }
    }

    private static void consumeMeta(String tag, TableMeta tableMeta, DataType col, BiConsumer<String, DataType> c) {
        String key = TableMeta.makeAttribKey(tag, col.getKeyName());
        if (tableMeta.contains(key)) {
            c.accept(tableMeta.getAttribute(key), col);
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
                v = dt.formatFixedWidth(row.getRowNum());
            }
            writer.print(" " + v);
        }
        writer.println(" ");
    }


    public static IpacTableDef createColumnDefs(String line) {
        IpacTableDef tableDef = new IpacTableDef();
        ArrayList<DataType> cols = new ArrayList<DataType>();
        if (line != null && line.startsWith("|")) {
            String[] names = parseHeadings(line.trim());
            int cursor = 1;
            String cname;
            for (int idx = 0; idx < names.length; idx++) {
                cname = names[idx];
                DataType dt = new DataType(cname.trim(), null);
                dt.setNullString("null");       // defaults to 'null'
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
                cols.get(i).setDataType(DataType.descToType(typeDesc));
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

    public static void applyGuessLogic(DataType type, String val, TableUtil.CheckInfo chkInfo) {
        if (!chkInfo.formatChecked) {
            chkInfo.formatChecked = guessFormatInfo(type, val);
        }

        if (!chkInfo.htmlChecked) {
            // disable sorting if value is HTML, or unit is 'html'
            // this block should only be executed once, when formatInfo is not set.
            if (type.getDataType() == String.class ) {
                if (String.valueOf(type.getUnits()).equalsIgnoreCase("html") ||
                        val.matches("<[^>]+>.*")) {
                    type.setSortable(false);
                    type.setFilterable(false);
                }
            }
            chkInfo.htmlChecked = true;
        }
    }

    public static boolean guessFormatInfo(DataType type, String val) {
        return guessFormatInfo(type, val, 0);
    }

    /**
     * Given column info and the value, guess the precision for that column, then set it.
     * Precision format is documented here: edu.caltech.ipac.table.DataType#setPrecision(java.lang.String)
     * @param type  column info
     * @param val   the value to guess on.  This should be be null or empty.
     * @param minPrecision minimum precision
     * @return false if logic cannot be applied to the given value, like when val is null or null-string.
     */
    public static boolean guessFormatInfo(DataType type, String val, int minPrecision) {

        if (!isEmpty(type.getFormat()) || !isEmpty(type.getFmtDisp()) || !isEmpty(type.getPrecision())) return  true;   // format exists.. should not guess
        if (!type.isFloatingPoint()) return true;       // precision only applies to floating-point numbers.
        if (isEmpty(val) || (StringUtils.areEqual(val, type.getNullString())))  return false;       // null value.. skip

        Matcher matcher = SCIENTIFIC.matcher(val);
        if (matcher.matches()) {
            type.setPrecision("E" + Math.max(matcher.group(1).length() + 1, minPrecision));
        } else {
            matcher = FLOATING.matcher(val);
            if (matcher.matches()) {
                type.setPrecision("F" + Math.max(matcher.group(1).length(), minPrecision));
            }
        }

        return true;
    }

    /**
     * Guess the data type of this column based on the given value, and then set it.
     * Since we cannot determine if a number is int, long, float, or double without scanning the full table,
     * we'll just say it's double if numeric and string otherwise.
     * @param type  column definition
     * @param val   value to
     */
    public static void guessDataType(DataType type, String val) {

        if (isEmpty(val)) return;
        try {
            Double.parseDouble(val);
            type.setDataType(Double.class);
        } catch (Exception ex) {
            type.setDataType(String.class);
        }
    }

    /**
     *
     * @param source
     * @param line
     * @return
     */
    public static DataObject parseRow(DataGroup source, String line, IpacTableDef tableDef) {
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

                    TableUtil.CheckInfo checkInfo = tableDef.getColCheckInfos().getCheckInfo(dt.getKeyName());
                    applyGuessLogic(dt, val, checkInfo);

                    row.setDataElement(dt, dt.convertStringToData(val));

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

    public static IpacTableDef getMetaInfo(File inf) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inf), FILE_IO_BUFFER_SIZE);
            return doGetMetaInfo(reader, inf);
        } finally {
            FileUtil.silentClose(reader);
        }
    }

    public static IpacTableDef getMetaInfo(BufferedReader reader) throws IOException {
        return doGetMetaInfo(reader, null);
    }

    private static IpacTableDef doGetMetaInfo(BufferedReader reader, File src) throws IOException {
        int nlchar = findLineSepLength(reader);

        List<DataGroup.Attribute> attribs = new ArrayList<>();
        int dataStartOffset = 0;

        int lineNum = 0;

        IpacTableDef tableDef = new IpacTableDef();
        String line = reader.readLine();
        // skip to column desc
        while (line != null) {
            lineNum++;
            if (line.length() == 0) {
                //  skip empty line
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
            } else if (line.startsWith("\\")) {
                DataGroup.Attribute attrib = DataGroup.Attribute.parse(line);
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
        return dataGroup.getAttribute(TableMeta.makeAttribKey(VISI_TAG, dt.getKeyName()), DataType.Visibility.show.name()).equals(DataType.Visibility.show.name());
    }

}

