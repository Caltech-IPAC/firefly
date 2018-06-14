/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.firefly.server.util.JsonToDataGroup;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.VoTableReader;
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
public class TableUtil {

    public static DataGroup readAnyFormat(File inf) throws IOException {
        return readAnyFormat(inf, 0);
    }

    public static DataGroup readAnyFormat(File inf, int tableIndex) throws IOException {
        Format format = guessFormat(inf);
        if (format == Format.IPACTABLE) {
            return IpacTableReader.read(inf, true);
        } else if (format == Format.VO_TABLE) {
            DataGroup[] tables = VoTableReader.voToDataGroups(inf.getAbsolutePath());
            if (tables.length > tableIndex) {
                return tables[tableIndex];
            } else return null;
        } else if (format == Format.CSV || format == Format.TSV) {
            return DsvTableIO.parse(inf, format.type);
        } else if (format == Format.FITS ) {
            try {
                // Switch to the new function:
                return FITSTableReader.convertFitsToDataGroup(inf.getAbsolutePath(), null, null, FITSTableReader.DEFAULT, tableIndex);
            } catch (Exception e) {
                throw new IOException("Unable to read FITS file:" + inf, e);
            }
        } else if (format == Format.JSON) {
            return JsonToDataGroup.parse(inf);
        } else {
            throw new IOException("Unsupported format, file:" + inf);
        }
    }

    public static DataGroup readAnyFormatHeader(File inf, Format ff) throws IOException {
        Format format = ff == null ? guessFormat(inf) : ff;
        DataGroup dg = null;

        if (format == Format.VO_TABLE) {
            dg = VoTableReader.voHeaderToDataGroup(inf.getAbsolutePath());
        } else if (format == Format.FITS) {
            dg = FitsHDUUtil.fitsHeaderToDataGroup(inf.getAbsolutePath());
        } else if (format == Format.CSV || format == Format.TSV || format == Format.IPACTABLE || format == Format.JSON) {
            String A = (format == Format.IPACTABLE) ? "IPAC Table" : format.toString();
            String title = String.format("%s", A);
            dg = new DataGroup(title, new ArrayList<DataType>());
        } else {
            dg = new DataGroup("invalid file format", new ArrayList<DataType>());
        }
        return dg;
    }

    public static Format guessFormat(File inf) throws IOException {

        String fileExt = FileUtil.getExtension(inf);
        if (fileExt != null) {
            if (fileExt.equalsIgnoreCase("tbl")) {
                return Format.IPACTABLE;
            } else if (fileExt.matches("xml|vot")) {
                return Format.VO_TABLE;
            } else if (fileExt.equalsIgnoreCase("csv")) {
                return Format.CSV;
            } else if (fileExt.equalsIgnoreCase("tsv")) {
                return Format.TSV;
            } else if (fileExt.equalsIgnoreCase("fits")) {
                return Format.FITS;
            } else if (fileExt.equalsIgnoreCase("json")) {
                return Format.JSON;
            }
        }

        int readAhead = 10;

        int row = 0;
        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        try {
            String line = reader.readLine();
            if (line.startsWith("{")) {
                return Format.JSON;
            } else if (line.startsWith("SIMPLE  = ")) {
                return Format.FITS;
            }

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
                } else if (line.startsWith("<VOTABLE")) {
                    return Format.VO_TABLE;
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
        } finally {
            try {reader.close();} catch (Exception e) {e.printStackTrace();}
        }

    }

    public static DataGroupPart getData(File inf, int start, int rows) throws IOException {
        TableDef tableDef = IpacTableUtil.getMetaInfo(inf);

        DataGroup dg = new DataGroup(null, tableDef.getCols());

        RandomAccessFile reader = new RandomAccessFile(inf, "r");
        long skip = ((long)start * (long)tableDef.getLineWidth()) + (long)tableDef.getRowStartOffset();
        int count = 0;
        try {
            reader.seek(skip);
            String line = reader.readLine();
            while (line != null && count < rows) {
                DataObject row = IpacTableUtil.parseRow(dg, line, tableDef);
                if (row != null) {
                    dg.add(row);
                    count++;
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        // sync attributes in datagroup with tabledef.
        Map<String, DataGroup.Attribute> attribs = dg.getAttributes();
        if (attribs.size() > 0) {
            tableDef.setKeywords(attribs.values());
        }
        dg.setKeywords(tableDef.getKeywords());

        long totalRow = tableDef.getLineWidth() == 0 ? 0 :
                        (inf.length()+1 - tableDef.getRowStartOffset())/tableDef.getLineWidth();
        return new DataGroupPart(tableDef, dg, start, (int) totalRow);
    }

//====================================================================
//
//====================================================================

    public static enum Format { TSV(CSVFormat.TDF), CSV(CSVFormat.DEFAULT), IPACTABLE(), UNKNOWN(), FIXEDTARGETS(), FITS(), JSON(), VO_TABLE();
        CSVFormat type;
        Format() {}
        Format(CSVFormat type) {this.type = type;}
    }

}

