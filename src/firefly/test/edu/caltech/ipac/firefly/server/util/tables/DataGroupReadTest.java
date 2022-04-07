/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.PrimitiveList;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.table.io.VoTableReader;
import nom.tam.fits.FitsException;
import org.apache.logging.log4j.Level;
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
import java.util.concurrent.atomic.AtomicReference;

import static edu.caltech.ipac.firefly.TestUtil.getDataFile;
import static edu.caltech.ipac.firefly.TestUtil.logMemUsage;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class DataGroupReadTest {

    private static final File largeIpacTable = getDataFile("large-ipac-tables/wise-950000.tbl");
    private static final File midIpacTable = getDataFile(DataGroupReadTest.class, "50k.tbl");
    private static final File ipacTable = getDataFile(DataGroupReadTest.class, "DataGroupTest.tbl");
    private static final File voTable = getDataFile(DataGroupReadTest.class, "p3am_cdd.xml");
    private static final File fitsTable =getDataFile(DataGroupReadTest.class, "lsst-table.fits");

    @Test
    public void ipacTable() throws IOException {
        DataGroup data = IpacTableReader.read(ipacTable);
        Assert.assertNotNull(data);

        // test col header
        DataType ra = data.getDataDefintion("ra");
        Assert.assertEquals("column name", "ra", ra.getKeyName());
        Assert.assertEquals("column unit", "deg", ra.getUnits());
        Assert.assertEquals("column desc", "null", ra.getNullString());
        Assert.assertEquals("column type", "double", ra.getTypeDesc());

        // test col info
        DataType desig = data.getDataDefintion("designation");
        Assert.assertEquals("column width", 100, desig.getWidth());
        Assert.assertEquals("column prefWidth", 10, desig.getPrefWidth());
        Assert.assertEquals("column sortable", false, desig.isSortable());
        Assert.assertEquals("column filterable", true, desig.isFilterable());
        Assert.assertEquals("column visibility", DataType.Visibility.hidden, desig.getVisibility());

        // random value test
        Assert.assertEquals("cell (ra, 4)", 10.744471, data.get(4).getDataElement("ra"));           // in the middle
        Assert.assertEquals("cell (h_msigcom, 0)", null, data.get(0).getDataElement("h_msigcom"));  // first row with null value
        Assert.assertEquals("cell (err_min, 15)", 0.29, data.get(15).getDataElement("err_min"));    // last row
    }

    @Test
    public void voTable() throws IOException {
        DataGroup data = VoTableReader.voToDataGroups(voTable.getPath())[0];
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
        DataGroup data = FITSTableReader.convertFitsToDataGroup(fitsTable.getPath(), null, FITSTableReader.DEFAULT, 0);
        Assert.assertNotNull(data);
        Assert.assertEquals("Number of rows", 765, data.size());
        Assert.assertEquals("Number of columns", 52, data.getDataDefinitions().length);

        // random value test
        Assert.assertEquals("cell (coord_ra, 0)", 6.13602424, getDouble(data.get(0).getDataElement("coord_ra")), 0.00000001);       // first row
        Assert.assertEquals("cell (footprint, 24)", 25, data.get(24).getDataElement("footprint"));       // in the middle
        Assert.assertEquals("cell (base_GaussianCentroid_x, 764)", 850.043863, getDouble(data.get(764).getDataElement("base_GaussianCentroid_x")), 0.000001);       // last row
    }

    @Test
    public void cloneWithoutData() throws IOException, FitsException {
        DataGroup data = IpacTableReader.read(ipacTable);
        DataGroup newClone = data.cloneWithoutData();
        Assert.assertEquals("Number of rows", 0, newClone.size());

        // test col header
        DataType ra = newClone.getDataDefintion("ra");
        Assert.assertEquals("column name", "ra", ra.getKeyName());
        Assert.assertEquals("column unit", "deg", ra.getUnits());
        Assert.assertEquals("column desc", "null", ra.getNullString());
        Assert.assertEquals("column type", "double", ra.getTypeDesc());

        // test col info
        DataType desig = newClone.getDataDefintion("designation");
        Assert.assertEquals("column width", 100, desig.getWidth());
        Assert.assertEquals("column prefWidth", 10, desig.getPrefWidth());
        Assert.assertEquals("column sortable", false, desig.isSortable());
        Assert.assertEquals("column filterable", true, desig.isFilterable());
        Assert.assertEquals("column visibility", DataType.Visibility.hidden, desig.getVisibility());

    }


    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestEmbeddedDB() throws DataAccessException {
        /*
            3/23/2022: git branch: research_big_table_performance
            Originally:
                - largeIpacTable is a 360mb file, requiring 550mb of RAM in memory
                - to ingest to DB, 3 copies is needed: DataGroup, PrepareStatement, DB tables in memory
                - Minimum memory needed to load this table is 1.7gb (550*3)
            Changes:
                - Turn off DB logging, IO time is cut in half.  From 6s to 3s.
                - Instead of loading the full table in one batch commit, separate the table into multiple smaller batches(set to 10000)
                  A PrepareStatement of 10000 rows take less memory than having the 3rd copy.
                - After the changes, it requires 1.4gb to load this table instead of the 1.7gb.
         */
        ConfigTest.setupServerContext(null);
        Logger.setLogLevel(Level.TRACE);

        TableServerRequest tsr = new TableServerRequest("IpacTableFromSource");
        tsr.setParam(ServerParams.SOURCE, "file://" + largeIpacTable.getPath());
        tsr.setPageSize(100);
        DataGroupPart dgp = (DataGroupPart) SearchManager.getProcessor(tsr.getRequestId()).getData(tsr);
        System.out.println("total rows: " + dgp.getRowCount());
        DbAdapter.getAdapter().cleanup(true);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestMidSize() throws IOException {
        Logger.setLogLevel(Level.DEBUG);
        tableIoTest(midIpacTable);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestLargeSize() throws IOException {
        Logger.setLogLevel(Level.DEBUG);
        tableIoTest(largeIpacTable);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void memTest() {
        Logger.setLogLevel(Level.DEBUG);
        int testSize = 10000000;
        final Ref<Object> ref = new Ref<>();        // prevent gc from collecting

        logMemUsage(() -> {
            List<Object> olist = new ArrayList<>();
            ref.setSource(olist);
            for (int i=0; i< testSize; i++) olist.add(i + Math.random());
            return String.format("List<Object>(%,d)", testSize);
        });
        ref.setSource(null);        // allow gc to collect

        logMemUsage(() -> {
            PrimitiveList.Objects objects = new PrimitiveList.Objects();
            ref.setSource(objects);
            for (int i=0; i< testSize; i++) objects.add(i + Math.random());
            return String.format("Object[](%,d)    ", testSize);
        });
        ref.setSource(null);

        logMemUsage(() -> {
            List<Double> dlist = new ArrayList<>();
            ref.setSource(dlist);
            for (int i=0; i< testSize; i++) dlist.add(i + Math.random());
            return String.format("List<Double>(%,d)", testSize);
        });
        ref.setSource(null);

        logMemUsage(() -> {
            PrimitiveList.Doubles doubleAry = new PrimitiveList.Doubles();
            ref.setSource(doubleAry);
            for (int i=0; i< testSize; i++) doubleAry.add(i * Math.random());
            return String.format("double[](%,d)    ", testSize);
        });
        ref.setSource(null);

        logMemUsage(() -> {
            PrimitiveList.Integers intAry = new PrimitiveList.Integers();
            ref.setSource(intAry);
            for (int i=0; i< testSize; i++) intAry.add((int) (i * Math.random()));
            return String.format("int[](%,d)       ", testSize);
        });
        ref.setSource(null);

        logMemUsage(() -> {
            String[] strAry = new String[testSize];
            ref.setSource(strAry);
            for (int i=0; i< testSize; i++) {
                strAry[i] = String.valueOf(i%10);
            }
            return String.format("strAry[](%,d)    ", testSize);
        });
        ref.setSource(null);

        logMemUsage(() -> {
            List<String> strList = new ArrayList<>();
            ref.setSource(strList);
            for (int i=0; i< testSize; i++) {
                strList.add(String.valueOf(i%10));
            }
            return String.format("strList[](%,d)   ", testSize);
        });
        ref.setSource(null);
    }

    private static DataGroup readTest(File inFile) throws IOException {
        Ref<DataGroup> data = new Ref<>();
        logMemUsage(() -> {
            try {
                data.setSource(IpacTableReader.read(inFile));
                return String.format("READ(%,d)", data.getSource().size());
            } catch (IOException e) {return "ERROR:" + e.getMessage();}
        });
        return data.getSource();
    }

    private static void writeTest(DataGroup data) throws IOException {
        logMemUsage(() -> {
            try {
                File outf = new File("./tmp.tbl");
                outf.deleteOnExit();
                IpacTableWriter.save(outf, data);
                return String.format("WRITE(%,d)", data.size());
            } catch (IOException e) {return "ERROR:" + e.getMessage();}
        });
    }

    private static void tableIoTest(File inFile) throws IOException {
        DataGroup data = readTest(inFile);
        writeTest(data);

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
