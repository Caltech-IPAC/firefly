/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.astro.FITSTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;

import nom.tam.fits.FitsException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class DataGroupReadTest {

    private static final File largeIpacTable = new File("/hydra/cm/firefly_test_data/large-ipac-tables/wise-950000.tbl"); //FileLoader.resolveFile(DataGroupReadTest.class, "50k.tbl");
    private static final File ipacTable = FileLoader.resolveFile(DataGroupReadTest.class, "DataGroupTest.tbl");
    private static final File voTable = FileLoader.resolveFile(DataGroupReadTest.class, "p3am_cdd.xml");
    private static final File fitsTable = FileLoader.resolveFile(DataGroupReadTest.class, "lsst-table.fits");

    @Test
    public void ipacTable() throws IOException {
        DataGroup data = DataGroupReader.read(ipacTable);
        Assert.assertNotNull(data);

        // test col header
        DataType ra = data.getDataDefintion("ra");
        Assert.assertEquals("column name", "ra", ra.getKeyName());
        Assert.assertEquals("column unit", "deg", ra.getUnits());
        Assert.assertEquals("column desc", "null", ra.getNullString());
        Assert.assertEquals("column type", "double", ra.getTypeDesc());

        // test col attributes
        String width = data.getAttribute(TableMeta.makeAttribKey(TableMeta.WIDTH_TAG, "designation"));
        String prefWidth = data.getAttribute(TableMeta.makeAttribKey(TableMeta.PREF_WIDTH_TAG, "designation"));
        String sortable = data.getAttribute(TableMeta.makeAttribKey(TableMeta.SORTABLE_TAG, "designation"));
        String filterable = data.getAttribute(TableMeta.makeAttribKey(TableMeta.FILTERABLE_TAG, "designation"));
        String visibility = data.getAttribute(TableMeta.makeAttribKey(TableMeta.VISI_TAG, "designation"));
        Assert.assertEquals("column width", "100", width);
        Assert.assertEquals("column prefWidth", "10", prefWidth);
        Assert.assertEquals("column sortable", "false", sortable);
        Assert.assertEquals("column filterable", "true", filterable);
        Assert.assertEquals("column visibility", "hidden", visibility);

        // random value test
        Assert.assertEquals("cell (ra, 4)", 10.744471, data.get(4).getDataElement("ra"));           // in the middle
        Assert.assertEquals("cell (h_msigcom, 0)", null, data.get(0).getDataElement("h_msigcom"));  // first row with null value
        Assert.assertEquals("cell (err_min, 15)", 0.29, data.get(15).getDataElement("err_min"));    // last row
    }

    @Test
    public void voTable() throws IOException {
        DataGroup data = VoTableUtil.voToDataGroups(voTable.getPath())[0];
        Assert.assertNotNull(data);
        Assert.assertEquals("Number of rows", 40, data.size());
        Assert.assertEquals("Number of columns", 20, data.getDataDefinitions().length);

        // random value test
        Assert.assertEquals("cell (magzip2, 0)", 0.006, data.get(0).getDataElement("magzip2"));       // first row
        Assert.assertEquals("cell (magzip, 9)", 19.5, data.get(9).getDataElement("magzip"));       // in the middle
        Assert.assertEquals("cell (magzip, 39)", 13.0, data.get(39).getDataElement("magzip"));       // last row
    }

    @Test
    public void fitsTable() throws IOException, FitsException {
        DataGroup data = FITSTableReader.convertFitsToDataGroup(fitsTable.getPath(), null, null, null, 0);
        Assert.assertNotNull(data);
        Assert.assertEquals("Number of rows", 765, data.size());
        Assert.assertEquals("Number of columns", 52, data.getDataDefinitions().length);

        // random value test
        Assert.assertEquals("cell (coord_ra, 0)", 6.13602424, getDouble(data.get(0).getDataElement("coord_ra")), 0.00000001);       // first row
        Assert.assertEquals("cell (footprint, 24)", 25, data.get(24).getDataElement("footprint"));       // in the middle
        Assert.assertEquals("cell (base_GaussianCentroid_x, 764)", 850.043863, getDouble(data.get(764).getDataElement("base_GaussianCentroid_x")), 0.000001);       // last row
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTest() throws IOException {

        Runtime.getRuntime().gc();
        long beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();

        DataGroup data = DataGroupReader.read(largeIpacTable);
        long elapsed = System.currentTimeMillis() - start;
        long usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        long usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("READ:  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs %n", usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0);

        IpacTableWriter.save(new File("./tmp.tbl"), data);
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - usedAfGc;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - usedAfGc;
        elapsed = System.currentTimeMillis() - start - elapsed;
        System.out.printf("WRITE:  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs  total:%.2fsecs %n", usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0, (System.currentTimeMillis()-start)/1000.0);

        System.out.println("");
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            MemoryUsage peak = pool.getPeakUsage();
            System.out.printf("Peak %s memory used: %,d %n", pool.getName(), peak.getUsed());
        }
    }


    private static double getDouble(Object val) {
        if (val instanceof Double) {
            return (Double) val;
        } else {
            return Double.parseDouble(String.valueOf(val));
        }
    }

}
