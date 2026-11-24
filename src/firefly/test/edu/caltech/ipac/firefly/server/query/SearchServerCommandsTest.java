/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import org.junit.Assert;
import org.junit.Test;

import static edu.caltech.ipac.firefly.server.query.SearchServerCommands.TableSearchSpatialBinary.populateToLittleEndianByte;
import static edu.caltech.ipac.firefly.server.query.SearchServerCommands.TableSearchSpatialBinary.wrapLongitude;

public class SearchServerCommandsTest {

    private static final double DELTA = 1e-7;

    /**
     * Pack a longitude the way TableSearchSpatialBinary does, then read it the way the browser does:
     * four bytes, little endian, as an unsigned integer scaled by 10^7.
     */
    private static double packThenReadAsBrowser(double lon) {
        double pow = Math.pow(10, 7);
        byte[] bytes = new byte[4];
        populateToLittleEndianByte((long) (wrapLongitude(lon) * pow), bytes, 0);
        long unsigned = (bytes[0] & 0xffL) | ((bytes[1] & 0xffL) << 8) | ((bytes[2] & 0xffL) << 16) | ((bytes[3] & 0xffL) << 24);
        return unsigned / pow;
    }

    @Test
    public void testWrapLongitude() {
        Assert.assertEquals(315.042, wrapLongitude(315.042), DELTA);
        Assert.assertEquals(315.042, wrapLongitude(-44.958), DELTA);
        Assert.assertEquals(10, wrapLongitude(370), DELTA);
        Assert.assertEquals(0, wrapLongitude(0), DELTA);
    }

    @Test
    public void testBrowserReadsNegativeLongitude() {
        // a galactic or ecliptic longitude can be negative, a right ascension is not
        Assert.assertEquals(315.042, packThenReadAsBrowser(-44.958), DELTA);
    }
}
