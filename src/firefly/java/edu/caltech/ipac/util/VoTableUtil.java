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
        INDEX("Index", "Index", Integer.class, "table index", false),
        TABLE("Table", "Table", String.class, "table name", false),
        TYPE("Type", "Type", String.class, "table type", false),
        NAME("Name", "Name", String.class, "table name", true),
        ROW("Rows", "Total Rows", Long.class, "total rows", true),
        COLUMN("Columns", "Total Columns", Integer.class, "total columns", true);

        String keyName;
        String title;
        Class  metaClass;
        String description;
        boolean bRowInfo;

        MetaInfo(String key, String title, Class c, String des, boolean rowInfo) {
            this.keyName = key;
            this.title = title;
            this.metaClass = c;
            this.description = des;
            this.bRowInfo = rowInfo;
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

        JSONObject addEntryToJson(Object value) {
            JSONObject oneObj = new JSONObject();

            oneObj.put("key", getTitle());
            oneObj.put("value", value);
            return oneObj;
        }

        boolean isRowInfo() { return bRowInfo; }

    }

    public static DataGroup voHeaderToDataGroup(String voTableFile) {
        List<DataType> cols = new ArrayList<DataType>();

        for ( MetaInfo meta : MetaInfo.values()) {    // index, name, row, column
            if (meta.isRowInfo()) continue;
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
            List<JSONObject> rowDetails = new ArrayList<>();

            for ( StarTable table; ( table = tseq.nextTable() ) != null; ) {
                String title = table.getName();
                Long rowNo = new Long(table.getRowCount());
                Integer columnNo = new Integer(table.getColumnCount());
                String tableId = "table-" + index;

                JSONObject rowInfo = new JSONObject();

                //{rowId: , rowInfo: [{name: }, {total_row: }, {total_column: }]}
                rowInfo.put("rowId", index);

                List<JSONObject> rowStats = new ArrayList<>();
                JSONObject oneStat;
                MetaInfo meta;

                meta = MetaInfo.NAME;
                oneStat = meta.addEntryToJson(title);
                rowStats.add(oneStat);

                meta = MetaInfo.ROW;
                oneStat = meta.addEntryToJson(rowNo);
                rowStats.add(oneStat);

                meta = MetaInfo.COLUMN;
                oneStat = meta.addEntryToJson(columnNo);
                rowStats.add(oneStat);

                rowInfo.put("rowInfo", rowStats);
                rowDetails.add(rowInfo);

                DataObject row = new DataObject(dg);
                row.setDataElement(cols.get(0), index);
                row.setDataElement(cols.get(1),tableId );
                row.setDataElement(cols.get(2), "Table");
                row.setRowIdx(index);
                dg.add(row);
                dg.addAttribute(Integer.toString(index), rowInfo.toJSONString());
                index++;
            }

            if (index == 0) {
                throw new IOException(invalidMsg);
            } else {
                dg.setTitle("a votable with " + index + (index > 1 ? " tables" : " table"));
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
