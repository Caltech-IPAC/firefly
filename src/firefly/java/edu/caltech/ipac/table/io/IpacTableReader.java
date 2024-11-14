/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.TableUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static DataGroup read(File inf, TableServerRequest request, String... onlyColumns) throws IOException {
        return read(new FileInputStream(inf), SpectrumMetaInspector.hasSpectrumHint(request), onlyColumns);
    }

    public static DataGroup read(InputStream inputStream, String... onlyColumns) throws IOException {
        return read(inputStream, false, onlyColumns);
    }

    public static DataGroup read(InputStream inputStream, boolean searchForSpectrum,  String... onlyColumns) throws IOException {
        var handler = new TableParseHandler.Memory(false, searchForSpectrum);
        parseTable(handler, inputStream, onlyColumns);
        return handler.getTable(0);
    }

    public static void parseTable(TableParseHandler handler, File srcFile, String... onlyColumns) throws IOException {
        parseTable(handler, new FileInputStream(srcFile), onlyColumns);
    }

    public static void parseTable(TableParseHandler handler, InputStream inputStream, String... onlyColumns) throws IOException {
        String line = null;
        int lineNum = 0;
        var bufferedReader = new BufferedReader(new InputStreamReader(inputStream), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        try (bufferedReader) {
            handler.start();
            handler.startTable(0);

            IpacTableDef tableDef = IpacTableUtil.getMetaInfo(bufferedReader);
            DataGroup headers = create(tableDef, onlyColumns);
            handler.header(headers);

            if (!handler.headerOnly()) {
                lineNum = tableDef.getExtras() == null ? 0 : tableDef.getExtras().getKey();

                line = tableDef.getExtras() == null ? bufferedReader.readLine() : tableDef.getExtras().getValue();
                lineNum++;
                DataType[] cols = headers.getDataDefinitions();
                TableUtil.ParsedColInfo[] parsedInfos = Arrays.stream(cols)
                        .map(dt -> tableDef.getParsedInfo(dt.getKeyName()))
                        .toArray(TableUtil.ParsedColInfo[]::new);
                while (line != null) {
                    Object[] row = IpacTableUtil.parseRow(line, cols, parsedInfos);
                    if (row != null)    handler.data(row);
                    line = bufferedReader.readLine();
                    lineNum++;
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage() + "<br>on line " + lineNum + ": " + line;
            if (msg.length() > 128) msg = msg.substring(0, 128) + "...";
            logger.error(e, "on line " + lineNum + ": " + line);
            throw new IOException(msg);
        } finally {
            handler.endTable(0);
            handler.end();
        }
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

    private static DataGroup create(IpacTableDef tableDef, String... onlyColumns) {
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

        // if fixlen != T, don't guess format
        if (!String.valueOf(tableDef.getAttribute("fixlen")).trim().equals("T")) {
            tableDef.getCols().forEach(c -> {
                tableDef.getParsedInfo(c.getKeyName()).formatChecked = true;
            });
        }
        if (tableDef.getRowCount() > 0) outData.setInitCapacity(tableDef.getRowCount());        // set initial capacity to avoid resizing

        return outData;
    }


}
