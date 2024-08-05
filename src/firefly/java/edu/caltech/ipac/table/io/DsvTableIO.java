/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.table.IpacTableUtil;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static edu.caltech.ipac.table.IpacTableUtil.isKnownType;
import static edu.caltech.ipac.table.TableUtil.Format;
import static edu.caltech.ipac.table.TableUtil.Format.CSV;
import static edu.caltech.ipac.table.TableUtil.Format.TSV;
import static edu.caltech.ipac.table.TableUtil.ParsedColInfo;
import static edu.caltech.ipac.table.TableUtil.ParsedInfo;
import static edu.caltech.ipac.table.TableUtil.getDetails;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

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
 * LZ added another method in order to read file through an InputStream
 *
 */
public class DsvTableIO {

    public static DataGroup parse(File inf, Format format) throws IOException {
        return parse(inf, toCsvFormat(format.type));
    }

    public static DataGroup parse(File inf, Format format, TableServerRequest request) throws IOException {
        return parse(inf, toCsvFormat(format.type), request);
    }

    public static DataGroup parse(InputStream inf, CSVFormat format) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inf, "UTF-8"), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        return getData(reader, format, null);
    }

    public static DataGroup parse(File inf, CSVFormat format) throws IOException {
        return parse(inf,format,null);
    }

    public static DataGroup parse(File inf, CSVFormat format, TableServerRequest request) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        return getData(reader, format, request);

    }

    public static CSVFormat toCsvFormat(String format) {
        format = format == null ? "null" : format;
            if (format.equals(CSV.type)) {
                return CSVFormat.DEFAULT;
            } else if (format.equals(TSV.type)) {
                return CSVFormat.TDF;
            }
        throw new IllegalArgumentException("Unknown format: " + format);
    }




    private static DataGroup getData( BufferedReader reader,
                                      CSVFormat format,
                                      TableServerRequest request) throws IOException{
        CSVParser parser = new CSVParser(reader, format);
        List<CSVRecord> records = parser.getRecords();
        if (records !=null && records.size() > 0) {

            // parse the column info
            CSVRecord cols = records.get(0);
            List<DataType> columns = convertToDataType(cols);
            DataGroup dg = new DataGroup(null, columns);
            ParsedInfo colCheckInfo = new ParsedInfo();

            // parse the data
            for (int i = 1; i < records.size(); i++) {
                DataObject row = parseRow(dg, records.get(i), colCheckInfo);
                if (row != null) {
                    dg.add(row);
                }
            }
            SpectrumMetaInspector.searchForSpectrum(dg,request);
            dg.trimToSize();
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
                    for (String s : row.getFormattedData(true)) {
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

    private static List<DataType> convertToDataType(CSVRecord record) {
        List<DataType> columns = new ArrayList<DataType>();
        for(Iterator<String> itr = record.iterator(); itr.hasNext(); ) {
            String s = itr.next();
            if ("\uFEFF".charAt(0) == s.toCharArray()[0]){
                s = s.substring(1);//LZ fixed the issue with the BOM character
            }
            if (!isEmpty(s)) {
                columns.add(new DataType(s.trim(), null)); // unknown type
            }
        }
        return columns;
    }

    static DataObject parseRow(DataGroup source, CSVRecord line, ParsedInfo colCheckInfo) {

        DataType[] headers = source.getDataDefinitions();
        if (line != null && line.size() > 0) {
            DataObject row = new DataObject(source);
            String val;
                for (int i = 0; i < headers.length; i++) {
                    DataType type = headers[i];
                    val = isEmpty(line.get(i)) ? null : line.get(i).trim();
                    if (!isKnownType(type.getDataType())) {
                        IpacTableUtil.guessDataType(type,val);
                    }
                    row.setDataElement(type, type.convertStringToData(val));

                    ParsedColInfo checkInfo = colCheckInfo.getInfo(type.getKeyName());
                    IpacTableUtil.applyGuessLogic(type, val, checkInfo);
                }
            return row;
        }
        return null;
    }

    private static DataGroup getHeader(File infile, String format)throws IOException{
        CSVFormat csvFormat = format.equals(TSV.type) ? CSVFormat.TDF : CSVFormat.DEFAULT;
        BufferedReader reader = new BufferedReader(new FileReader(infile), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        try {
            reader.mark(IpacTableUtil.FILE_IO_BUFFER_SIZE);
            CSVParser parser = new CSVParser(reader, csvFormat);
            List<DataType> columns = convertToDataType(parser.iterator().next()); // read just the first line.
            DataGroup dg = new DataGroup(null, columns);
            reader.reset();
            int lines = -1;             // don't count the header
            while (reader.readLine() != null) lines++;      // get line count
            dg.setSize(lines);
            return dg;
        } finally {
            reader.close();
        }
    }

    public static FileAnalysisReport analyze(File infile, String format, FileAnalysisReport.ReportType type) throws IOException {
        DataGroup header = getHeader(infile, format);
        FileAnalysisReport report = new FileAnalysisReport(type, format, infile.length(), infile.getPath());
        FileAnalysisReport.Part part = new FileAnalysisReport.Part(FileAnalysisReport.Type.Table, String.format("%s (%d cols x %s rows)", format.getClass().getSimpleName(), header.getDataDefinitions().length, header.size()));
        part.setTotalTableRows(header.size());
        report.addPart(part);
        if (type.equals(FileAnalysisReport.ReportType.Details)) {
            IpacTableDef meta = new IpacTableDef();
            meta.setCols(Arrays.asList(header.getDataDefinitions()));
            part.setDetails(getDetails(0, meta));
        }
        return report;
    }

    public static void main(String[] args) {

        try {
            File inf = new File(args[0]);
            DataGroup dg = parse(inf, CSVFormat.DEFAULT);
            write(new File(inf.getAbsolutePath() + ".csv"), dg);
            write(new File(inf.getAbsolutePath() + ".tsv"), dg, CSVFormat.TDF);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
