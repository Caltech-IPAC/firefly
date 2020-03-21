/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.TableHDU;
import nom.tam.fits.UndefinedHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.firefly.core.FileAnalysisReport.Type.*;


/**
 * Date: Dec 5, 2011
 *
 * @author loi
 * @version $Id: VoTableUtil.java,v 1.4 2013/01/07 22:10:01 tatianag Exp $
 */
public class FitsHDUUtil {
    private static final List<String> NAXIS_SET = Arrays.asList("naxis", "naxis1", "naxis2", "naxis3");

    public static FitsAnalysisReport analyze(File infile, FileAnalysisReport.ReportType type) throws Exception {
        FileAnalysisReport report = new FileAnalysisReport(type, TableUtil.Format.FITS.name(), infile.length(), infile.getPath());
        Header headerAry[];

        Fits fits = null;
        try {
            fits = new Fits(infile);
            BasicHDU[] parts = new Fits(infile).read();               // get the headers
            headerAry= new Header[parts.length];
            for(int i = 0; i < parts.length; i++) {
                FileAnalysisReport.Type ptype;
                int naxis= parts[i].getHeader().getIntValue("NAXIS");

                if (parts[i] instanceof CompressedImageHDU)  ptype= Image;
                else if (parts[i] instanceof ImageHDU)  ptype= Image;
                else if (parts[i] instanceof UndefinedHDU && naxis==1)  ptype= Image;
                else if (parts[i] instanceof TableHDU)  ptype= Table;
                else ptype= FileAnalysisReport.Type.Unknown;

                boolean isCompressed = (parts[i] instanceof CompressedImageHDU);

                Header header = parts[i].getHeader();
                headerAry[i]= header;

                if (ptype == Image && !hasGoodData(header)) {
                    ptype = HeaderOnly;
                }

                String desc = i == 0 ? "Primary" : null;
                if (desc == null) desc = header.getStringValue("EXTNAME");
                if (desc == null) desc = header.getStringValue("NAME");
                if (desc == null) desc = isCompressed ? "CompressedImage" : "NoName";

                FileAnalysisReport.Part part = new FileAnalysisReport.Part(ptype, desc);
                part.setIndex(i);
                part.setFileLocationIndex(i);
                if (ptype == Table) {
                    TableHDU<?> tHdu= (TableHDU<?>)parts[i];
                    desc = String.format("%s (%d cols x %d rows)", desc, tHdu.getNCols(), tHdu.getNRows());
                    part.setTotalTableRows(tHdu.getNRows());
                    part.setDesc(desc);
                }
                report.addPart(part);

                if (type == FileAnalysisReport.ReportType.Brief) {
                    break;
                } else if (type == FileAnalysisReport.ReportType.Details) {
                    part.setDetails(getDetails(i, header));
                }
            }
        }
        finally {
            try {
                if (fits!=null && fits.getStream()!=null) fits.getStream().close();
            } catch (IOException e) {
                // do nothing
            }
        }
        return new FitsAnalysisReport(report,headerAry);
    }

    private static boolean hasGoodData(Header header) {
        for (Cursor citr = header.iterator(); citr.hasNext(); ) {
            HeaderCard hc = (HeaderCard) citr.next();
            if (NAXIS_SET.contains(hc.getKey().toLowerCase()) && String.valueOf(hc.getValue()).equals("0")) return false;
        }
        return  true;
    }

