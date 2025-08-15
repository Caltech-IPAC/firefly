/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.JsonToDataGroup;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FormatUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static edu.caltech.ipac.util.StringUtils.isEmpty;


/**
 * Date: May 14, 2009
 *
 * @author loi
 * @version $Id: DataGroupReader.java,v 1.13 2012/11/05 18:59:59 loi Exp $
 */
public class TableUtil {

    private static final String[] regStartWith= new String [] {
          "circle", "box", "diamond", "cross", "x", "arrow", "annulus", "point", "polygon",
            "j2000", "galactic", "image", "physical", "global", "boxcircle"};

    public static DataGroup readAnyFormat(File inf) throws IOException {
        return readAnyFormat(inf, 0);
    }

    public static DataGroup readAnyFormat(File inf, int tableIndex) throws IOException {
        return readAnyFormat(inf, tableIndex, null);
    }

    public static DataGroup readAnyFormat(File inf, int tableIndex, TableServerRequest request) throws IOException {
        FormatUtil.Format format = FormatUtil.detect(inf);
        if (format == FormatUtil.Format.IPACTABLE) {
            return IpacTableReader.read(inf, request);
        } else if (format == FormatUtil.Format.VO_TABLE) {
            DataGroup[] tables = VoTableReader.voToDataGroups(inf.getAbsolutePath(), request, tableIndex);
            if (tables.length > 0) {
                return tables[0];
            } else return null;
        } else if (format == FormatUtil.Format.CSV || format == FormatUtil.Format.TSV) {
            return DsvTableIO.parse(inf, format, request);
        } else if (format == FormatUtil.Format.FITS ) {
            try {
                // Switch to the new function:
                return FITSTableReader.convertFitsToDataGroup(inf.getAbsolutePath(), request, tableIndex);
            } catch (Exception e) {
                throw new IOException("Unable to read FITS file:" + inf, e);
            }
        } else if (format == FormatUtil.Format.JSON) {
            return JsonToDataGroup.parse(inf, request);
        } else {
            throw new IOException("Unsupported format, file:" + inf);
        }
    }


    public static DataGroupPart getData(File inf, int start, int rows) throws IOException {
        IpacTableDef tableDef = IpacTableUtil.getMetaInfo(inf);

        DataGroup dg = new DataGroup(null, tableDef.getCols());

        RandomAccessFile reader = new RandomAccessFile(inf, "r");
        long skip = ((long)start * (long)tableDef.getLineWidth()) + (long)tableDef.getRowStartOffset();
        int count = 0;
        TableUtil.ParsedColInfo[] parsedColInfos = Arrays.stream(dg.getDataDefinitions())
                                                        .map(dt -> tableDef.getParsedInfo(dt.getKeyName()))
                                                        .toArray(TableUtil.ParsedColInfo[]::new);
        try {
            reader.seek(skip);
            String line = reader.readLine();
            while (line != null && count < rows) {
                Object[] row = IpacTableUtil.parseRow(line, dg.getDataDefinitions(), parsedColInfos);
                if (row != null) {
                    dg.add(row);
                    count++;
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        dg.getTableMeta().setKeywords(tableDef.getKeywords());

        long totalRow = tableDef.getLineWidth() == 0 ? 0 :
                        (inf.length()+1 - tableDef.getRowStartOffset())/tableDef.getLineWidth();
        return new DataGroupPart(dg, start, (int) totalRow);
    }

    /**
     * takes all the TableMeta that is column's related and use it to set column's properties.
     * remove the TableMeta that was used.
     * @param dg
     * @param treq  if not null, merge META-INFO from this request into TableMeta before consuming
     */
    public static void consumeColumnMeta(DataGroup dg, TableServerRequest treq) {
        if (treq != null && treq.getMeta() != null) {
            treq.getMeta().forEach((k,v) -> {
                if (k.equals(TableServerRequest.TITLE)) {
                    dg.setTitle(v);
                }else if (k.equals(TableMeta.TBL_RESOURCES)) {
                    dg.setResourceInfos(JsonTableUtil.toResourceInfos(v));
                }else if (k.equals(TableMeta.TBL_LINKS)) {
                    dg.setLinkInfos(JsonTableUtil.toLinkInfos(v));
                }else if (k.equals(TableMeta.TBL_GROUPS)) {
                    dg.setGroupInfos(JsonTableUtil.toGroupInfos(v));
                } else {
                    dg.getTableMeta().setAttribute(k, v);
                }
            });
        }
        IpacTableUtil.consumeColumnInfo(dg);
    }

    /**
     * converts table headers information into a tabular displayable table
     * @param idx
     * @param meta
     * @return
     */
    public static DataGroup getDetails(int idx, IpacTableDef meta) {
        DataType[] cols = new DataType[] {
                new DataType("name", String.class),
                new DataType("type", String.class),
                new DataType("unit", String.class),
                new DataType("desc", String.class),
                new DataType("UCD", String.class)
        };
        DataGroup dg = new DataGroup("Header of extension with index " + idx, cols);
        meta.getAttributeList().forEach(a -> dg.getTableMeta().addKeyword(a.getKey(), a.getValue()));
        for (DataType col : meta.getCols() ) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], col.getKeyName());
            row.setDataElement(cols[1], col.getTypeLabel());
            row.setDataElement(cols[2], col.getUnits());
            row.setDataElement(cols[3], col.getDesc());
            row.setDataElement(cols[4], col.getUCD());
            dg.add(row);
        }
        return dg;
    }

