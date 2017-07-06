/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.astro.IpacTableWriter;
import org.json.simple.JSONObject;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.TableHDU;
import nom.tam.util.Cursor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Date: Dec 5, 2011
 *
 * @author loi
 * @version $Id: VoTableUtil.java,v 1.4 2013/01/07 22:10:01 tatianag Exp $
 */
public class FitsHDUUtil {

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
            dt.setShortDesc(meta.getDescription());
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

                String name = "NoName";
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
                        if (key.equals("XTENSION")) {
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
                String title = index == 1 ? "A FITS file" :
                                            "A FITS with " + (index - 1) + ((index > 2) ? " extensions" : " extension");
                title = String.format("%s: the file size is %,d bytes." +
                                      "-- The following left table shows the file summary and the right table shows the header content of " +
                                      "the primary HDU or extension HDU which is highlighted in the file summary.", title, ff.length());

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
}
