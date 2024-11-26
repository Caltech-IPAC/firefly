/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.HealpixProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.StringUtils;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.query.HealpixProcessor.*;
import static edu.caltech.ipac.table.JsonTableUtil.toJsonTableRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HealpixProcessorTest extends ConfigTest {
    static File testFile;
    // first 10 rows of the results from healpy on test table at order=10; total rows = 670
    static int expectedDataSize = 670;
    static String[] expectedData ={
            "6974933,540",
            "6974934,175",
            "6974935,520",
            "6974937,399",
            "6974938,624",
            "6974939,1950",
            "6974940,563",
            "6974941,1115",
            "6974942,1526",
            "6974943,1472"
    };;
    // first 10 rows of the results from healpy on test table with auto_flag=2 at order=10; total rows = 470
    static int autoFlagDataSize = 470;
    static String[] autoFlagData = {
            "6974934,1",
            "6974935,2",
            "6974939,8",
            "6974941,4",
            "6974942,3",
            "6974943,8",
            "6974951,25",
            "6974955,1",
            "6974956,8",
            "6974957,9"
    };

    @BeforeClass
    public static void setUp() {
        // needed because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
        testFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/table_1mil.parquet");
        if (false) Logger.setLogLevel(Level.TRACE, "edu.caltech.ipac.firefly.server.util.StopWatch");			// for debugging; show elapsed time only
    }

    private TableServerRequest makeSearchReq() {
        TableServerRequest sreq = new TableServerRequest(IpacTableFromSource.PROC_ID);
        sreq.setParam(ServerParams.SOURCE, testFile.getAbsolutePath());
        return sreq;
    }

    private TableServerRequest makeMapReq(TableServerRequest sreq) {
        TableServerRequest req = new TableServerRequest(HealpixProcessor.ID);
        req.setParam(HealpixProcessor.SEARCH_REQUEST, toJsonTableRequest(sreq).toJSONString());
        req.setParam(ORDER, "10");      // using order=10 to down-sample the healpix map which were generated at order=12
        req.setParam(RA, "ra");
        req.setParam(DEC, "dec");
        req.setParam(MODE, MAP);
        return req;
    }

    @Test
    public void healpix() {
        try {
            TableServerRequest req = makeMapReq(makeSearchReq());
            DataGroup res = new SearchManager().getDataGroup(req).getData();

            testResults(expectedData, res, req, expectedDataSize);
        } catch (Exception e) {
            fail("HealpixProcessorTest.healpix failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void healpixSorted() {
        try {
            TableServerRequest sreq = makeSearchReq();
            sreq.setSortInfo(new SortInfo("auto_flag"));
            TableServerRequest req = makeMapReq(sreq);
            DataGroup res = new SearchManager().getDataGroup(req).getData();

            // expected same results as unsorted test data
            testResults(expectedData, res, req, expectedDataSize);
        } catch (Exception e) {
            fail("HealpixProcessorTest.healpixSorted failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void healpixFiltered() {
        try {
            TableServerRequest sreq = makeSearchReq();
            sreq.setFilters(Arrays.asList("auto_flag = 2"));
            TableServerRequest req = makeMapReq(sreq);
            DataGroup res = new SearchManager().getDataGroup(req).getData();

            testResults(autoFlagData, res, req, autoFlagDataSize);
        } catch (Exception e) {
            fail("HealpixProcessorTest.healpixFiltered failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void healpixMapByIdx() {
        try {
            // test map of multiple pixels
            Map<Long, Long> pixels = toMap(expectedData);
            TableServerRequest req = makeMapReq(makeSearchReq());
            req.setParam(PIXELS, StringUtils.toString(pixels.keySet()));
            DataGroup res = new SearchManager().getDataGroup(req).getData();
            testResults(expectedData, res, req, pixels.size());

            // test map of one pixel
            Long firstPixel = pixels.keySet().stream().findFirst().get();
            req.setParam(MODE, MAP);
            req.setParam(PIXELS, firstPixel.toString());
            res = new SearchManager().getDataGroup(req).getData();
            assertEquals("number of returned columns", 2, res.getDataDefinitions().length);
            assertEquals("number of returned rows", 1, res.size());
            assertEquals("number of pixels", pixels.get(firstPixel), res.getData("count", 0));
        } catch (Exception e) {
            fail("HealpixProcessorTest.healpixSelectedIdx failed with exception: " + e.getMessage());
        }
    }

    /**
     * This function test only the expected data among the actual data returned
     * For each expected pixel, it will then call fetch the points in that pixel
     * to confirm that the number of returned points matched the expected count.
     * @param expectedData  expected data
     * @param res           actual data
     * @param req           table request used to fetch the data
     * @param rowCount      expected number of rows
     * @throws DataAccessException
     */
    private static void testResults(String[] expectedData, DataGroup res, TableServerRequest req, long rowCount) throws DataAccessException {
        assertEquals("number of returned columns", 2, res.getDataDefinitions().length);
        assertEquals("number of returned rows", rowCount, res.size());

        // test map against testdata.
        Map<Long, Long> expected = toMap(expectedData);
        expected.forEach((pixel, count) -> {
            assertEquals("pixel " + pixel, (long)count, getCountForPixel(res, pixel));
        });

        // test mode=points against testdata.
        req.setParam(MODE, POINTS);
        for(long pixel : expected.keySet()) {
            req.setParam(PIXELS, pixel+"");
            DataGroup points =  new SearchManager().getDataGroup(req).getData();
            assertEquals("points in pixel " + pixel, (long) expected.get(pixel), points.size());
        }
    }

    private static Map<Long, Long> toMap(String[] data) {
        // Create a map using streams, converting values to long
        return Arrays.stream(data)
                .map(entry -> entry.split(",")) // Split each pixel/count pair
                .collect(Collectors.toMap(      // put them into a map
                        entry -> Long.parseLong(entry[0]),
                        entry -> Long.parseLong(entry[1])
                ));
    }

    private static long getCountForPixel(DataGroup data, long pixel) {
        for(DataObject row : data) {
            if (row.getDataElement("pixel").equals(pixel)) return (long) row.getDataElement("count");
        }
        return -1;
    };
}