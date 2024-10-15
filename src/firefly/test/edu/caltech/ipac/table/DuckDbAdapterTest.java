/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbMonitor;
import edu.caltech.ipac.firefly.server.db.DuckDbReadable;
import edu.caltech.ipac.firefly.server.db.H2DbAdapter;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DecimationProcessor;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.util.decimate.DecimateKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.server.db.DuckDbAdapter.*;
import static edu.caltech.ipac.firefly.server.util.QueryUtil.SEARCH_REQUEST;
import static org.junit.Assert.*;

public class DuckDbAdapterTest extends ConfigTest {

	@Before
	public void setUp() {
		setupServerContext(null);
		if (false) Logger.setLogLevel(Level.TRACE);			// for debugging.
	}

	@After
	public void tearDown() {
		DbMonitor.cleanup(true, true);
	}

	/**
	 * test results based on the constructed TableServerRequest
	 */
	@Test
	public void testParquetDbAdapter() throws DataAccessException {
		File testFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/iris.parquet");

		TableServerRequest req = new TableServerRequest(IpacTableFromSource.PROC_ID);
		req.setParam(ServerParams.SOURCE, testFile.getAbsolutePath());
		req.setFilters(List.of("\"sepal.width\" > 3"));
		var dgp = new SearchManager().getDataGroup(req);
		var data = dgp.getData();

		// test total rows, returned rows, and cols
		assertEquals(67, dgp.getRowCount());
		assertEquals(7, data.getDataDefinitions().length);
	}

	/**
	 * test DuckDB decimate_key() function
	 */
	@Test
	public void testDecimateKey() throws Exception {
		// stats from file
		//┌─────────────┬─────────────┬───────────┬───────────┬───────────────┬─────────────────────┬──────────────────────┬─────────┐
		//│ column_name │ column_type │    min    │    max    │ approx_unique │         avg         │         std          │  count  │
		//├─────────────┼─────────────┼───────────┼───────────┼───────────────┼─────────────────────┼──────────────────────┼─────────┤
		//│ ra          │ DOUBLE      │ 149.41147 │ 150.82684 │        122939 │ 150.10669643252479  │ 0.4002817385008964   │ 1005002 │
		//│ dec         │ DOUBLE      │ 1.498815  │ 2.91273   │        269551 │ 2.219145634837543   │ 0.3942764400346131   │ 1005002 │

		double xMin = 149.41147;
		double xMax = 150.82684;
		double yMin = 1.498815;
		double yMax = 2.91273;


	// code from doDecimation; to calculate nXs, nYs, xUnit, yUnit
		int nXs = (int)Math.sqrt(100_000 * 1.0);	// number of cells on the x-axis
		int nYs = (int)Math.sqrt(100_000 / 1.0);  	// number of cells on the x-axis

		double xUnit = (xMax - xMin)/nXs;        // the x size of a cell
		double yUnit = (yMax - yMin)/nYs;        // the y size of a cell

		// case when min and max values are the same
		if (xUnit == 0) xUnit = Math.abs(xMin) > 0 ? Math.abs(xMin) : 1;
		if (yUnit == 0) yUnit = Math.abs(yMin) > 0 ? Math.abs(yMin) : 1;

		// increase cell size a bit to include max values into grid
		xUnit += xUnit/1000.0/nXs;
		yUnit += yUnit/1000.0/nYs;
	//-----------------

		DecimateKey deciKey = new DecimateKey(xMin, yMin, nXs, nYs, xUnit, yUnit);

		File testFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/table_1mil.csv");

		TableServerRequest req = new TableServerRequest(IpacTableFromSource.PROC_ID);
		req.setParam(ServerParams.SOURCE, testFile.getAbsolutePath());
		req.setInclColumns(String.format("\"ra\", \"dec\", decimate_key(\"ra\", \"dec\", %.15f, %.15f, %d, %d, %.15f, %.15f) as dkey", xMin, yMin, nXs, nYs, xUnit, yUnit));
		var dgp = new SearchManager().getDataGroup(req);
		var dbData = dgp.getData();
		var csvData = DsvTableIO.parse(testFile, CSVFormat.DEFAULT);

		for (int i = 0; i < csvData.size(); i++) {
			DataObject row = csvData.get(i);

			double xval = row.getDouble("ra", 0);
			double yval = row.getDouble("dec", 0);
			String dkey = deciKey.getKey(xval, yval);
			assertEquals(String.format("row: %d(%f,%f)-(%f,%f)", i, xval, yval, dbData.getData("ra", i), dbData.getData("dec", i)),
					dkey,
					dbData.getData("dkey", i)
					);
		}
	}

