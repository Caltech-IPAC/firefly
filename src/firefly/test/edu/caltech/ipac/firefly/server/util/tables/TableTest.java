/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.table.DataType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

/**
 * General tests for table related functions
 */
public class TableTest {

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