    private static DataGroup getDetails(int idx, Header header) {
        DataType[] cols = new DataType[] {
                new DataType("#", Integer.class),
                new DataType("key", String.class),
                new DataType("value", String.class),
                new DataType("comment", String.class)
        };
        DataGroup dg = new DataGroup("Header of extension with index " + idx, cols);
        for (Cursor citr = header.iterator(); citr.hasNext(); ) {
            HeaderCard hc = (HeaderCard) citr.next();

            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], dg.size());
            row.setDataElement(cols[1], hc.getKey());
            row.setDataElement(cols[2], hc.getValue());
            row.setDataElement(cols[3], hc.getComment());
            dg.add(row);
        }
        return dg;
    }

    private enum MetaInfo {
        EXT("Index", "Index", Integer.class, "Extension Index"),
        NAME("Extension", "Extension", String.class, "Extension name"),
        TYPE("Type", "Type", String.class, "table type");

        String keyName;
        String title;
        Class  metaClass;
        String description;

        MetaInfo(String key, String title, Class c, String des) {
            this.keyName = key;
            this.title = title;
            this.metaClass = c;
            this.description = des;
        }

        List<Object> getInfo() {
            return Arrays.asList(keyName, title, metaClass, description);
        }

        String getKey() {
            return keyName;
        }

        String getTitle() {
            return title;
        }

        Class getMetaClass() {
            return metaClass;
        }

        String getDescription() {
            return description;
        }
    }

    public static List<JSONObject> createHeaderTableColumns(boolean bComment) {
        List<JSONObject> headerColumns = new ArrayList<>();
        String[] headerTblColName = new String[] {"#", "key", "value", "comment"};
        String[] headerTblColType = new String[] {"char", "int", "char", "char"};
        String[] headerTblDesc = new String[] {"index", "key", "value", "description"};

        int len = bComment ? headerTblColName.length : headerTblColName.length -1;
        for (int colIdx =0; colIdx < len; colIdx++) {
            JSONObject oneColumn = new JSONObject();

            oneColumn.put("name", headerTblColName[colIdx]);
            oneColumn.put("width", 30);
            oneColumn.put("type", headerTblColType[colIdx]);
            oneColumn.put("desc", headerTblDesc[colIdx]);
            headerColumns.add(oneColumn);
        }

        return headerColumns;
    }

    public static JSONObject createHeaderTable(List headerColumns, List headerRows, String title) {
        JSONObject extensionInfo = new JSONObject();
        JSONObject headerTableData = new JSONObject();

        headerTableData.put("columns", headerColumns);
        headerTableData.put("data", headerRows);
        extensionInfo.put("tableData", headerTableData);
        extensionInfo.put("tbl_id", "TABLE_HEADER");
        extensionInfo.put("totalRows", headerRows.size());
        extensionInfo.put("highlightedRow", 0);
        extensionInfo.put("title", title);

        return extensionInfo;
    }

    public static DataGroup fitsHeaderToDataGroup(String fitsFile) {
        List<DataType> cols = new ArrayList<DataType>();

        for (MetaInfo meta : MetaInfo.values()) {    // index, name, row, column
            DataType dt = new DataType(meta.getKey(), meta.getTitle(), meta.getMetaClass());
            dt.setDesc(meta.getDescription());
            cols.add(dt);
        }

        DataGroup dg = new DataGroup("fits", cols);
        String invalidMsg = "invalid fits file";

        try {
            Fits fits = new Fits(fitsFile); // open fits file
            BasicHDU hdu;
            int index = 0;
            List<JSONObject> headerColumns = createHeaderTableColumns(true);

            while ((hdu = fits.readHDU()) != null) {
                JSONObject extensionInfo;
                List<List<String>> headerRows = new ArrayList<>();

                Boolean isCompressed = (hdu instanceof CompressedImageHDU);
                String name = isCompressed ? "CompressedImage" : "NoName";
                String type = "IMAGE";

                Header hduHeader = hdu.getHeader();

                int rowIdx = 0;
                for (Cursor citr = hduHeader.iterator(); citr.hasNext(); rowIdx++ ) {
                    HeaderCard hc = (HeaderCard) citr.next();

                    if (!hc.isKeyValuePair()) continue;

                    List<String> rowStats = new ArrayList<>();
                    String key = hc.getKey();
                    String val = hc.getValue();
                    String comment = hc.getComment();

                    if (index == 0) {
                        if (key.equals("NAXIS") && val.equals("0")) {
                            type = "";
                        }
                    } else {
                        if (key.equals("XTENSION") && (!isCompressed)) {
                            type = val;
                        } else if (key.equals("NAME") || key.equals("EXTNAME")) {
                            name = val;
                        }
                    }
                    rowStats.add(Integer.toString(rowIdx));
                    rowStats.add(key);
                    rowStats.add(val);
                    rowStats.add(comment==null ? "" : comment);
                    headerRows.add(rowStats);
                }

                extensionInfo = createHeaderTable(headerColumns, headerRows,
                                                  "Header of extension with index " + index );

                DataObject row = new DataObject(dg);
                row.setDataElement(cols.get(0), index);
                if (type.toLowerCase().contains("table")){
                    TableHDU tHdu = (TableHDU)(fits.getHDU(index));
                    int colNo = tHdu.getNCols();
                    int rowNo = tHdu.getNRows();

                    name = String.format("%s/%d cols x %d rows", name, colNo, rowNo);
                }

                row.setDataElement(cols.get(1), (index == 0) ? "Primary" : name);
                row.setDataElement(cols.get(2), type);
                dg.add(row);
                dg.addAttribute(Integer.toString(index), extensionInfo.toJSONString());
                index++;
            }

            if (index == 0) {
                throw new FitsException(invalidMsg);
            } else {
                File ff = new File(fitsFile);
                //String title = index == 1 ? "A FITS file" :
                //                            "A FITS with " + (index - 1) + ((index > 2) ? " extensions" : " extension");
                String title = String.format("FITS" +
                                      "-- The following left table shows the file summary and the right table shows the header content of " +
                                      "the primary HDU or extension HDU which is highlighted in the file summary.");

                dg.setTitle(title);
            }
        }
        catch (FitsException e) {
            dg.setTitle(invalidMsg);
            e.printStackTrace();

        }
        catch (IOException e) {
            dg.setTitle(invalidMsg);
            e.printStackTrace();
        }

        return dg;
    }

    public static class FitsAnalysisReport {
        final FileAnalysisReport report;
        final Header[] headerAry;

        public FitsAnalysisReport(FileAnalysisReport report, Header[] headerAry) {
            this.report = report;
            this.headerAry = headerAry;
        }

        public FileAnalysisReport getReport() { return report; }
        public Header[] getHeaderAry() { return headerAry; }
    }
}