	/**
	 * normal SQL regression test
	 */
	@Test
	public void testNormalQuery() throws DataAccessException {
		File duckFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/cars.csv");

		TableServerRequest duckReq = new TableServerRequest(IpacTableFromSource.PROC_ID);
		duckReq.setParam(ServerParams.SOURCE, duckFile.getAbsolutePath());
		duckReq.setPageSize(-1);

		TableServerRequest hsqlReq = (TableServerRequest) duckReq.cloneRequest();
		hsqlReq.setMeta(TBL_FILE_TYPE, HsqlDbAdapter.NAME);

		SearchManager sm = new SearchManager();

		DataGroup duckTbl = sm.getDataGroup(duckReq).getData();
		DataGroup hsqlTbl = sm.getDataGroup(hsqlReq).getData();

		fullTableTest(duckTbl, hsqlTbl);

		// apply filter
		duckReq.setInclColumns(Stream.of("model", "hp", "gear").map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) );
		hsqlReq.setInclColumns(duckReq.getInclColumns());
		duckReq.setFilters(List.of("\"gear\" > 3"));
		hsqlReq.setFilters(duckReq.getFilters());

		duckTbl = sm.getDataGroup(duckReq).getData();
		hsqlTbl = sm.getDataGroup(hsqlReq).getData();

		fullTableTest(duckTbl, hsqlTbl);

		// apply filter and sort
		duckReq.setSortInfo(SortInfo.parse("SortInfo:DESC,\"hp\""));
		hsqlReq.setSortInfo(duckReq.getSortInfo());

		duckTbl = sm.getDataGroup(duckReq).getData();
		hsqlTbl = sm.getDataGroup(hsqlReq).getData();

