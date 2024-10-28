/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.tables;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbMonitor;
import edu.caltech.ipac.firefly.server.db.DuckDbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.PrimitiveList;
import edu.caltech.ipac.table.VotableTest;
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
    private static final File largeVoTable = getDataFile(VotableTest.class, "table_1mil.vot");

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
    public void perfTestVoTableRead() throws DataAccessException {
        /*
        Initial:  10/23/2024
            smallVoTableRead:  Memory Usage peak=  3.00MB  final:  0.28MB  elapsed:0.05secs
            largeVoTableRead:  Memory Usage peak=1077.10MB  final:  0.02MB  elapsed:3.07secs  jvm:mx=1.1g
        Changes:
            FIREFLY-1591:   10/23/2024
            - ADAPTIVE + TableParseHandler.Memory
                smallVoTableRead:  Memory Usage peak=  4.00MB  final:  0.48MB  elapsed:0.06secs
                largeVoTableRead:  Memory Usage peak=245.04MB  final:  0.10MB  elapsed:2.67secs     jvm:mx=250m
        */
        ConfigTest.setupServerContext(null);
        Logger.setLogLevel(Level.INFO);
        logMemUsage(() -> {
            try {
                VoTableReader.voToDataGroups(voTable.getAbsolutePath());
            } catch (Exception ignored) {}
            return "smallVoTableRead";
        });
        logMemUsage(() -> {
            try {
                VoTableReader.voToDataGroups(largeVoTable.getAbsolutePath());
            } catch (Exception ignored) {}
            return "largeVoTableRead";
        });
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestVoTableIngest() throws DataAccessException {
        /*
        Initial:
            smallVoTableRead:  elapsed:0.33secs
            largeVoTableRead:  elapsed:6.76secs     jvm:mx=1.1g
        Changes:
            FIREFLY-1591:   10/23/2024
            - ADAPTIVE + TableParseHandler.Memory
                smallVoTableRead:  elapsed:0.33secs
                largeVoTableRead:  elapsed:6.36secs     jvm:mx=250m
            - ADAPTIVE + TableParseHandler.DbIngest
                smallVoTableRead: elapsed:0.38secs
                largeVoTableRead: elapsed:5.25secs      jvm:mx=32m
        */
        ConfigTest.setupServerContext(null);
        Logger.setLogLevel(Level.INFO);
        try {
            File f = File.createTempFile("tst", "duckdb");
            f.deleteOnExit();
            new DuckDbAdapter(ext -> f).initDbFile();       // skip init pause
        } catch (IOException ignored) {}
        logMemUsage(() -> {
            TableServerRequest tsr = new TableServerRequest("IpacTableFromSource");
            tsr.setParam(ServerParams.SOURCE, "file://" + voTable.getPath());
            tsr.setPageSize(1);
            try {
                SearchManager.getProcessor(tsr.getRequestId()).getData(tsr);
            } catch (DataAccessException ignored) {}
            DbMonitor.cleanup(true,true);
            return "smallVoTableRead";
        });

        logMemUsage(() -> {
            TableServerRequest tsr = new TableServerRequest("IpacTableFromSource");
            tsr.setParam(ServerParams.SOURCE, "file://" + largeVoTable.getPath());
            tsr.setPageSize(1);
            try {
                SearchManager.getProcessor(tsr.getRequestId()).getData(tsr);
            } catch (DataAccessException ignored) {}
            DbMonitor.cleanup(true,true);
            return "largeVoTableRead";
        });

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

        /*
        Initial:
            perfTestEmbeddedDB:  Memory Usage peak=1114.47MB  final:  4.75MB  elapsed:12.35secs
        FIREFLY-1297: Support null values
            perfTestEmbeddedDB:  Memory Usage peak=1144.28MB  final:  4.75MB  elapsed:12.74secs
        FIREFLY-1471-table-null-val
            perfTestEmbeddedDB:  Memory Usage peak=1179.26MB  final:  4.74MB  elapsed:12.71secs
        FIREFLY-1592: direct ingestion of IPAC table (10/28/2024)
            Memory Usage peak= 42.28MB  final:  4.94MB  elapsed:10.22secs

        */
        logMemUsage(() -> {
            ConfigTest.setupServerContext(null);
            Logger.setLogLevel(Level.INFO);

            TableServerRequest tsr = new TableServerRequest("IpacTableFromSource");
            tsr.setParam(ServerParams.SOURCE, "file://" + largeIpacTable.getPath());
            tsr.setPageSize(100);
            DataGroupPart dgp = null;
            try {
                dgp = (DataGroupPart) SearchManager.getProcessor(tsr.getRequestId()).getData(tsr);
            } catch (DataAccessException ignored) {}
            System.out.println("total rows: " + dgp.getRowCount());
            DbMonitor.cleanup(true,true);
            return "perfTestEmbeddedDB";
        });

    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestMidSize() throws IOException {
        /*
        Initial:
            11:31:20.200 [main] INFO  test - READ(49,933):  Memory Usage peak=100.78MB  final:  8.47MB  elapsed:0.41secs
            11:31:22.545 [main] INFO  test - WRITE(49,933):  Memory Usage peak= 44.11MB  final:  0.03MB  elapsed:2.30secs
        FIREFLY-1297: Support null values
            12:17:42.396 [main] INFO  test - READ(49,933):  Memory Usage peak=101.28MB  final:  9.15MB  elapsed:0.38secs
            12:17:44.556 [main] INFO  test - WRITE(49,933):  Memory Usage peak=104.11MB  final:  0.03MB  elapsed:2.12secs
        FIREFLY-1471-table-null-val
            13:41:09.291 [main] INFO  test - READ(49,933):  Memory Usage peak= 72.66MB  final:  8.39MB  elapsed:0.42secs
            13:41:11.138 [main] INFO  test - WRITE(49,933):  Memory Usage peak=124.11MB  final:  0.03MB  elapsed:1.80secs
        */

        Logger.setLogLevel(Level.DEBUG);
        tableIoTest(midIpacTable);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfTestLargeSize() throws IOException {
        /*
        Initial:
            11:32:24.864 [main] INFO  test - READ(950,002):  Memory Usage peak=699.83MB  final:512.94MB  elapsed:4.61secs
            11:33:05.518 [main] INFO  test - WRITE(950,002):  Memory Usage peak=184.14MB  final:  0.03MB  elapsed:40.53secs

        FIREFLY-1297: Support null values
            12:20:33.105 [main] INFO  test - READ(950,002):  Memory Usage peak=919.18MB  final:563.41MB  elapsed:4.79secs
            12:21:13.399 [main] INFO  test - WRITE(950,002):  Memory Usage peak=520.13MB  final:  0.03MB  elapsed:40.13secs
        FIREFLY-1471-table-null-val
            13:44:29.975 [main] INFO  test - READ(950,002):  Memory Usage peak=1002.53MB  final:518.10MB  elapsed:4.54secs
            13:45:09.884 [main] INFO  test - WRITE(950,002):  Memory Usage peak=264.13MB  final:  0.03MB  elapsed:39.79secs
        */
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
            var list = new ArrayList<>();
            ref.set(list);
            for (int i=0; i< testSize; i++) list.add(i + Math.random());
            return String.format("List<Object>(%,d)", testSize);
        });
        ref.set(null);        // allow gc to collect

        logMemUsage(() -> {
            var list = new PrimitiveList.Objects();
            ref.set(list);
            for (int i=0; i< testSize; i++) list.add(i + Math.random());
            return String.format("Object[](%,d)    ", testSize);
        });
        ref.set(null);

        logMemUsage(() -> {
            var list = new ArrayList<>();
            ref.set(list);
            for (int i=0; i< testSize; i++) list.add(i + Math.random());
            return String.format("List<Double>(%,d)", testSize);
        });
        ref.set(null);

        logMemUsage(() -> {
            var list = new PrimitiveList.Doubles();
            ref.set(list);
            for (int i=0; i< testSize; i++) list.add(i * Math.random());
            return String.format("double[](%,d)    ", testSize);
        });
        ref.set(null);

        logMemUsage(() -> {
            var list = new PrimitiveList.Longs();
            ref.set(list);
            for (int i=0; i< testSize; i++) list.add((long) (i * Math.random()));
            return String.format("long[](%,d)       ", testSize);
        });
        ref.set(null);

        logMemUsage(() -> {
            var list = new PrimitiveList.Integers();
            ref.set(list);
            for (int i=0; i< testSize; i++) list.add((int) (i * Math.random()));
            return String.format("int[](%,d)       ", testSize);
        });
        ref.set(null);

        logMemUsage(() -> {
            var strAry = new String[testSize];
            ref.set(strAry);
            for (int i=0; i< testSize; i++) {
                strAry[i] = String.valueOf(i%10);
            }
            return String.format("strAry[](%,d)    ", testSize);
        });
        ref.set(null);

        logMemUsage(() -> {
            var list = new ArrayList<>();
            ref.set(list);
            for (int i=0; i< testSize; i++) {
                list.add(String.valueOf(i%10));
            }
            return String.format("strList[](%,d)   ", testSize);
        });
        ref.set(null);
    }

    private static DataGroup readTest(File inFile) throws IOException {
        Ref<DataGroup> data = new Ref<>();
        logMemUsage(() -> {
            try {
                data.set(IpacTableReader.read(inFile));
                return String.format("READ(%,d)", data.get().size());
            } catch (IOException e) {return "ERROR:" + e.getMessage();}
        });
        return data.get();
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
