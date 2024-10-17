/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DuckDbReadable;
import edu.caltech.ipac.firefly.server.util.JsonToDataGroup;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.csv.CSVFormat;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        Format format = guessFormat(inf);
        if (format == Format.IPACTABLE) {
            return IpacTableReader.read(inf, request);
        } else if (format == Format.VO_TABLE) {
            DataGroup[] tables = VoTableReader.voToDataGroups(inf.getAbsolutePath(), request, tableIndex);
            if (tables.length > 0) {
                return tables[0];
            } else return null;
        } else if (format == Format.CSV || format == Format.TSV) {
            return DsvTableIO.parse(inf, format, request);
        } else if (format == Format.FITS ) {
            try {
                // Switch to the new function:
                return FITSTableReader.convertFitsToDataGroup(inf.getAbsolutePath(), request, FITSTableReader.DEFAULT, tableIndex);
            } catch (Exception e) {
                throw new IOException("Unable to read FITS file:" + inf, e);
            }
        } else if (format == Format.JSON) {
            return JsonToDataGroup.parse(inf, request);
        } else {
            throw new IOException("Unsupported format, file:" + inf);
        }
    }



    private static boolean isRegLine(String line) {
        if (isEmpty(line)) return false;
        line= line.trim().toLowerCase();
        for(String sw : regStartWith) {
            if (line.startsWith(sw)) return true;
        }
        return false;
    }

    public static Format guessFormat(File inf) throws IOException {

        // guess based on filename extension
        if (inf.getName().toLowerCase().endsWith("tar")) {
            return Format.TAR;
        }

        var fmt = DuckDbReadable.guessFileFormat(inf.getAbsolutePath());      // test for files that DuckDb can import directly
        if (fmt != null) return fmt;

        // guess by sampling file content
        int readAhead = 10;
        int row = 0;

        BufferedReader subsetReader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        char[] charAry= new char[IpacTableUtil.FILE_IO_BUFFER_SIZE];
        subsetReader.read(charAry,0,charAry.length);           // limit the amount for the guess to FILE_IO_BUFFER_SIZE(32k)
        BufferedReader reader= new BufferedReader(new CharArrayReader(charAry));
        try {
            String line = reader.readLine();
            if (line.startsWith("{")) {
                return Format.JSON;
            } else if (line.startsWith("SIMPLE  = ")) {
                return Format.FITS;
            }

            int[][] counts = new int[readAhead][2];
            int csvIdx = 0, tsvIdx = 1;
            int regionTextCnt= 0;
            if (isRegLine(line))  regionTextCnt++;
            while ( (line != null && !line.trim().isEmpty()) && row < readAhead) {
                if (line.startsWith("|") || line.startsWith("\\")) {
                    return Format.IPACTABLE;
                } else if (line.startsWith("COORD_SYSTEM: ") || line.startsWith("EQUINOX: ") ||
                        line.startsWith("NAME-RESOLVER: ")) {
                    //NOTE: a fixed targets file contains the following lines at the beginning:
                    //COORD_SYSTEM: xxx
                    //EQUINOX: xxx
                    //NAME-RESOLVER: xxx
                    return Format.FIXEDTARGETS;
                } else if (line.startsWith("<VOTABLE") ||
                        (line.contains("<?xml") && line.contains("<VOTABLE "))) {
                    return Format.VO_TABLE;
                } else if (isUwsEl(line)) {
                    return Format.UWS;
                } else if (row == 0 && line.toLowerCase().indexOf("pdf") > 0) {
                    return Format.PDF;
                } else if (isRegLine(line)) {
                    regionTextCnt++;
                    if (regionTextCnt>5) return Format.REGION;
                }

                counts[row][csvIdx] = getColCount(CSVFormat.DEFAULT, line);
                counts[row][tsvIdx] = getColCount(CSVFormat.TDF, line);
                row++;
                line = reader.readLine();
            }
            if (row<readAhead && regionTextCnt>1) return Format.REGION;
            // check csv
            int c = counts[0][csvIdx];
            boolean cMatch = c > 0;
            for(int i = 1; i < row; i++) {
                cMatch = cMatch && counts[i][csvIdx] == c;
            }
            // check tsv
            int t = counts[0][tsvIdx];
            boolean tMatch = t > 0;
            for(int i = 1; i < row; i++) {
                tMatch = tMatch && counts[i][tsvIdx] == t;
            }

            if (cMatch && tMatch) {
                if (t > c) {
                    return Format.TSV;
                } else {
                    return Format.CSV;
                }
            } else {
                if (cMatch) {
                    return Format.CSV;
                } else if (tMatch) {
                    return Format.TSV;
                } else {
                    return Format.UNKNOWN;
                }
            }
        } catch (Exception e){
            return Format.UNKNOWN;
        } finally {
            FileUtil.silentClose(subsetReader);
            FileUtil.silentClose(reader);
        }

    }

    private static boolean isUwsEl(String line) {
        line = line.trim().toLowerCase();
        boolean isUws = line.contains("www.ivoa.net/xml/uws");
        return isUws && line.matches("<(.+:)?job .*");
    }

    private static int getColCount(CSVFormat format, String line) {
        try {
            return format.parse(new StringReader(line)).iterator().next().size();
        } catch (Exception e) {
            return -1;
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
                new DataType("desc", String.class)
        };
        DataGroup dg = new DataGroup("Header of extension with index " + idx, cols);
        meta.getAttributeList().forEach(a -> dg.getTableMeta().addKeyword(a.getKey(), a.getValue()));
        for (DataType col : meta.getCols() ) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], col.getKeyName());
            row.setDataElement(cols[1], col.getTypeLabel());
            row.setDataElement(cols[2], col.getUnits());
            row.setDataElement(cols[3], col.getDesc());
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
                int idx = 1;
                String nCname;
                do {
                    nCname = "%s_%d".formatted(dt.getKeyName(), idx++);
                } while (cnames.contains(nCname.toUpperCase()));
                if (isEmpty(dt.getID()))    dt.setID(dt.getKeyName());      // if we change cname, save original as ID.
                dt.setLabel(dt.getKeyName());
                dt.setKeyName(nCname);
            }
            cnames.add(dt.getKeyName().toUpperCase());
        }
    }

