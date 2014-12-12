package edu.caltech.ipac.util;

import edu.caltech.ipac.astro.IpacTableReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Date: Jun 25, 2009
 *
 * @author loi
 * @version $Id: IpacTableUtil.java,v 1.5 2012/11/08 23:56:49 tlau Exp $
 */
public class IpacTableUtil {

    public static final int FILE_IO_BUFFER_SIZE = 1024*256;

    public static void writeAttributes(PrintWriter writer, Collection<DataGroup.Attribute> attribs, String... ignoreList) {
        String type, line;
        for (DataGroup.Attribute attrib : attribs) {
            if (ignoreList == null || !CollectionUtil.exists(ignoreList, attrib.getKey())) {
                type = attrib.hasType() ? attrib.getType() + " " : "";
                line = "\\" + type + attrib.getKey() + " = " + attrib.formatValue(Locale.US);
                writer.println(line);
            }
        }
    }

    public static void writeHeader(PrintWriter writer, List<DataType> headers) {
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getKeyName()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getTypeDesc()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getDataUnit()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getNullString()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
    }

    public static void writeRow(PrintWriter writer, List<DataType> headers, DataObject row) {
        for (DataType dt : headers) {
            String v = row.getFormatedData(dt);
            // when writting out the IPAC table.. if ROWID is given, and data is not found. use the getRowId() value instead.
            if (v == null && dt.getKeyName().equals(DataGroup.ROWID_NAME)) {
                v = dt.getFormatInfo().formatData(row.getRowIdx());
            }
            writer.print(" " + v);
        }
        writer.println(" ");
    }


    /**
     * 
     * @param reader
     * @return
     * @throws IOException
     */
    public static List<DataType> readColumns(BufferedReader reader) throws IOException {

        reader.mark(IpacTableUtil.FILE_IO_BUFFER_SIZE);
        List<DataType> cols = new ArrayList<DataType>();
        try {
            String line = reader.readLine();
            // skip to column desc
            while (line != null) {
                line = line.trim();
                if (line.startsWith("\\")) {
                    // attributes.. skip
                } else if (line.startsWith("|")) {
                    break;
                } else if (line.length() == 0) {
                    //  skip this line
                } else {
                    // data row begins.
                    throw new IOException("Table without column headers");
                }
                line = reader.readLine();
            }

            cols = createColumnDefs(line);
            if (cols.size() == 0) return cols;

            // column type
            line = reader.readLine();
            if (line != null && line.startsWith("|")) {
                setDataType(cols, line);

                // column unit
                line = reader.readLine();
                if (line != null && line.startsWith("|")) {
                    setDataUnit(cols, line);

                    // column null string identifier
                    line = reader.readLine();
                    if (line != null && line.startsWith("|")) {
                        setDataNullStr(cols, line);
                    }
                }
            }

        } finally {
            reader.reset();
        }

        return cols;
    }

    public static  List<DataType> createColumnDefs(String line) {
        ArrayList<DataType> cols = new ArrayList<DataType>();
        if (line != null && line.startsWith("|")) {
            String[] names = parseHeadings(line.trim());
            for(String n : names) {
                DataType dt = new DataType(n.trim(), Object.class);
                dt.getFormatInfo().setWidth(n.length());
                cols.add(dt);
            }
        }
        return cols;
    }

    public static  void setDataType(List<DataType> cols, String line) {
        if (line != null && line.startsWith("|")) {
            String[] types = parseHeadings(line.trim());
            for (int i = 0; i < types.length; i++) {
                String typeDesc = types[i].trim();
                cols.get(i).setDataType(IpacTableReader.resolveClass(typeDesc));
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
                cols.get(i).setNullString(nullables[i].trim());
            }
        }
    }
    public static void guessFormatInfo(DataType dataType, String value) {

        String formatStr = IpacTableReader.guessFormatStr(dataType, value.trim());
        if (formatStr != null) {
            DataType.FormatInfo.Align align = value.startsWith(" ") ? DataType.FormatInfo.Align.RIGHT
                                                : DataType.FormatInfo.Align.LEFT;
            DataType.FormatInfo fi = dataType.getFormatInfo();
            fi.setDataFormat(formatStr);
            fi.setDataAlign(align);
            fi.setIsDefault(false);
        }
    }

