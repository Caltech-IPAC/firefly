/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
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

import static edu.caltech.ipac.table.DataType.*;
import static edu.caltech.ipac.table.JsonTableUtil.toLinkInfos;
import static edu.caltech.ipac.table.TableMeta.*;
import static edu.caltech.ipac.util.StringUtils.*;

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
            ensureKey(attribs, col.getKeyName(), col.getTypeDesc(), TYPE_TAG);
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

            if (!col.isSortable())   ensureKey(attribs, col.getKeyName(), "false", SORTABLE_TAG);
            if (!col.isFilterable()) ensureKey(attribs, col.getKeyName(), "false", FILTERABLE_TAG);
            if (col.isFixed())       ensureKey(attribs, col.getKeyName(), "true", FIXED_TAG);

            ensureKey(attribs, col.getKeyName(), col.getSortByCols(), SORT_BY_TAG);
            ensureKey(attribs, col.getKeyName(), col.getEnumVals(), ENUM_VALS_TAG);
            ensureKey(attribs, col.getKeyName(), col.getPrecision(), PRECISION_TAG);
            ensureKey(attribs, col.getKeyName(), col.getUCD(), UCD_TAG);
            ensureKey(attribs, col.getKeyName(), col.getUType(), UTYPE_TAG);
            ensureKey(attribs, col.getKeyName(), col.getRef(), REF_TAG);
            ensureKey(attribs, col.getKeyName(), col.getMinValue(), MIN_VALUE_TAG);
            ensureKey(attribs, col.getKeyName(), col.getMaxValue(), MAX_VALUE_TAG);
            ensureKey(attribs, col.getKeyName(), col.getArraySize(), ARY_SIZE_TAG);
            ensureKey(attribs, col.getKeyName(), col.getCellRenderer(), CELL_RENDERER);
            if (col instanceof ParamInfo) {
                ensureKey(attribs, col.getKeyName(), ((ParamInfo)col).getStringValue(), VALUE_TAG);
            }

            List<LinkInfo> links = col.getLinkInfos();
            if (links != null && links.size() > 0) {
                String json = JSONValue.toJSONString(JsonTableUtil.toJsonLinkInfos(links));
                ensureKey(attribs, col.getKeyName(), json, LINKS_TAG);
            }
        }
        return attribs;
    }

    public static void ensureKey(List<DataGroup.Attribute> attribs, String name, String value, String tag) {
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
            consumeMeta(TYPE_TAG, meta, dt, (v, c) -> {
                c.setDataType(DataType.descToType(v));
                c.setTypeDesc(v);
            });
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
            consumeMeta(FIXED_TAG, meta, dt, (v, c) -> c.setFixed(StringUtils.getBoolean(v, false)));
            consumeMeta(SORT_BY_TAG, meta, dt, (v, c) -> c.setSortByCols(v));
            consumeMeta(ENUM_VALS_TAG, meta, dt, (v, c) -> c.setEnumVals(v));
            consumeMeta(UCD_TAG, meta, dt, (v, c) -> c.setUCD(v));
            consumeMeta(UTYPE_TAG, meta, dt, (v, c) -> c.setUType(v));
            consumeMeta(REF_TAG, meta, dt, (v, c) -> c.setRef(v));
            consumeMeta(MIN_VALUE_TAG, meta, dt, (v, c) -> c.setMinValue(v));
            consumeMeta(MAX_VALUE_TAG, meta, dt, (v, c) -> c.setMaxValue(v));
            consumeMeta(ARY_SIZE_TAG, meta, dt, (v, c) -> c.setArraySize(v));
            consumeMeta(CELL_RENDERER, meta, dt, (v, c) -> c.setCellRenderer(v));

            consumeMeta(LINKS_TAG, meta, dt, (json, c) -> applyIfNotEmpty(toLinkInfos(json), infos -> c.setLinkInfos(infos)));

            if (dt instanceof ParamInfo) {
                consumeMeta(VALUE_TAG, meta, dt, (v, c) -> ((ParamInfo)c).setValue(v));
            }

            consumeMeta(PRECISION_TAG, meta, dt, (v, c) -> {
                c.setPrecision(v);      // if precision is given as meta, use it because we want this to be the official method.  clear higher format precedences.
                c.setFormat(null);
                c.setFmtDisp(null);
            });
        }
    }

    private static void consumeMeta(String tag, TableMeta tableMeta, DataType col, BiConsumer<String, DataType> c) {
        if (tag.length() < 7) return;  // col.@.? --- that's why 6 is used as magic position
        // check backward-compatible convention..  i.e.  Label vs label, Width vs width
        String btag = tag.substring(0,6) + tag.substring(6,7).toUpperCase() + tag.substring(7);
        if (tag.equals(UNIT_TAG))       btag = "col.@.Unit";
        if (tag.equals(NULL_STR_TAG))   btag = "col.@.NullStr";
        doConsumeMeta(btag, tableMeta, col, c);

        doConsumeMeta(tag, tableMeta, col, c);          // do current now to override if both are given.
    }

    private static void doConsumeMeta(String tag, TableMeta tableMeta, DataType col, BiConsumer<String, DataType> c) {
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
            if (!CollectionUtil.exists(kw.getKey(), ignoreList)) {
                if ( !(kw.isComment() || (ignoreSysMeta && isSysMeta(kw.getKey()))) ) {
                    if (!kw.getKey().equals(DESC) )  writer.println(kw);    // ignore DESC.  we will handle it at the end.
                }
            }
        }
        // then write comments
        for (DataGroup.Attribute kw : attribs) {
            if (!CollectionUtil.exists(kw.getKey(), ignoreList)) {
                if (kw.isComment()) {
                    writer.println(kw.toString());
                }
            }
        }
        // now, write description if exists
        String desc = attribs.stream()
                .filter(a -> a.getKey() != null && a.getKey().equals(DESC))
                .findFirst()
                .map(a -> a.getValue())
                .orElse(null);

        if (!isEmpty(desc)) {
            List<String> descAsList = breakIntoMultipleLines(desc, 80);
            descAsList.forEach((s) -> writer.println("\\ %s".formatted(s)));
        }
    }

    public static void writeDescriptionAsComment(PrintWriter writer, String desc) {
        if (isEmpty(desc)) return;
        List<String> descAsList = breakIntoMultipleLines(desc, 80);
        descAsList.forEach((s) -> writer.println("\\ %s".formatted(s)));
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
            String cname; String cval;
            for (int idx = 0; idx < names.length; idx++) {
                cval = names[idx];
                cname = cval.trim();
                DataType dt = new DataType(cname, null);
                cols.add(dt);
                TableUtil.ParsedColInfo pci = tableDef.getParsedInfo(cname);
                pci.startIdx = cursor;
                pci.endIdx = cursor + cval.length();
                cursor = pci.endIdx + 1;
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
                String typeDesc = ipacToFireflyType(types[i].trim().toLowerCase());
                cols.get(i).setTypeDesc(typeDesc);
                cols.get(i).setDataType(DataType.descToType(typeDesc));
            }
        }
    }

    /**
     * Convert IPAC Table specific type string into one Firefly can understand.
     * @param typeDesc  IPAC Table data type string
     * @return Firefly data type string representation
     */
    private static String ipacToFireflyType(String typeDesc) {

        switch (typeDesc) {
            case "bool":
            case "b":
                return BOOLEAN;
            case "d":
            case "r":
            case "real":
                return DOUBLE;
            case "f":
                return FLOAT;
            case "i":
                return INTEGER;
            case "l":
                return LONG;
            case "c":
                return CHAR;
        }
        return typeDesc;
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

    public static void applyGuessLogic(DataType type, String val, TableUtil.ParsedColInfo pci) {

        if (!pci.formatChecked && val != null) {
            pci.formatChecked = guessFormatInfo(type, val);
        }

        if (!pci.htmlChecked) {
            // disable sorting if value is HTML, or unit is 'html'
            // this block should only be executed once, when formatInfo is not set.
            if (type.getDataType() == String.class ) {
                if (String.valueOf(type.getUnits()).equalsIgnoreCase("html") ||
                        val.matches("<[^>]+>.*")) {
                    type.setSortable(false);
                    type.setFilterable(false);
                }
            }
            pci.htmlChecked = true;
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
            type.setPrecision("E" + Math.max(matcher.group(1).length(), minPrecision));
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
     * DataObject and DataGroup member's functions rely heavily on HashMap.  Although HashMap.get is very performant,
     * but when calling it tens and hundreds of millions times will add up.
     * This function is used by DataGroupQuery only and will be removed when we remove DataGroupQuery.
     *
     * @param source
     * @param line
     * @param tableDef
     * @return
     */
    public static DataObject parseRow(DataGroup source, String line, IpacTableDef tableDef) {

        TableUtil.ParsedColInfo[] parsedColInfos = Arrays.stream(source.getDataDefinitions())
                                                    .map(dt -> tableDef.getParsedInfo(dt.getKeyName()))
                                                    .toArray(TableUtil.ParsedColInfo[]::new);

        Object[] data = parseRow(line, source.getDataDefinitions(), parsedColInfos);
        if (data != null) {
            DataObject row = new DataObject(source);
            row.setData(data);
            return row;
        } else {
            return null;
        }
    }

    /**
     * Return the parsed values of the given line for the given columns.
     * @param line      the line to data to parse.
     * @param cols      the columns to extract data from.  may be a subset of the total columns in the line.
     * @param parsedColInfos  the corresponding parsed info of the cols.
     * @return an array of converted values corresponding to the given cols.
     */
    public static Object[] parseRow(String line, DataType[] cols, TableUtil.ParsedColInfo[] parsedColInfos) {

        if (line==null) return null;
        int endOfLine = line.length();
        try {
            if (line.startsWith(" ") && line.trim().length() > 0) {

                Object[] arow = new Object[cols.length];
                for (int i =0; i <cols.length; i++) {
                    TableUtil.ParsedColInfo pci = parsedColInfos[i];
                    DataType dt = cols[i];
                    if (pci.startIdx > endOfLine) return null; // it's okay.  we'll take what's given and treat the rest as null.

                    // if ending spaces are missing... just ignore it.
                    int endoffset = Math.min(pci.endIdx, endOfLine);
                    String val = line.substring(pci.startIdx, endoffset).trim();

                    if (dt.getDataType() == null) {
                        IpacTableUtil.guessDataType(dt, val);
                    }
                    applyGuessLogic(dt, val, pci);
                    arow[i] = dt.convertStringToData(val);
                }
                return arow;
            } else if (line.trim().length() > 0 && !line.startsWith("\\") && !line.startsWith("|")) {
                throw new RuntimeException("Data row must start with a space.");
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException("line.length()="+line.length()+",line="+line);
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
    public static IpacTableDef getMetaInfo(File inf, Map<String, String> metaInfo) throws IOException {
        IpacTableDef tableDef = getMetaInfo(inf);
        if (metaInfo != null) {
            metaInfo.entrySet().forEach((e -> tableDef.setAttribute(e.getKey(), e.getValue())));
        }
        return tableDef;
    }

    public static IpacTableDef getMetaInfo(BufferedReader reader) throws IOException {
        return doGetMetaInfo(reader, null);
    }

    public static boolean isKnownType(Class type) {
        return (type == String.class  ||
                type == Double.class  ||
                type == Float.class   ||
                type == Integer.class ||
                type == Long.class
        );
    }

    public static Class mapToIpac(Class type) {
        if (type == Short.class)    return Integer.class;
        if (type == Boolean.class)  return String.class;
        if (type == Byte.class)     return Integer.class;

        return type;
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

