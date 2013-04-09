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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
 */
public class DsvToDataGroup {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    public static DataGroup parse(File inf, CSVFormat format) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);

        List<DataType> columns = new ArrayList<DataType>();
        CSVParser parser = new CSVParser(reader, format);
        List<CSVRecord> records = parser.getRecords();
        if (records !=null && records.size() > 0) {

            // parse the column info
            CSVRecord cols = records.get(0);
            for(Iterator<String> itr = cols.iterator(); itr.hasNext(); ) {
                String s = itr.next();
                if (!StringUtils.isEmpty(s)) {
                    columns.add(new DataType(s, String.class));
                }
            }

            DataGroup dg = new DataGroup(null, columns);
            dg.beginBulkUpdate();

            // parse the data
            for (int i = 1; i < records.size(); i++) {
                DataObject row = parseRow(dg, records.get(i));
                if (row != null) {
                    dg.add(row);
                }
            }
            dg.endBulkUpdate();
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