    public static void guessDataType(DataType type, String rval) {
        if (StringUtil.isEmpty(rval)) return;

        try {
            Integer.parseInt(rval);
            type.setDataType(Integer.class);
            return;
        }catch (Exception e){}

        try {
            Long.parseLong(rval);
            type.setDataType(Long.class);
            return;
        }catch (Exception e){}

        try {
            Float.parseFloat(rval);
            type.setDataType(Float.class);
            return;
        }catch (Exception e){}

        try {
            Double.parseDouble(rval);
            type.setDataType(Double.class);
            return;
        }catch (Exception e){}

        type.setDataType(String.class);

    }

    public static List<DataGroup.Attribute> readAttributes(BufferedReader reader) throws IOException {

        reader.mark(IpacTableUtil.FILE_IO_BUFFER_SIZE);

        ArrayList<DataGroup.Attribute> attributes = new ArrayList<DataGroup.Attribute>();
        try {
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.startsWith("\\")) {
                    DataGroup.Attribute attrib = parseAttribute(line);
                    if (attrib != null) {
                        attributes.add(attrib);
                    }
                } else if (line.startsWith("|")) {
                    break;
                } else if (line.length() == 0) {
                    //  skip this line
                } else {
                    // data row begins.
                    break;
                }
                line = reader.readLine();
            }
        } finally {
            reader.reset();
        }
        return attributes;
    }

    public static DataObject parseRow(DataGroup source, String line) {
        return parseRow(source,  line, true);
    }

    public static DataObject parseRow(DataGroup source, String line, boolean isFixedLength) {
        return parseRow(source, line, isFixedLength, false);
    }

    /**
     *
     * @param source
     * @param line
     * @param isFixedLength true if all columns are given.  line with missing columns will throws exception.
     * @return
     */
    public static DataObject parseRow(DataGroup source, String line, boolean isFixedLength, boolean saveFormattedData) {
        if (line==null) return null;
        DataType[] headers = source.getDataDefinitions();
        String rval, val;
        int offset=0, endOfLine=0, endoffset=0;
        try {
            if (line.startsWith(" ") && line.trim().length() > 0) {
                DataObject row = new DataObject(source);
                offset = 0;
                endOfLine = line.length();

                for (DataType type : headers) {
                    endoffset = offset + type.getFormatInfo().getWidth() + 1;

                    if (offset > endOfLine) {
                        // expecting more data, but end of line has been reached.
                        if (isFixedLength) {
                            throw new RuntimeException("Expecting more data when end-of-end has been reached:\n" + " [" + offset + "-" + endoffset + "]  line:" + line );
                        } else {
                            // it's okay.  we'll take what's given and treat the rest as null.
                            break;
                        }
                    }
                    // if ending spaces are missing... just ignore it.
                    if (endoffset > endOfLine) {
                        endoffset = endOfLine;
                    }

                    rval = line.substring(offset, endoffset);
                    val = rval == null ? null : rval.trim();

                    if (!type.isKnownType()) {
                        IpacTableUtil.guessDataType(type, val);
                    }

                    row.setDataElement(type, type.convertStringToData(val));
                    if (saveFormattedData) {
                        String fval = rval == null || rval.length() < 2 ? null : rval.substring(1);
                        row.setFormattedData(type, fval);
                    }

                    if (val != null && val.length() > type.getMaxDataWidth()) {
                        type.setMaxDataWidth(val.length());
                    }
                    offset = endoffset;
                    if (type.getFormatInfo().isDefault()) {
                        IpacTableUtil.guessFormatInfo(type, rval);
                    }
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

    private static String[] parseHeadings(String line) {
        return line.substring(1).split("\\|");
    }

    public static DataGroup.Attribute parseAttribute(String line) {
        DataGroup.Attribute retval = null;

        String[] keyVal = line.replaceFirst("\\\\", "").split("=", 2);  // the first '=' is the key/val separator
        if ( keyVal.length == 2 ) {                                      // the following '=' chars(if any) are part of val.
            String[] typeKey = keyVal[0].trim().split("\\s+", 2);        // the first word is type.  if only 1 word, then type is empty-string
            String type = typeKey != null && typeKey.length > 1 ? typeKey[0].trim() : "";
            String key = typeKey != null && typeKey.length > 1 ? typeKey[1].trim() : typeKey[0].trim();
            String val = keyVal[1].trim();
            retval = new DataGroup.Attribute(key, val, type, null);
        }
        return retval;
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

