/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.TableDef;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * read in the file in IPAC table format
 *
 * @author Xiuqin Wu
 */

//TODO: must work with IrsaAncilDataGetter

public final class IpacTableReader {


    private static final String NO_DATA = "No data found";
    private static final Logger.LoggerImpl logger = Logger.getLogger();

//    /**
//     * Parse the file in IPAC table format and put the data in a
//     * DataObjectGroup. If there is no data in the file, throw
//     * IpacTableException
//     */
//    public static DataGroup readIpacTable(Reader fr, String catName) throws IpacTableException {
//        return readIpacTable(fr, catName, null, true);
//    }
//
//
//    /**
//     * Parse the file in IPAC table format and put the data in a
//     * DataObjectGroup. If there is no data in the file, throw
//     * IpacTableException
//     *
//     * @param isHeadersOnlyAllow set to true to allow ipac table with only headers(refer to as
//     *                           attributes in this class) information.
//     *                           don't confuse this with column's headers(refer to as headers in this class).
//     */
//    public static DataGroup readIpacTable(Reader fr,
//                                          String catName,
//                                          String onlyColumns[],
//                                          boolean isHeadersOnlyAllow) throws IpacTableException {
//        try {
//            DataGroup retval = read(fr, false, onlyColumns);
//            ensureIpac(retval, catName, isHeadersOnlyAllow);
//            return retval;
//        } catch (IOException e) {
//            throw new IpacTableException(e.getMessage(), e);
//        }
//    }
//
//
//    public static DataGroup readIpacTable(File f, String catName) throws IpacTableException {
//        return readIpacTable(f, null, catName);
//    }
//
//    public static DataGroup readIpacTable(File f,
//                                          String onlyColumns[],
//                                          String catName) throws IpacTableException {
//        return readIpacTable(f, onlyColumns, catName, true);
//    }
//
//    /**
//     * Parse the file in IPAC table format and put the data in a
//     * DataObjectGroup. If there is no data in the file, throw
//     * IpacTableException
//     */
//    public static DataGroup readIpacTable(File f,
//                                          String onlyColumns[],
//                                          String catName,
//                                          boolean isHeadersOnlyAllow) throws IpacTableException {
//        try {
//            DataGroup retval = read(f, false, onlyColumns);
//            ensureIpac(retval, catName, isHeadersOnlyAllow);
//            return retval;
//        } catch (FileNotFoundException fnfe) {
//            System.out.println("File not found Exception");
//            throw new IpacTableException("File or object not found");
//        } catch (IOException e) {
//            throw new IpacTableException(e.getMessage(), e);
//        }
//    }
//
//
//    private static void ensureIpac(DataGroup retval, String catName, boolean isHeadersOnlyAllow) throws IpacTableException {
//        retval.setTitle(catName);
//        if (retval.size() == 0) {
//            if (!isHeadersOnlyAllow) {
//                String name = AppProperties.getProperty("CatalogDialog.cats."
//                        + catName + ".ShortName");
//                if (name == null)
//                    name = catName;
//                throw new IpacTableException(NO_DATA + ": " + name);
//            }
//        }
//    }

    public static DataGroup read(File inf, String... onlyColumns) throws IOException {
        return read(inf, false, onlyColumns);
    }

    public static DataGroup read(File inf, boolean saveFormattedData, String... onlyColumns) throws IOException {
        TableDef tableDef = IpacTableUtil.getMetaInfo(inf);
        tableDef.setSaveFormattedData(saveFormattedData);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        return doRead(bufferedReader, tableDef, onlyColumns);
    }

    public static DataGroup read(InputStream inputStream, String... onlyColumns) throws IOException {
        return  read(inputStream, false, onlyColumns);
    }

