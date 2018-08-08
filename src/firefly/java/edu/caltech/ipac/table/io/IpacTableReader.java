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

public final class IpacTableReader {

    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static DataGroup read(File inf, String... onlyColumns) throws IOException {
        TableDef tableDef = IpacTableUtil.getMetaInfo(inf);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        return doRead(bufferedReader, tableDef, onlyColumns);
    }

    public static DataGroup read(InputStream inputStream, String... onlyColumns) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        TableDef tableDef = IpacTableUtil.getMetaInfo(bufferedReader);
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
        IpacTableUtil.consumeColumnInfo(outData);   // move column attributes into columns
        outData.trimToSize();
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