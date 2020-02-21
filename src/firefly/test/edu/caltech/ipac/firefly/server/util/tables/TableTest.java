/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.io.IpacTableReader;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.table.JsonTableUtil.getPathValue;
import static org.junit.Assert.assertEquals;

/**
 * General tests for table related functions
 */
public class TableTest extends ConfigTest {

    @BeforeClass
    public static void setUp() {
        // needed by test testGetSelectedData because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
    }

    @Test
    public void testFormat() {
        DataType dt = new DataType("aString", String.class);

    // transform a string using format.. surround it with '()' w/ fixed width
        dt.setPrecision("F3");      // order of precedence... ignored because format is already set
        dt.setFormat("(%s)");
        dt.setWidth(6);
        String v = dt.formatFixedWidth("abc");
        assertEquals("(abc) ", v);

        dt.setFmtDisp("z-%s");
        v = dt.format("abc");
        assertEquals("z-abc", v);   // order of precedence... FmtDisp is highest precedence

    // using precision
        dt = new DataType("aNum", Double.class);
        dt.setPrecision("3");                   // without the T spec, should defaults to F
        v = dt.format(12345.12345);
        assertEquals("12345.123", v);

        dt.setPrecision("F3");                   // F spec
        v = dt.format(12345.12345);
        assertEquals("12345.123", v);

        dt.setPrecision("f3");                   // lowercase f spec .. F is case insensitive
        v = dt.format(12345.12345);
        assertEquals("12345.123", v);

        dt.setPrecision("E3");                   // E spec
        v = dt.format(12345.12345);
        assertEquals("1.235E+04", v);

        dt.setPrecision("G3");                   // G spec
        v = dt.format(12345.12345);
        assertEquals("1.23E+04", v);

        dt.setPrecision("g3");                   // lowercase G spec
        v = dt.format(12345.12345);
        assertEquals("1.23e+04", v);

        dt.setPrecision("G3");                   // G spec without loss of precision
        v = dt.format(12.1);
        assertEquals("12.1", v);

        dt = new DataType("aString", Integer.class);
        dt.setPrecision("F3");                   // precision should be ignored for type other than floating point numbers
        v = dt.format(123);
        assertEquals("123", v);

    }


    /**
     * Ensure column meta from syntax `col.*.[prop]` are properly preserved during SearchProcessor's
     * execution cycle.. source -> DataGroup -> db -> DataGroup -> json
     */
    @Test
    public void testColumnMeta() {
        String testTable = "\\col.ra.Visibility = hidden\n" +
                "\\col.ra.label = Label\n" +
                "\\col.ra.width = 30\n" +
                "\\col.ra.prefWidth = 40\n" +
                "\\col.ra.desc = Desc\n" +
                "\\col.ra.nullString = NullStr\n" +
                "\\col.ra.units = Unit\n" +
                "\\col.ra.format = Format\n" +
                "\\col.ra.fmtDisp = FmtDisp\n" +
                "\\col.ra.sortable = false\n" +
                "\\col.ra.filterable = false\n" +
                "\\col.ra.sortByCols = SortByCols\n" +
                "\\col.ra.enumVals = EnumVals\n" +
                "\\col.ra.precision = E7\n" +
                "\\col.ra.UCD = UCD\n" +
                "\\col.ra.utype = UType\n" +
                "\\col.ra.ref = Ref\n" +
                "\\col.ra.minValue = MinValue\n" +
                "\\col.ra.maxValue = MaxValue\n" +
                "\\col.dec.ShortDescription = ShortDescription\n" +
                "\\\n" +
                "|ra         |dec       |\n" +
                "|double     |double    |\n" +
                "|deg        |rad       |\n" +
                "|-999       |-999      |\n" +
                " 1.234567890 1.23456789 ";

        EmbeddedDbProcessor proc = new EmbeddedDbProcessor() {
            public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
                try {
                return IpacTableReader.read(new ByteArrayInputStream(testTable.getBytes()));
                } catch (Exception e) {
                    throw new DataAccessException(e.getMessage(), e);
                }
            }
        };