    public static DataGroup read(InputStream  inputStream, boolean saveFormattedData, String... onlyColumns) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        TableDef tableDef = IpacTableUtil.getMetaInfo(bufferedReader);
        tableDef.setSaveFormattedData(saveFormattedData);
        return  doRead(bufferedReader, tableDef, onlyColumns);
    }


    public static void main(String args[]) {

        if (args.length > 0) {
            try {
                DataGroup dg = read(new File(args[0]));
                dg.setTitle("test");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                System.in.read();
            } catch (IOException e) {
            }
            DataGroup IRAC1fixedGroup;
            File f = new File("2massmag3_formatted.tbl");

            String onlyColumns[] = {"ra", "dec", "name", "mag"};
            String catName = "IRAC1";
            try {
                IRAC1fixedGroup = read(f, onlyColumns);
                IRAC1fixedGroup.setTitle(catName);
            } catch (Exception e) {
                System.out.println("got an exception:  " + e);
                e.printStackTrace();
            }
        }
    }

    static DataGroup doRead(BufferedReader bufferedReader, TableDef tableDef, String... onlyColumns) throws IOException {

        List<DataGroup.Attribute> attributes = tableDef.getKeywords();
        List<DataType> cols = tableDef.getCols();

        DataGroup inData = new DataGroup(null, cols);
        DataGroup outData;
        boolean isSelectedColumns = onlyColumns != null && onlyColumns.length > 0;

        if (isSelectedColumns) {
            List<DataType> selCols = new ArrayList<DataType>();
            for (String c : onlyColumns) {
                DataType dt = inData.getDataDefintion(c);
                if (dt != null) {
                    try {
                        selCols.add((DataType) dt.clone());
                    } catch (CloneNotSupportedException e) {}       // shouldn't happen
                }
            }
            outData = new DataGroup(null, selCols);
        } else {
            outData = inData;
        }

        outData.setKeywords(attributes);

        String line = null;
        int lineNum = tableDef.getExtras() == null ? 0 : tableDef.getExtras().getKey();

        try {
            line = tableDef.getExtras() == null ? bufferedReader.readLine() : tableDef.getExtras().getValue();
            lineNum++;
            DataObject row, arow;
            while (line != null) {
                row = IpacTableUtil.parseRow(inData, line, tableDef);
                if (row != null) {
                    if (isSelectedColumns) {
                        arow = new DataObject(outData);
                        for (DataType dt : outData.getDataDefinitions()) {
                            arow.setDataElement(dt, row.getDataElement(dt.getKeyName()));
                        }
                        outData.add(arow);
                    } else {
                        outData.add(row);
                    }
                }
                line = bufferedReader.readLine();
                lineNum++;
            }
        } catch(Exception e) {
            String msg = e.getMessage()+"<br>on line "+lineNum+": " + line;
            if (msg.length()>128) msg = msg.substring(0,128)+"...";
            logger.error(e, "on line "+lineNum+": " + line);
            throw new IOException(msg);
        } finally {
            bufferedReader.close();
        }

        return outData;
    }

    public static DataGroup getEnumValues(File inf, int cutoffPoint)  throws IOException {

        TableDef tableDef = IpacTableUtil.getMetaInfo(inf);
        List<DataType> cols = tableDef.getCols();

        HashMap<DataType, List<String>> enums = new HashMap<DataType, List<String>>();
        ArrayList<DataType> workList = new ArrayList<DataType>();

        for (DataType dt : cols) {
            if (dt.getDataType().isAssignableFrom(Float.class) ||
                    dt.getDataType().isAssignableFrom(Double.class)) {
                continue;
            }
            enums.put(dt, new ArrayList<String>());
            workList.add(dt);
        }
        DataGroup dg = new DataGroup(null, cols);

        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        String line = null;
        int lineNum = 0;
        try {
            line = reader.readLine();
            lineNum++;
            while (line != null) {
                DataObject row = IpacTableUtil.parseRow(dg, line, tableDef);
                if (row != null) {
                    List<DataType> ccols = new ArrayList<DataType>(workList);
                    for(DataType dt : ccols) {
                        String v = String.valueOf(row.getDataElement(dt));
                        List<String> l = enums.get(dt);
                        if (l == null || l.size() >= cutoffPoint ||
                                (dt.getDataType().isAssignableFrom(String.class) && v.length() > 20)) {
                            workList.remove(dt);
                            enums.remove(dt);
                        } else if (!l.contains(v)) {
                            l.add(v);
                        }
                    }
                }
                line = reader.readLine();
                lineNum++;
                if (enums.size() == 0) {
                    break;
                }
            }
        } catch(Exception e) {
            String msg = e.getMessage()+"<br>on line "+lineNum+": " + line;
            if (msg.length()>128) msg = msg.substring(0,128)+"...";
            logger.error(e, "on line "+lineNum+": " + line);
            throw new IOException(msg);
        } finally {
            reader.close();
        }

        if (enums.size() > 0) {
            for(DataType dt : enums.keySet()) {
                List<String> values = enums.get(dt);
                Collections.sort(values, DataGroupUtil.getComparator(dt));
                dg.addAttribute(TableMeta.makeAttribKey(
                        TableMeta.ITEMS_TAG, dt.getKeyName()),
                        StringUtils.toString(values, ","));
            }
        }

        return dg;
    }
}
