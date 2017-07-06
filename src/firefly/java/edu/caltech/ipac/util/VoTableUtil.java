/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.server.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import uk.ac.starlink.table.*;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableWriter;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;


/**
 * Date: Dec 5, 2011
 *
 * @author loi
 * @version $Id: VoTableUtil.java,v 1.4 2013/01/07 22:10:01 tatianag Exp $
 */
public class VoTableUtil {

    public static DataGroup[] voToDataGroups(String voTableFile) {
        return voToDataGroups(voTableFile,false);
    }

    public static DataGroup[] voToDataGroups(String voTableFile, boolean headerOnly) {
        VOTableBuilder votBuilder = new VOTableBuilder();
        List<DataGroup> groups = new ArrayList<DataGroup>();
        try {
            //DataSource datsrc = DataSource.makeDataSource(voTableFile);
            //StoragePolicy policy = StoragePolicy.getDefaultPolicy();
            //TableSequence tseq = votBuilder.makeStarTables( datsrc, policy );
            StarTableFactory stFactory = new StarTableFactory();
            TableSequence tseq = stFactory.makeStarTables(voTableFile, null);

            for ( StarTable table; ( table = tseq.nextTable() ) != null; ) {
                DataGroup dg = convertToDataGroup(table, headerOnly);
                groups.add( dg );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return groups.toArray(new DataGroup[groups.size()]);
    }

    private enum MetaInfo {
        INDEX("Index", "Index", Integer.class, "table index"),
        TABLE("Table", "Table", String.class, "table name"),
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

    public static DataGroup voHeaderToDataGroup(String voTableFile) {
        List<DataType> cols = new ArrayList<DataType>();

        for ( MetaInfo meta : MetaInfo.values()) {    // index, name, row, column
            DataType dt = new DataType(meta.getKey(), meta.getTitle(), meta.getMetaClass());
            dt.setShortDesc(meta.getDescription());
            cols.add(dt);
        }
        DataGroup dg = new DataGroup("votable", cols);
        String invalidMsg = "invalid votable file";

        try {
            StarTableFactory stFactory = new StarTableFactory();
            TableSequence tseq = stFactory.makeStarTables(voTableFile, null);

            int index = 0;
            List<JSONObject> headerColumns = FitsHDUUtil.createHeaderTableColumns(true);

            for ( StarTable table; ( table = tseq.nextTable() ) != null; ) {
                String title = table.getName();
                Long rowNo = new Long(table.getRowCount());
                Integer columnNo = new Integer(table.getColumnCount());

                String tableName = String.format("%d cols x %d rows", columnNo, rowNo) ;
                List<List<String>> headerRows = new ArrayList<>();

                List<DescribedValue> tblParams = table.getParameters();
                int rowIdx = 0;

                List<String> rowStats = new ArrayList<>();
                rowStats.add(Integer.toString(rowIdx++));
                rowStats.add("Name");
                rowStats.add(title);
                rowStats.add("Table name");
                headerRows.add(rowStats);

                for (DescribedValue dv : tblParams) {
                    ValueInfo vInfo = dv.getInfo();

                    rowStats = new ArrayList<>();
                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add(vInfo.getName());
                    rowStats.add(dv.getValueAsString(200));

                    String desc = vInfo.getDescription();
                    if (desc == null) {
                        desc = "";
                    }
                    rowStats.add(desc);

                    headerRows.add(rowStats);
                }

                JSONObject voParamsHeader = FitsHDUUtil.createHeaderTable(headerColumns, headerRows,
                                               "Information of table with index " + index);


                DataObject row = new DataObject(dg);
                row.setDataElement(cols.get(0), index);
                row.setDataElement(cols.get(1),tableName );
                row.setDataElement(cols.get(2), "Table");
                row.setRowIdx(index);
                dg.add(row);
                dg.addAttribute(Integer.toString(index), voParamsHeader.toJSONString());
                index++;
            }

            if (index == 0) {
                throw new IOException(invalidMsg);
            } else {
                File ff = new File(voTableFile);

                String title = "A VOTable file with " + index + (index > 1 ? " tables" : " table");

                title = String.format("%s: the file size is %,d bytes." +
                        "-- The following left table shows the file summary and the right table shows the information of " +
                        "the table which is highlighted in the file summary.", title, ff.length());
                dg.setTitle(title);
            }
        } catch (IOException e) {
            dg.setTitle(invalidMsg);
            e.printStackTrace();
        }

        return dg;
    }


    private static DataGroup convertToDataGroup(StarTable table, boolean headerOnly) {
        String title = table.getName();
        List<DataType> cols = new ArrayList<DataType>();
        String raCol=null, decCol=null;
        int precision = 8;
        for (int i = 0; i < table.getColumnCount(); i++) {
            ColumnInfo cinfo = table.getColumnInfo(i);
            if(cinfo.getAuxDatum(VOStarTable.PRECISION_INFO)!=null){
                try{
                    precision = Integer.parseInt(cinfo.getAuxDatum(VOStarTable.PRECISION_INFO).toString());
                }catch (NumberFormatException e){
                    // problem with VOTable vinfo precision: should be numeric - keep default min precision
                    continue;
                }
            }
            DataType dt = new DataType(cinfo.getName(), cinfo.getName(),
                    cinfo.isArray() ? String.class : cinfo.getContentClass(),
                    null, cinfo.getUnitString(), false); // mayBeNull is false to use empty space instead of null
            String desc = cinfo.getDescription();
            if (desc != null) {
                dt.setShortDesc(desc.replace("\n", " "));
            }
            String ucd = cinfo.getUCD();
            if ( ucd != null) { // should we save all UCDs?
                if (ucd.equals("POS_EQ_RA_MAIN")) {
                    raCol = cinfo.getName();
                }
                if (ucd.equals("POS_EQ_DEC_MAIN")) {
                    decCol = cinfo.getName();
                }
            }
            cols.add(dt);
        }
        DataGroup dg = new DataGroup(title, cols);

        for (Object p : table.getParameters()) {
            DescribedValue dv = (DescribedValue)p;
            dg.addAttribute(dv.getInfo().getName(), dv.getValueAsString(50).replace("\n", " "));
        }
        if (raCol != null && decCol != null) {
            dg.addAttribute("POS_EQ_RA_MAIN", raCol);
            dg.addAttribute("POS_EQ_DEC_MAIN", decCol);
        }

        try {
            if (!headerOnly) {
                RowSequence rs = table.getRowSequence();
                while (rs.next()) {
                    DataObject row = new DataObject(dg);
                    for(int i = 0; i < cols.size(); i++) {
                        DataType dtype = cols.get(i);
                        Object val = rs.getCell(i);
                        String sval = table.getColumnInfo(i).formatValue(val, 1000);
                        if (dtype.getDataType().isAssignableFrom(String.class) && !(val instanceof String)) {
                            row.setDataElement(dtype, sval);   // array value
                        } else {
                            if (val instanceof Double && Double.isNaN((Double)val)) {
                                val = null;
                            }
                            row.setDataElement(dtype, val);
                        }
                        if (dtype.getFormatInfo().isDefault()) {
                            IpacTableUtil.guessFormatInfo(dtype, sval, precision);// precision min 8 can come from VOTable attribute 'precision' later on.
                        }
                        if (sval != null && sval.length() > dtype.getMaxDataWidth()) {
                            dtype.setMaxDataWidth(sval.length());
                        }
                    }
                    dg.add(row);
                }
            }
            dg.shrinkToFitData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dg;
    }


    public static void main(String args[]) {

        File inf = new File(args[0]);
        DataGroup[] groups = voToDataGroups(inf.getAbsolutePath(), false);
        if (groups != null) {
            for (DataGroup dg : groups) {
                try {
                    IpacTableWriter.save(new File(inf.getParent(), inf.getName() + "-" + dg.getTitle()), dg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