        DataGroup dg = null;
        try {
            dg = proc.getData(new TableServerRequest("dummyId")).getData();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        JSONObject jsonTable = JsonTableUtil.toJsonDataGroup(dg);

        JSONObject ra = (JSONObject) getPathValue(jsonTable, "tableData", "columns", "0");    // 1st col.. ra
        Assert.assertEquals("ra", ra.get("name"));
        Assert.assertEquals("Label", ra.get("label"));
        Assert.assertEquals("double", ra.get("type"));
        Assert.assertEquals("Unit", ra.get("units"));
        Assert.assertEquals("NullStr", ra.get("nullString"));
        Assert.assertEquals("Desc", ra.get("desc"));
        Assert.assertEquals(30, ra.get("width"));
        Assert.assertEquals(40, ra.get("prefWidth"));
        Assert.assertEquals(false, ra.get("sortable"));
        Assert.assertEquals(false, ra.get("filterable"));
        Assert.assertEquals("hidden", ra.get("visibility"));
        Assert.assertEquals("SortByCols", ra.get("sortByCols"));
        Assert.assertEquals("EnumVals", ra.get("enumVals"));
        Assert.assertEquals("E7", ra.get("precision"));
        Assert.assertEquals("UCD", ra.get("UCD"));
        Assert.assertEquals("UType", ra.get("utype"));
        Assert.assertEquals("Ref", ra.get("ref"));
        Assert.assertEquals("MaxValue", ra.get("maxValue"));
        Assert.assertEquals("MinValue", ra.get("minValue"));
        Assert.assertEquals(null, ra.get("format"));        // due to FIREFLY-471:  when precision is set; clear format and fmtDisp
        Assert.assertEquals(null, ra.get("fmtDisp"));       // due to FIREFLY-471:  when precision is set; clear format and fmtDisp

        JSONObject dec = (JSONObject) getPathValue(jsonTable, "tableData", "columns", "1");    // 2nd col.. dec
        Assert.assertEquals("ShortDescription", dec.get("desc"));        // testing old-meta ShortDescription
    }


//====================================================================
//  Not part of main test.
//====================================================================

    @Category({TestCategory.Perf.class})
    @Test
    public void testFormatPerf() {
        int count = 100000 * 100;       // assume 100k rows with 100 columns requiring format

        System.out.println("For comparison:  Test ran 11/16/2018 on MacBook Pro (2018) w 32 GB DDR4 \n" +
                "Using no format: 457 msecs elapsed \n" +
                "Using format '.3f': 6302 msecs elapsed \n" +
                "Using format '.3G': 6335 msecs elapsed \n" +
                "Using precision 'G3': 6852 msecs elapsed \n" +
                "Using fixedWidth '.3f': 6634 msecs elapsed \n"
        );

        DataType dt = new DataType(null, Long.class);
        long elapsed = System.currentTimeMillis();
        for (int i=0; i < count; i++) {
            String v = dt.format(12345 + i);
        }
        System.out.printf("Using no format: %d msecs elapsed %n", System.currentTimeMillis() - elapsed);

        dt = new DataType(null, Double.class);
        dt.setFormat("(%.3f)");
        elapsed = System.currentTimeMillis();
        for (int i=0; i < count; i++) {
            String v = dt.format(12345.123 + i);
        }
        System.out.printf("Using format '.3f': %d msecs elapsed %n", System.currentTimeMillis() - elapsed);

        dt = new DataType(null, Double.class);
        dt.setFormat("(%.3G)");
        elapsed = System.currentTimeMillis();
        for (int i=0; i < count; i++) {
            String v = dt.format(12345.231 + i);
        }
        System.out.printf("Using format '.3G': %d msecs elapsed %n", System.currentTimeMillis() - elapsed);

        dt = new DataType(null, Double.class);
        dt.setPrecision("G3");      // order of precedence... ignored because format is already set
        elapsed = System.currentTimeMillis();
        for (int i=0; i < count; i++) {
            String v = dt.format(12345.312 + i);
        }
        System.out.printf("Using precision 'G3': %d msecs elapsed %n", System.currentTimeMillis() - elapsed);

        dt = new DataType(null, Double.class);
        dt.setFormat("(%.3f)");
        dt.setWidth(15);
        elapsed = System.currentTimeMillis();
        for (int i=0; i < count; i++) {
            String v = dt.formatFixedWidth(12345.1231 + i);
        }
        System.out.printf("Using fixedWidth '.3f': %d msecs elapsed %n", System.currentTimeMillis() - elapsed);

    }

}