//====================================================================
//
//====================================================================

        public enum Mode { original, displayed};

        public enum Format {
                        TSV("tsv", ".tsv"),
                        CSV("csv", ".csv"),
                        IPACTABLE("ipac", ".tbl"),
                        UNKNOWN("null", null),
                        FIXEDTARGETS("fixed-targets", ".tbl"),
                        FITS("fits",".fits"),
                        JSON("json", ".json"),
                        PDF("pdf", ".pdf"),
                        TAR("tar", ".tar"),
                        HTML("html", ".html"),
                        VO_TABLE("votable", ".xml"),
                        VO_TABLE_TABLEDATA("votable-tabledata", ".vot"),
                        VO_TABLE_BINARY("votable-binary-inline", ".vot"),
                        VO_TABLE_BINARY2("votable-binary2-inline", ".vot"),
                        VO_TABLE_FITS("votable-fits-inline",".vot"),
                        REGION("reg", ".reg"),
                        PNG("png", ".png"),
                        UWS("uws", ".xml"),
                        PARQUET(DuckDbReadable.Parquet.NAME, "."+DuckDbReadable.Parquet.NAME);
        public String type;
        String fileNameExt;
        Format(String type, String ext) {
            this.type = type;
            this.fileNameExt = ext;
        }
        public String getFileNameExt() {
            return fileNameExt;
        }
        public String toString() {return type;}
    }

    public static Map<String, Format> getAllFormats() {
        return Arrays.stream(Format.values())
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

