/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.astro.FITSTableReader;
import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;

import nom.tam.fits.FitsException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class DataGroupReadTest {

    private static final File largeIpacTable = new File("/hydra/cm/firefly_test_data/large-ipac-tables/wise-950000.tbl");
    private static final File midIpacTable = FileLoader.resolveFile(DataGroupReadTest.class, "50k.tbl");
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
        Assert.assertEquals("column unit", "deg", ra.getDataUnit());
        Assert.assertEquals("column desc", "null", ra.getNullString());
        Assert.assertEquals("column type", "double", ra.getTypeDesc());

        // test col attributes
        String width = data.getAttribute(IpacTableUtil.makeAttribKey(IpacTableUtil.WIDTH_TAG, "designation")).getValue();
        String prefWidth = data.getAttribute(IpacTableUtil.makeAttribKey(IpacTableUtil.PREF_WIDTH_TAG, "designation")).getValue();
        String sortable = data.getAttribute(IpacTableUtil.makeAttribKey(IpacTableUtil.SORTABLE_TAG, "designation")).getValue();
        String filterable = data.getAttribute(IpacTableUtil.makeAttribKey(IpacTableUtil.FILTERABLE_TAG, "designation")).getValue();
        String visibility = data.getAttribute(IpacTableUtil.makeAttribKey(IpacTableUtil.VISI_TAG, "designation")).getValue();
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
    public void perfTestMidSize() throws IOException, IpacTableException {
        tableIoTest(midIpacTable);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestLargeSize() throws IOException, IpacTableException {
        tableIoTest(largeIpacTable);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void memTest() throws IOException {
        Runtime.getRuntime().gc();
        long beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();
        int testSize = 5000000;

        List<Object> objects = new ArrayList<>(100);
        for (int i=0; i< testSize; i++) {
            objects.add(i + Math.random());
        }

        long elapsed = System.currentTimeMillis() - start;
        long usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        long usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("List<Object>(%,d):  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs %n", testSize, usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0);

        //-------------------

        beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        start = System.currentTimeMillis();
        Object[] objectAry = new Object[testSize];
        for (int i=0; i< testSize; i++) {
            objectAry[i] = i + Math.random();
        }

        elapsed = System.currentTimeMillis() - start;
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("Object[](%,d)    :  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs  total:%.2fsecs %n", testSize, usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0, (System.currentTimeMillis()-start)/1000.0);

        //-------------------

        beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        start = System.currentTimeMillis();
        List<Double> doubles = new ArrayList<>(100);
        for (int i=0; i< testSize; i++) {
            doubles.add(i + Math.random());
        }

        elapsed = System.currentTimeMillis() - start;
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("List<Double>(%,d):  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs %n", testSize, usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0);

        //-------------------

        beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        start = System.currentTimeMillis();
        double[] doubleAry = new double[testSize];
        for (int i=0; i< testSize; i++) {
            doubleAry[i] = i + Math.random();
        }

        elapsed = System.currentTimeMillis() - start;
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("double[](%,d)    :  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs  total:%.2fsecs %n", testSize, usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0, (System.currentTimeMillis()-start)/1000.0);


        //-------------------

        beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        start = System.currentTimeMillis();
        int[] intAry = new int[testSize];
        for (int i=0; i< testSize; i++) {
            intAry[i] = (int) (i + Math.random());
        }

        elapsed = System.currentTimeMillis() - start;
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("int[](%,d)       :  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs  total:%.2fsecs %n", testSize, usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0, (System.currentTimeMillis()-start)/1000.0);

        //-------------------

        beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        start = System.currentTimeMillis();
        String[] strAry = new String[testSize];
        for (int i=0; i< testSize; i++) {
            strAry[i] = String.valueOf(i%10);
        }

        elapsed = System.currentTimeMillis() - start;
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("strAry[](%,d)      :  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs  total:%.2fsecs %n", testSize, usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0, (System.currentTimeMillis()-start)/1000.0);

        //-------------------

        beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        start = System.currentTimeMillis();
        List<String> strList = new ArrayList<>();
        for (int i=0; i< testSize; i++) {
            strList.add(String.valueOf(i%10));
        }

        elapsed = System.currentTimeMillis() - start;
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("strList[](%,d)       :  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs  total:%.2fsecs %n", testSize, usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0, (System.currentTimeMillis()-start)/1000.0);

        //-------------------

        Object v = objects.get(0);
        v = objectAry[0];
        v = doubles.get(0);
        v = doubleAry[0];
        v = intAry[0];
        v = strAry[0];
        v = strList.get(0);

    }

    private static void tableIoTest(File inFile) throws IOException, IpacTableException {
        Runtime.getRuntime().gc();
        long beginMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();

        DataGroup data = IpacTableReader.readIpacTable(inFile, "dummy");
        long elapsed = System.currentTimeMillis() - start;
        long usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        Runtime.getRuntime().gc();
        long usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - beginMem;
        System.out.printf("READ(%,d):  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs %n", data.size(), usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0);

        IpacTableWriter.save(new File("./tmp.tbl"), data);
        usedB4Gc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - usedAfGc;
        Runtime.getRuntime().gc();
        usedAfGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - usedAfGc;
        elapsed = System.currentTimeMillis() - start - elapsed;
        System.out.printf("WRITE(%,d):  Memory temp=%.2fMB  final:%.2fMB  elapsed:%.2fsecs  total:%.2fsecs %n", data.size(), usedB4Gc/1000/1000.0, usedAfGc/1000/1000.0, elapsed/1000.0, (System.currentTimeMillis()-start)/1000.0);

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