    public static List foldAry(DataType dt, Object value) {

        List aryList = aryToList(value);
        int[] shape = dt.getShape();
        if (shape.length > 1) {
            for(int i = 0; i < shape.length-1; i++) {
                aryList = foldList(aryList, shape[i]);
            }
        }
        return aryList;
    }

    private static List foldList(List source, int size) {
        size = size <= 0 ? Integer.MAX_VALUE : size;
        List rval = new ArrayList();
        for(int i = 0; i < source.size(); ) {
            int end = Math.min(i + size, source.size());
            rval.add(source.subList(i, end));
            i = end;
        }
        return rval;
    }

    /**
     * Simple util function to convert any array to a list, including primitives
     * @param val   value to format
     * @return
     */
    static List aryToList(Object val) {
        if (val.getClass().isArray()) {
            if (val instanceof Object[])
                return Arrays.stream((Object[]) val).collect(Collectors.toList());
            else {  // we can't cast to Object[] - case of primitive arrays
                List alist = new ArrayList();
                for (int i = 0; i < Array.getLength(val); i++) alist.add(Array.get(val, i));
                return alist;
            }
        }
        return Arrays.asList(val);
    }

    /**
     * @param cnames a string of column name(s).  It can also be expression including functions.
     *               ignore commas inside double-quotes or parentheses(e.g. function parameters) when parsing
     * @return an array of column names.
     */
    public static String[] splitCols(String cnames) {
        return cnames == null ? new String[0] : cnames.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)(?![^(]*\\))");
    }

    /**
     * If both ID and label exists, the label should be used in contexts where
     * case sensitivity is supported.
     * @param dt Firefly table column
     * @return the resolved name
     */
    public static String getAliasName(DataType dt) {
        return !isEmpty(dt.getID()) && !isEmpty(dt.getLabel()) ? dt.getLabel() : dt.getKeyName();
    }

    public static void fixDuplicates(List<DataType> cols) {
        HashSet<String> cnames = new HashSet<>();
        for (DataType dt : cols) {
            if (cnames.contains(dt.getKeyName().toUpperCase())) {
                String nCname = adjCname(dt.getKeyName(), (name)->cnames.contains(name.toUpperCase()));
                dt.rename(nCname);
            }
            cnames.add(dt.getKeyName().toUpperCase());
        }
    }

    private static String adjCname(String cname, Predicate<String> matcher) {
        int idx = 1;
        String nCname;
        do {
            nCname = "%s_%d".formatted(cname, idx++);
        } while (matcher.test(nCname));
        return nCname;
    }

    public static void fixCname(DataGroup dg, String cname) {
        if (dg.containsKey(cname)) {
            String nCname = adjCname(cname, (name)->dg.containsKey(name));
            dg.renameColumn(cname, nCname);
        }
    }

    /**
     * Parse datatype="char" (or unicodeChar) to String or String[]
     *  - shape[0] = chars per string (> 0), unless shape.length == 1 and shape[0] == -1 (variable)
     *  - Only the last dimension may be -1 (variable)
     * Returns:
     *  - shape.length == 0              -> String (scalar)
     *  - shape.length == 1 && >0        -> one fixed-length String (exactly shape[0] chars)
     *  - shape.length == 1 && == -1     -> one variable-length String (no split)
     *  - shape.length >= 2              -> nested Object[] with leaf = String (each of length shape[0])
     *
     * @param s         flat data (already decoded)
     * @param shape     dims info (e.g., "3x2x*" -> [3, 2, -1])
     * @param trimPad   if true, strip trailing spaces from each fixed-length string
     */
    public static Object strToStringAry(String s, int[] shape, boolean trimPad) {
        if (s == null || s.isEmpty()) return null;
        if (shape == null || shape.length == 0) return s; // scalar: no arraysize

        // Single-dimension cases; should not be needed because isArrayType is called first, but handle anyway
        if (shape.length == 1) {
            int first = shape[0];
            if (first <= 0) {
                // arraysize="*" → variable-length single string
                return trimPad ? rstrip(s) : s;
            }
            if (s.length() != first) {
                throw new IllegalArgumentException("Input length (" + s.length()
                        + ") must equal arraysize[0] (" + first + ") for fixed-length single string");
            }
            return trimPad ? rstrip(s) : s;
        }

        // Multi-dimension cases (length >= 2)
        final int charsPerString = shape[0];
        if (charsPerString <= 0) {
            throw new IllegalArgumentException("arraysize[0] (chars per string) must be > 0");
        }
        if (s.length() % charsPerString != 0) {
            throw new IllegalArgumentException("Input length not a multiple of charsPerString");
        }

        // Validate middle dims and resolve last=-1 if present
        int[] dims = Arrays.copyOfRange(shape, 1, shape.length);
        for (int i = 0; i < dims.length - 1; i++) {
            if (dims[i] <= 0) {
                throw new IllegalArgumentException("Only the last dimension may be -1; others must be > 0");
            }
        }

        final int totalStrings = s.length() / charsPerString;

        // Resolve variable last dim
        int fixedProd = 1;
        for (int i = 0; i < dims.length - 1; i++) fixedProd *= dims[i];
        if (dims[dims.length - 1] == -1) {
            if (fixedProd == 0) throw new IllegalArgumentException("Invalid arraysize");
            int last = totalStrings / fixedProd;
            if (last * fixedProd != totalStrings) {
                throw new IllegalArgumentException("Data length incompatible with variable last dimension");
            }
            dims[dims.length - 1] = last;
        } else if (dims[dims.length - 1] <= 0) {
            throw new IllegalArgumentException("Last dimension must be > 0 or -1");
        }

        // Check product of dims matches number of strings
        int expectedStrings = 1;
        for (int d : dims) expectedStrings *= d;
        if (expectedStrings != totalStrings) {
            throw new IllegalArgumentException("Data length incompatible with arraysize: expected "
                    + expectedStrings + " strings, got " + totalStrings);
        }

        // Build nested arrays; leaf = String (len = charsPerString)
        AtomicInteger idx = new AtomicInteger(0);
        return buildNested(s, charsPerString, dims, 0, idx, trimPad);
    }

    private static Object buildNested(String s, int cps, int[] dims, int depth,
                                      AtomicInteger idx, boolean trimPad) {
        int count = dims[depth];
        Object[] out = new Object[count];

        if (depth == dims.length - 1) {
            // Leaf level: materialize Strings
            for (int i = 0; i < count; i++) {
                int start = idx.getAndIncrement() * cps;
                String piece = s.substring(start, start + cps);
                out[i] = trimPad ? rstrip(piece) : piece;
            }
        } else {
            for (int i = 0; i < count; i++) {
                out[i] = buildNested(s, cps, dims, depth + 1, idx, trimPad);
            }
        }
        return out;
    }

    /** Strip trailing ASCII spaces (for fixed-length padded strings). */
    private static String rstrip(String x) {
        int end = x.length();
        while (end > 0 && x.charAt(end - 1) == ' ') end--;
        return (end == x.length()) ? x : x.substring(0, end);
    }

//====================================================================
//
//====================================================================

    public enum Mode { original, displayed};

    public static Map<String, FormatUtil.Format> getAllFormats() {
        return Arrays.stream(FormatUtil.Format.values())
                .collect(Collectors.toMap(f -> f.type, f -> f));
    }

    public static class ParsedInfo {
        HashMap<String, ParsedColInfo> parsedInfo = new HashMap<>();  // keyed by column name

        public ParsedColInfo getInfo(String cname) {
            ParsedColInfo checkInfo = parsedInfo.get(cname);
            if (checkInfo == null) {
                checkInfo = new ParsedColInfo();
                parsedInfo.put(cname, checkInfo);
            }
            return checkInfo;
        }
    }

    public static class ParsedColInfo {
        public boolean formatChecked;              // indicates guess format logic has been performed
        public boolean htmlChecked;                // indicates html content check has been performed
        public int startIdx;
        public int endIdx;

    }
}