		fullTableTest(duckTbl, hsqlTbl);
	}

	/**
	 * compairing decimation between HsqlDB and DuckDB
	 */
	@Test
	public void testDecimateQuery() throws DataAccessException {
		File duckFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/table_1mil.csv");

		TableServerRequest duck = new TableServerRequest(IpacTableFromSource.PROC_ID);
		duck.setParam(ServerParams.SOURCE, duckFile.getAbsolutePath());
		duck.setInclColumns(Stream.of("ra", "dec").map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) );
		duck.setPageSize(-1);

		TableServerRequest hsql = (TableServerRequest) duck.cloneRequest();
		hsql.setMeta(TBL_FILE_TYPE, HsqlDbAdapter.NAME);

		TableServerRequest duckReq = new TableServerRequest(DecimationProcessor.ID);
		duckReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(duck).toJSONString());
		duckReq.setParam(DecimationProcessor.DECIMATE_INFO, new DecimateInfo("ra", "dec").toString());
		duckReq.setPageSize(-1);
		duckReq.setSortInfo(new SortInfo("dkey"));

		TableServerRequest hsqlReq = (TableServerRequest) duckReq.cloneRequest();
		hsqlReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(hsql).toJSONString());

		DataGroup duckTbl = new SearchManager().getDataGroup(duckReq).getData();
		DataGroup hsqlTbl = new SearchManager().getDataGroup(hsqlReq).getData();

		fullTableTest(duckTbl, hsqlTbl);

		// apply filter and sort to the original table, then check again.
		duck.setSqlFilter("\"dec\" > 2");
		duck.setSortInfo(new SortInfo("icmag"));
		// create same req for HsqlDB version
		hsql = (TableServerRequest) duck.cloneRequest();
		hsql.setMeta(TBL_FILE_TYPE, HsqlDbAdapter.NAME);

		// update deci request
		duckReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(duck).toJSONString());
		hsqlReq.setParam(SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(hsql).toJSONString());

		duckTbl = new SearchManager().getDataGroup(duckReq).getData();
		hsqlTbl = new SearchManager().getDataGroup(hsqlReq).getData();

		// then do the same test
		fullTableTest(duckTbl, hsqlTbl);

	}

	@Test
	public void testCleanup() throws DataAccessException {

		File duckFile = FileLoader.resolveFile(DuckDbAdapterTest.class, "/iris.parquet");

		TableServerRequest treq = new TableServerRequest(IpacTableFromSource.PROC_ID);
		treq.setParam(ServerParams.SOURCE, duckFile.getAbsolutePath());
		treq.setPageSize(1);  // we won't be testing data

		EmbeddedDbProcessor proc = (EmbeddedDbProcessor) SearchManager.getProcessor(IpacTableFromSource.PROC_ID);
		DbAdapter dbAdapter = proc.getDbAdapter(treq);

		assertEquals("DuckDB is used for parquet files", DuckDbReadable.NAME, dbAdapter.getName());

		// load the data into the database; 4 tables will be created.  DATA, DATA_DD, DATA_META, and DATA_AUX
		proc.getData(treq);
		assertEquals("Data table and its associated tables are created", 4, dbAdapter.getTableNames().size());

		// sort by model.  this should create a temp table
		treq.setSortInfo(new SortInfo("sepal.width"));
		proc.getData(treq);
		assertEquals("New set of temp tables created for the request", 8, dbAdapter.getTableNames().size());

		dbAdapter.clearCachedData();
		assertEquals("Temp tables are removed", 4, dbAdapter.getTableNames().size());

		dbAdapter.close(true);
		assertFalse("DuckDb file is deleted", dbAdapter.getDbFile().exists());
	}

	@Test
	public void testSynchronizedAccess() throws InterruptedException {
		ArrayList<Long> even = new ArrayList<>();
		ArrayList<Long> odd = new ArrayList<>();

		var locker = new SearchProcessor.SynchronizedAccess();
		Function<String,Long> setResults = (q) -> {
			long start = System.currentTimeMillis();
			var release = locker.lock(q);
            try {
				Thread.sleep(1_000);
            } catch (InterruptedException e) {}
			finally {
				release.run();
            }
			return System.currentTimeMillis() - start;
        };

		int ntimes = 10;
		var p = Executors.newFixedThreadPool(ntimes);		// when all threads start at the same time, all be blocked.
		for(int i = 0; i < ntimes; i++) {
			long a = i % 2;
			p.submit(() -> {
                    if (a == 0 ) {
                        even.add(Math.round(setResults.apply("even")/1000.0));
                    } else {
                        odd.add(Math.round(setResults.apply("odd")/1000.0));
                    }
            });
		}
		p.shutdown();
		if (!p.awaitTermination(10, TimeUnit.SECONDS)) {
			System.out.println("Not all tasks completed in time.");
		}
//		even.forEach(System.out::println);
//		odd.forEach(System.out::println);

		assertEquals(ntimes/2, (long)Collections.max(even));
		assertEquals(1L, (long)Collections.min(even));
		assertEquals(ntimes/2, (long)Collections.max(odd));
		assertEquals(1L, (long)Collections.min(odd));
    }

	@Test
	public void testLikeSubstitution() {
		// replace uppercase LIKE
		assertEquals("WHERE col ILIKE '%abc%'",
				replaceLike("WHERE col LIKE '%abc%'"));

		// replace lower case like
		assertEquals("WHERE col ILIKE '%xyz%'",
				replaceLike("WHERE col like '%xyz%'"));

		// ignore when inside of single quotes
		assertEquals("WHERE col = 'This is LIKE something'",
				replaceLike("WHERE col = 'This is LIKE something'"));

		// ignore when inside of double quotes
		assertEquals("WHERE \"col like\" = 'Some string LIKE pattern'",
				replaceLike("WHERE \"col like\" = 'Some string LIKE pattern'"));

		// LIKE outside and inside quotes
		assertEquals("WHERE abc ILIKE '%abc%' and col = 'Some string LIKE pattern'",
				replaceLike("WHERE abc like '%abc%' and col = 'Some string LIKE pattern'"));

		// no match; return original
		assertEquals("WHERE col like-one '%xyz%'",
				replaceLike("WHERE col like-one '%xyz%'"));

		// mixing single and double quotes
		assertEquals("'test ILIKE inside\" badly quoted ILIKE ",
				replaceLike("'test like inside\" badly quoted like "));
	}

//====================================================================
//  PRIVATE section
//====================================================================

	private static void fullTableTest(DataGroup t1, DataGroup t2, String ...checkCols) {
		assertEquals(t1.size(), t2.size());
		assertEquals(t1.getDataDefinitions().length, t2.getDataDefinitions().length);

		DataType[] colsTocheck = checkCols != null ? Arrays.stream(checkCols).map(t1::getDataDefintion).toArray(DataType[]::new) : t1.getDataDefinitions();

		for (int i = 0; i < t1.size(); i++) {
			DataObject t1row = t1.get(i);
			DataObject t2row = t2.get(i);
			for (DataType c : colsTocheck) {
				assertEquals(String.format("Cell (%d,%s)", i, c.getKeyName()), getVal(t1row, c), getVal(t2row, c));
			}
		}
	}

	private static Object getVal(DataObject row, DataType c) {
		return c.isNumeric() ? row.getDouble(c.getKeyName(), 0) : row.getStringData(c.getKeyName());
	}

}