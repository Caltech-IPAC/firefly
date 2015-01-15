/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.util.DataSource;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import edu.caltech.ipac.astro.IpacTableWriter;

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
            DataSource datsrc = DataSource.makeDataSource(voTableFile);
            StoragePolicy policy = StoragePolicy.getDefaultPolicy();
            TableSequence tseq = votBuilder.makeStarTables( datsrc, policy );
            for ( StarTable table; ( table = tseq.nextTable() ) != null; ) {
                //System.out.println("table found:" + table.getName());
                DataGroup dg = convertToDataGroup(table, headerOnly);
                groups.add( dg );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return groups.toArray(new DataGroup[groups.size()]);
    }

    private static DataGroup convertToDataGroup(StarTable table, boolean headerOnly) {
        String title = table.getName();
        List<DataType> cols = new ArrayList<DataType>();
        String raCol=null, decCol=null;
        for (int i = 0; i < table.getColumnCount(); i++) {
            ColumnInfo cinfo = table.getColumnInfo(i);
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
        dg.beginBulkUpdate();

        for (Object p : table.getParameters()) {
            DescribedValue dv = (DescribedValue)p;
            dg.addAttributes(new DataGroup.Attribute(dv.getInfo().getName(), dv.getValueAsString(50)));
        }
        if (raCol != null && decCol != null) {
            dg.addAttributes(new DataGroup.Attribute("POS_EQ_RA_MAIN", raCol));
            dg.addAttributes(new DataGroup.Attribute("POS_EQ_DEC_MAIN", decCol));
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
                            IpacTableUtil.guessFormatInfo(dtype, sval);
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
        } finally {
            dg.endBulkUpdate();
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
