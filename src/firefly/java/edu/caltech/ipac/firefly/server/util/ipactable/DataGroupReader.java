/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.astro.FITSTableReader;
import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupUtil;
import edu.caltech.ipac.firefly.server.query.TemplateGenerator;
import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.*;
import org.apache.commons.csv.CSVFormat;

import java.io.*;
import java.util.*;


/**
 * Date: May 14, 2009
 *
 * @author loi
 * @version $Id: DataGroupReader.java,v 1.13 2012/11/05 18:59:59 loi Exp $
 */
public class DataGroupReader {
    public static final int MIN_PREFETCH_SIZE = AppProperties.getIntProperty("IpacTable.min.prefetch.size", 500);
    public static final String LINE_SEP = System.getProperty("line.separator");
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static enum Format { TSV(CSVFormat.TDF), CSV(CSVFormat.DEFAULT), IPACTABLE(), UNKNOWN(), FIXEDTARGETS(), FITS();
        CSVFormat type;
        Format() {}
        Format(CSVFormat type) {this.type = type;}
    }

    public static DataGroup readAnyFormat(File inf) throws IOException {
        Format format = guessFormat(inf);
        if (format == Format.IPACTABLE) {
            return read(inf, false, false);
        } else if (format == Format.CSV || format == Format.TSV) {
            return DsvToDataGroup.parse(inf, format.type);
        } else if (format == Format.FITS ) {
            try {
                // Switch to the new function:
                //List<DataGroup> retval = FITSTableReader.convertFITSToDataGroup(inf.getAbsolutePath(), null);
                List<DataGroup> retval = FITSTableReader.convertFitsToDataGroup(inf.getAbsolutePath(), null, null, "TOP_MOST");
                if (retval != null && retval.size() > 0) {
                    return retval.get(0);
                } else {
                    return null;
                }

            } catch (Exception e) {
                throw new IOException("Unable to read FITS file:" + inf, e);
            }
        } else {
            throw new IOException("Unsupported format, file:" + inf);
        }
    }
    
    public static DataGroup read(File inf, String... onlyColumns) throws IOException {
        return read(inf, false, onlyColumns);
        
    }

    public static DataGroup read(File inf, boolean readAsString, String... onlyColumns) throws IOException {
        return read(inf, true, readAsString, onlyColumns);
    }

    public static DataGroup read(File inf, boolean isFixedLength, boolean readAsString, String... onlyColumns) throws IOException {
        return read(inf, isFixedLength, readAsString, false, onlyColumns);
    }

    public static DataGroup read(File inf, boolean isFixedLength, boolean readAsString, boolean saveFormattedData, String... onlyColumns) throws IOException {

        TableDef tableMeta = IpacTableUtil.getMetaInfo(inf);
        Collection<DataGroup.Attribute> attributes = tableMeta.getAttributes().values();
        List<DataType> cols = tableMeta.getCols();

        if (readAsString) {
            for (DataType dt : cols) {
                dt.setDataType(String.class);
            }
        }

        DataGroup headers = new DataGroup(null, cols);
        DataGroup data = null;
        boolean isSelectedColumns = onlyColumns != null && onlyColumns.length > 0;

        if (isSelectedColumns) {
            List<DataType> selCols = new ArrayList<DataType>();
            for (String c : onlyColumns) {
                DataType dt = headers.getDataDefintion(c);
                if (dt != null) {
                    try {
                        selCols.add((DataType) dt.clone());
                    } catch (CloneNotSupportedException e) {}       // shouldn't happen
                }
            }
            data = new DataGroup(null, selCols);
        } else {
            data = headers;
        }

        data.beginBulkUpdate();

        for(DataGroup.Attribute a : attributes) {
            data.addAttributes(a);
        }

        String line = null;
        int lineNum = 0;

        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        try {
            line = reader.readLine();
            lineNum++;
            while (line != null) {
                DataObject row = IpacTableUtil.parseRow(headers, line, isFixedLength, saveFormattedData);
                if (row != null) {
                    if (isSelectedColumns) {
                        DataObject arow = new DataObject(data);
                        for (DataType dt : data.getDataDefinitions()) {
                            arow.setDataElement(dt, row.getDataElement(dt.getKeyName()));
                        }
                        data.add(arow);
                    } else {
                        data.add(row);
                    }
                }
                line = reader.readLine();
                lineNum++;
            }
        } catch(Exception e) {
            String msg = e.getMessage()+"<br>on line "+lineNum+": " + line;
            if (msg.length()>128) msg = msg.substring(0,128)+"...";
            logger.error(e, "on line "+lineNum+": " + line);
            throw new IOException(msg);
        } finally {
            reader.close();
        }

        data.endBulkUpdate();
        if (!saveFormattedData) {
            data.shrinkToFitData();
        }
        return data;
    }

    public static DataGroup getEnumValues(File inf, int cutoffPoint)  throws IOException {

        TableDef tableMeta = IpacTableUtil.getMetaInfo(inf);
        List<DataType> cols = tableMeta.getCols();

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
                DataObject row = IpacTableUtil.parseRow(dg, line, false);
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
                dg.addAttributes(TemplateGenerator.createAttribute(TemplateGenerator.Tag.ITEMS_TAG,
                                    dt.getKeyName(), StringUtils.toString(values, ",")));
            }
        }

        return dg;
    }

    public static Format guessFormat(File inf) throws IOException {

        String fileExt = FileUtil.getExtension(inf);
        if (fileExt != null) {
            if (fileExt.equalsIgnoreCase("csv")) {
                return Format.CSV;
            } else if (fileExt.equalsIgnoreCase("tsv")) {
                return Format.TSV;
            } else if (fileExt.equalsIgnoreCase("tbl")) {
                return Format.IPACTABLE;
            } else if (fileExt.equalsIgnoreCase("fits")) {
                return Format.FITS;
            }
        }

        int readAhead = 10;
        
        int row = 0;
        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        String line = reader.readLine();
        int[][] counts = new int[readAhead][2];
        int csvIdx = 0, tsvIdx = 1;
        while (line != null && row < readAhead) {
            if (line.startsWith("|") || line.startsWith("\\")) {
                return Format.IPACTABLE;
            } else if (line.startsWith("COORD_SYSTEM: ") || line.startsWith("EQUINOX: ") ||
                    line.startsWith("NAME-RESOLVER: ")) {
                //NOTE: a fixed targets file contains the following lines at the beginning:
                //COORD_SYSTEM: xxx
                //EQUINOX: xxx
                //NAME-RESOLVER: xxx
                return Format.FIXEDTARGETS;
            }
            
            counts[row][csvIdx] = CSVFormat.DEFAULT.parse(new StringReader(line)).iterator().next().size();
            counts[row][tsvIdx] = CSVFormat.TDF.parse(new StringReader(line)).iterator().next().size();
            row++;
            line = reader.readLine();
        }

        // check csv
        int c = counts[0][csvIdx];
        boolean cMatch = true;
        for(int i = 1; i < row; i++) {
            cMatch = cMatch && counts[i][csvIdx] == c;
        }
        // check tsv
        int t = counts[0][tsvIdx];
        boolean tMatch = true;
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
    }

    

//====================================================================
//
//====================================================================

}

