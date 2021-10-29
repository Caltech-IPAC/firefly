/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.TableUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * read in the file in IPAC table format
 *
 * @author Xiuqin Wu
 */

public final class IpacTableReader {

    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static DataGroup read(File inf, String... onlyColumns) throws IOException {
       return read(inf,null, onlyColumns) ;
    }

    public static DataGroup read(File inf, Map<String, String> passedMetaInfo, String... onlyColumns) throws IOException {
        IpacTableDef tableDef = IpacTableUtil.getMetaInfo(inf);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        return doRead(bufferedReader, passedMetaInfo, tableDef, onlyColumns);
    }

    public static DataGroup read(InputStream inputStream, String... onlyColumns) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        IpacTableDef tableDef = IpacTableUtil.getMetaInfo(bufferedReader);
        return  doRead(bufferedReader, null, tableDef, onlyColumns);
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

    static DataGroup doRead(BufferedReader bufferedReader, Map<String, String> passedMetaInfo,
                            IpacTableDef tableDef, String... onlyColumns) throws IOException {

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

        outData.getTableMeta().setKeywords(attributes);
        IpacTableUtil.consumeColumnInfo(outData);   // move column attributes into columns

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
        String dataTypeHint= passedMetaInfo !=null ? passedMetaInfo.getOrDefault(MetaConst.DATA_TYPE_HINT,"").toLowerCase() : "";
        SpectrumMetaInspector.searchForSpectrum(outData,dataTypeHint.equals("spectrum"));
        outData.trimToSize();
        return outData;
    }

    public static FileAnalysisReport analyze(File infile, FileAnalysisReport.ReportType type) throws IOException {
        IpacTableDef meta = IpacTableUtil.getMetaInfo(infile);
        FileAnalysisReport report = new FileAnalysisReport(type, TableUtil.Format.IPACTABLE.name(), infile.length(), infile.getPath());
        FileAnalysisReport.Part part = new FileAnalysisReport.Part(FileAnalysisReport.Type.Table, String.format("IPAC Table (%d cols x %s rows)", meta.getCols().size(), meta.getRowCount()));
        part.setTotalTableRows(meta.getRowCount());
        report.addPart(part);
        if (type.equals(FileAnalysisReport.ReportType.Details)) {
            part.setDetails(TableUtil.getDetails(0, meta));
        }
        return report;
    }

}
