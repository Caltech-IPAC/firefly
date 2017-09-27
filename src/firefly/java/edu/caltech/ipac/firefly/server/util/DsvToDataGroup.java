/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a utility class used to read/write Delimiter-separated values(DSV)
 * file into/from a DataGroup object.
 *
 * The CSV standard can be found here.  http://www.ietf.org/rfc/rfc4180.txt
 *
 * @author loi
 * @version $Id: DsvToDataGroup.java,v 1.2 2012/10/23 18:37:22 loi Exp $
 *
 * 09/28/17
 * LZ added another method to sue InputStream to read data
 *
 */
public class DsvToDataGroup {

    public static DataGroup parse(InputStream inf, CSVFormat format) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        return getData(reader, format);
    }
    public static DataGroup parse(File inf, CSVFormat format) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        return getData(reader, format);

       /* List<DataType> columns = new ArrayList<DataType>();
        CSVParser parser = new CSVParser(reader, format);
        List<CSVRecord> records = parser.getRecords();
        if (records !=null && records.size() > 0) {

            // parse the column info
            CSVRecord cols = records.get(0);
            for(Iterator<String> itr = cols.iterator(); itr.hasNext(); ) {
                String s = itr.next();
                if (!StringUtils.isEmpty(s)) {
                    columns.add(new DataType(s, null)); // unknown type
                }
            }

            DataGroup dg = new DataGroup(null, columns);

            // parse the data
            for (int i = 1; i < records.size(); i++) {
                DataObject row = parseRow(dg, records.get(i));
                if (row != null) {
                    dg.add(row);
                }
            }
            dg.shrinkToFitData();
            return dg;
        }
        return null;*/
    }


    private static DataGroup getData( BufferedReader reader, CSVFormat format)throws IOException{
        List<DataType> columns = new ArrayList<DataType>();
        CSVParser parser = new CSVParser(reader, format);
        List<CSVRecord> records = parser.getRecords();
        if (records !=null && records.size() > 0) {

            // parse the column info
            CSVRecord cols = records.get(0);
            for(Iterator<String> itr = cols.iterator(); itr.hasNext(); ) {
                String s = itr.next();
                if (!StringUtils.isEmpty(s)) {
                    columns.add(new DataType(s, null)); // unknown type
                }
            }

            DataGroup dg = new DataGroup(null, columns);

            // parse the data
            for (int i = 1; i < records.size(); i++) {
                DataObject row = parseRow(dg, records.get(i));
                if (row != null) {
                    dg.add(row);
                }
            }
            dg.shrinkToFitData();
            return dg;
        }
        return null;
    }
    public static void write(File outf, DataGroup data) throws IOException {
        write(new FileWriter(outf), data, CSVFormat.DEFAULT);
    }

    public static void write(File outf, DataGroup data, CSVFormat format) throws IOException {
        write(new FileWriter(outf), data, format);
    }

    public static void write(Writer writer, DataGroup data, CSVFormat format) throws IOException {

        BufferedWriter outf = new BufferedWriter(writer, IpacTableUtil.FILE_IO_BUFFER_SIZE);
        try {
            CSVPrinter printer = new CSVPrinter(outf, format);

            if (data != null && data.size() > 0) {
                for (DataType t : data.getDataDefinitions()) {
                    printer.print(t.getKeyName());
                }
                printer.println();

                for (DataObject row : data.values()) {
                    for (String s : row.getFormatedData()) {
                        printer.print(s.trim());
                    }
                    printer.println();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outf != null) {
                outf.close();
            }
        }
    }

    static DataObject parseRow(DataGroup source, CSVRecord line) {

        DataType[] headers = source.getDataDefinitions();
        if (line != null && line.size() > 0) {
            DataObject row = new DataObject(source);
            String val;
                for (int i = 0; i < headers.length; i++) {
                    DataType type = headers[i];
                    val = StringUtils.isEmpty(line.get(i)) ? null : line.get(i).trim();
                    if (!type.isKnownType()) {
                        IpacTableUtil.guessDataType(type,val);
                    }
                    row.setFormattedData(type,val);
                    row.setDataElement(type, type.convertStringToData(val));

                    if (val != null && val.length() > type.getMaxDataWidth()) {
                        type.setMaxDataWidth(val.length());
                    }
                    if (type.getFormatInfo().isDefault()) {
                        IpacTableUtil.guessFormatInfo(type, val);
                    }
                }
            return row;
        }
        return null;
    }

    public static void main(String[] args) {

        try {
            File inf = new File(args[0]);
            DataGroup dg = parse(inf, CSVFormat.DEFAULT);
            IpacTableWriter.save(System.out, dg);
            write(new File(inf.getAbsolutePath() + ".csv"), dg);
            write(new File(inf.getAbsolutePath() + ".tsv"), dg, CSVFormat.TDF);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
