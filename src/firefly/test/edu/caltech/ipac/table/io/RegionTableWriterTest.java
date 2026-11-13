/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * The region file a table is saved as must name the coordinate system of the table's position columns.
 */
public class RegionTableWriterTest extends ConfigTest {

    /**
     * @param lon      the longitude of the table's one row, in degrees
     * @param lat      the latitude of the table's one row, in degrees
     * @param coordSys the DS9 coordinate system keyword of the request, or null when the request has none
     * @return the region file written for the table
     */
    private static String write(double lon, double lat, String coordSys) throws IOException {
        DataGroup table = new DataGroup("positions",
                List.of(new DataType("lon", Double.class), new DataType("lat", Double.class)));
        table.add(new Object[]{lon, lat});

        StringWriter regionFile = new StringWriter();
        RegionTableWriter.write(regionFile, table, "lon,lat", coordSys);
        return regionFile.toString();
    }

    /**
     * The star Mizar is at galactic 113.11, 61.58, which is ra 200.98, dec 54.93 (HEASARC bsc5p, HR 5054).
     * Saved from its galactic columns, the region file must put the point at Mizar when the server reads the file
     * back (RegionParser, the reader behind a region file upload).
     */
    @Test
    public void galacticRegionFileReadsBackAtTheSameSkyPosition() throws IOException {
        String regionFile = write(113.11, 61.58, "GALACTIC");

        List<Region> regions = new RegionParser().processFile(new BufferedReader(new StringReader(regionFile))).regionList();
        Assert.assertEquals(1, regions.size());
        WorldPt mizar = VisUtil.convertToJ2000(regions.get(0).getPt());
        Assert.assertEquals(200.98, mizar.getLon(), 0.01);
        Assert.assertEquals(54.93, mizar.getLat(), 0.01);
    }

    @Test
    public void galacticTableIsWrittenAsGalactic() throws IOException {
        // the position is written as it is in the table, not converted
        Assert.assertEquals("GALACTIC; color=blue" + System.lineSeparator() + "point    120.5    -30.25 # 10 point=circle",
                write(120.5, -30.25, "GALACTIC"));
    }

    @Test
    public void eclipticAndB1950TablesAreWrittenInTheirOwnSystem() throws IOException {
        Assert.assertTrue(write(120.5, -30.25, "ECLIPTIC").startsWith("ECLIPTIC; color=blue"));
        Assert.assertTrue(write(120.5, -30.25, "B1950").startsWith("B1950; color=blue"));
    }

    @Test
    public void j2000TableIsWrittenAsJ2000() throws IOException {
        Assert.assertTrue(write(120.5, -30.25, "J2000").startsWith("J2000; color=blue"));
    }

    @Test
    public void requestWithoutCoordSysIsWrittenAsJ2000() throws IOException {
        Assert.assertTrue(write(120.5, -30.25, null).startsWith("J2000; color=blue"));
    }

    @Test
    public void keywordCaseIsIgnored() throws IOException {
        Assert.assertTrue(write(120.5, -30.25, "galactic").startsWith("GALACTIC; color=blue"));
    }

    @Test
    public void unknownCoordSysIsRejected() {
        // the keyword goes into the file, so text that is not a known keyword must not be written
        Assert.assertThrows(IOException.class, () -> write(120.5, -30.25, "SUPERGALACTIC"));
        Assert.assertThrows(IOException.class, () -> write(120.5, -30.25, "J2000\npoint 1 2"));
    }
}
