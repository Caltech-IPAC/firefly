/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static edu.caltech.ipac.firefly.server.db.DbAdapter.MAIN_DB_TBL;

public class EmbeddedDbUtilTest extends ConfigTest {

	public static File dbFile;
	public static File testFile;

	@BeforeClass
	public static void setUp() {
		try {
			// needed by test testGetSelectedData because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
			setupServerContext(null);

			dbFile = File.createTempFile("TestDb_", ".hsql");
			dbFile.deleteOnExit();

			HsqlDbAdapter dbAdapter = new HsqlDbAdapter();
			testFile = FileLoader.resolveFile(EmbeddedDbUtilTest.class, "/embedded_db_test.tbl");
			DataGroup data = IpacTableReader.read(testFile);
			EmbeddedDbUtil.createDbFile(dbFile,dbAdapter);
			EmbeddedDbUtil.ingestDataGroup(dbFile, data, dbAdapter, MAIN_DB_TBL);
		} catch (IOException e) {
			Assert.fail("Unable to ingest data into the database");
		}
	}

	/**
	 * test add new column
	 */
	@Test
	public void testAddColumn() {

		DataType nCol = new DataType("NEW_COL_1", Double.class, "label", "units", null, "desc");
		nCol.setUCD("ucd");
		EmbeddedDbUtil.addColumn(dbFile, DbAdapter.getAdapter(), nCol, "\"dec\" + 3");
		DataGroup res = EmbeddedDbUtil.execQuery(DbAdapter.getAdapter(), dbFile, String.format("select * from %s", MAIN_DB_TBL), MAIN_DB_TBL);

		DataType c = res.getDataDefintion("NEW_COL_1");
		Assert.assertEquals("test number of columns", 10, res.getDataDefinitions().length);			// column added
		Assert.assertNotNull("test name", c);
		Assert.assertEquals("test label", c.getLabel(), "label");
		Assert.assertEquals("test units", c.getUnits(), "units");
		Assert.assertEquals("test label", c.getLabel(), "label");
		Assert.assertEquals("test UCD", c.getUCD(), "ucd");
		Assert.assertEquals("test visibility", c.getVisibility(), DataType.Visibility.show);
		Assert.assertEquals("test value of the expression is populated",
					Double.parseDouble(res.getData("dec", 0).toString()) + 3,
							Double.parseDouble(res.getData("NEW_COL_1", 0).toString()), 0.0001);

		// test rollback conditions

		// bad expression:  dec + 3 is a bad expression since DEC is not a column; dec without quotes is treated as uppercase
		nCol.setKeyName("NEW_COL_2");
		try {
			EmbeddedDbUtil.addColumn(dbFile, DbAdapter.getAdapter(), nCol, "dec + 3");
		} catch (Exception ignored){}
		res = EmbeddedDbUtil.execQuery(DbAdapter.getAdapter(), dbFile, String.format("select * from %s", MAIN_DB_TBL), MAIN_DB_TBL);
		Assert.assertEquals("same number of columns as before", 10, res.getDataDefinitions().length);
		Assert.assertNull("new column not added", res.getDataDefintion("NEW_COL_2"));

		// bad column name:  exceeded 128 characters
		nCol.setKeyName("c12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
		try {
			EmbeddedDbUtil.addColumn(dbFile, DbAdapter.getAdapter(), nCol , "\"dec\" + 3");
		} catch (Exception ignored){}
		res = EmbeddedDbUtil.execQuery(DbAdapter.getAdapter(), dbFile, String.format("select * from %s", MAIN_DB_TBL), MAIN_DB_TBL);
		Assert.assertEquals("same number of columns as before", 10, res.getDataDefinitions().length);

	}


	/**
	 * test results based on the constructed TableServerRequest
	 */
	@Test
	public void testExecRequestQuery() {
		try {
			TableServerRequest req = new TableServerRequest("n/a (id not used)");
			req.setPageSize(9);
			req.setInclColumns("\"designation\", \"dec\", \"sigdec\"");
			DataGroupPart res = EmbeddedDbUtil.execRequestQuery(req, dbFile, MAIN_DB_TBL);
			DataGroup data = res.getData();

			// test total rows, returned rows, and cols
			Assert.assertEquals(res.getRowCount(), 10);
			Assert.assertEquals(data.size(), 9);
			Assert.assertEquals(data.getDataDefinitions().length, 3);

			// random tests of returned values
			Assert.assertEquals("J132955.35+471336.8", data.get(0).getDataElement("designation"));
			Assert.assertEquals(0.0476, data.get(8).getDataElement("sigdec"));

			// make sure meta information is passed back
			testMeta(data);

			// test sort;  reset request object
			req = new TableServerRequest("n/a (id not used)");
			req.setSortInfo(new SortInfo(SortInfo.Direction.DESC, "sigra"));
			res = EmbeddedDbUtil.execRequestQuery(req, dbFile, MAIN_DB_TBL);
			data = res.getData();
			Assert.assertEquals(0.0744, data.get(0).getDataElement("sigra"));
			Assert.assertEquals(0.0413, data.get(9).getDataElement("sigra"));

			// make sure meta information is still good after sorting
			testMeta(data);

			// test filter;  reset request object
			req = new TableServerRequest("n/a (id not used)");
			req.setSortInfo(new SortInfo(SortInfo.Direction.DESC, "sigra"));
			req.setFilters(Arrays.asList(String.format("\"%s\" in (%f, %f, %f)", "sigra", 0.0413, 0.0744, 12345.6)));
			res = EmbeddedDbUtil.execRequestQuery(req, dbFile, MAIN_DB_TBL);
			data = res.getData();
			Assert.assertEquals(2, data.size());		// the 3rd values(12345.6) is not in the table
			Assert.assertEquals(0.0755, data.get(0).getDataElement("sigdec"));
			Assert.assertEquals(0.0394, data.get(1).getDataElement("sigdec"));

			// make sure meta information is still good after sorting
			testMeta(data);


		} catch (Exception e) {
			Assert.fail("testExecRequestQuery failed with exception: " + e.getMessage());
		}
	}

	/**
	 * a lower level access based on a SQL statement
	 */
	@Test
	public void testExecQuery() {
		HsqlDbAdapter dbAdapter = new HsqlDbAdapter();
		String sql = "select d.* from (select \"designation\", \"RA(deg)\"+1 as ABC from data where \"sigdec\" > 0.0750) as d order by d.ABC";
		DataGroup data = EmbeddedDbUtil.execQuery(dbAdapter, dbFile, sql, MAIN_DB_TBL);

		Assert.assertEquals(203.4851848, data.get(0).getDataElement("ABC"));
		Assert.assertEquals(203.4874371, data.get(1).getDataElement("ABC"));

	}

	/**
	 * test data retrieval based on selected rows
	 */
	@Test
	public void testGetSelectedData() {
		TableServerRequest treq = new TableServerRequest(IpacTableFromSource.PROC_ID);
		treq.setParam(ServerParams.SOURCE, testFile.toString());
		treq.setPageSize(Integer.MAX_VALUE);
		treq.setSortInfo(new SortInfo("sigra"));
		treq.setFilters(Arrays.asList("\"sigra\" < 0.06"));

		MappedData selVals = EmbeddedDbUtil.getSelectedMappedData(treq, Arrays.asList(1, 3, 5), "dec", "clon+1", "RA(deg)");

		Assert.assertEquals("13h29m56.55s", selVals.get(1, "clon+1"));
		Assert.assertEquals(47.2321996, selVals.get(3, "dec"));
		Assert.assertEquals(202.4680189, selVals.get(5, "RA(deg)"));
	}

	private void testMeta(DataGroup data) {
		// test meta
		Assert.assertEquals("ORIGIN value", data.getAttribute("ORIGIN"));
		Assert.assertEquals("SQL value", data.getAttribute("SQL"));
		Assert.assertEquals("repeated key will get overriden", data.getTableMeta().getKeywords().get(5).getValue());  // but, it's still in keywords

		// test column meta
		DataType dt = data.getDataDefintion("dec");
		Assert.assertEquals("rad", dt.getUnits());
		Assert.assertEquals(Double.class, dt.getDataType());
		Assert.assertEquals("%.6g", dt.getFmtDisp());
	}

	@Category({TestCategory.Perf.class})
	@Test
	public void perfTestLargeSize() throws Exception {

//		no added props.  all defaults from hsqldb
//		StopWatch - ingest DB file ran with elapsed time of 16.7340 SECONDS
//		StopWatch - sort DATA table ran with elapsed time of 5.0720 SECONDS

// 		custom props
//		StopWatch - ingest DB file ran with elapsed time of 3.0330 SECONDS
//		StopWatch - sort DATA table ran with elapsed time of 5.3010 SECONDS

//		custom props; with hsqldb.tx=MVCC
//		StopWatch - ingest DB file ran with elapsed time of 3.3680 SECONDS
//		StopWatch - sort DATA table ran with elapsed time of 5.5850 SECONDS


		File testFile = FileLoader.resolveFile("large-ipac-tables/wise-950000.tbl");            // 358MB; 950k rows

		File dbFile = File.createTempFile("perfTest", ".hsql");
		dbFile.deleteOnExit();

		HsqlDbAdapter dbAdapter = new HsqlDbAdapter();

		Logger.setLogLevel(Level.TRACE, "edu.caltech");
		DataGroup data = IpacTableReader.read(testFile);
		StopWatch.getInstance().start("ingest DB file");
		EmbeddedDbUtil.createDbFile(dbFile,dbAdapter);
		EmbeddedDbUtil.ingestDataGroup(dbFile, data, dbAdapter, MAIN_DB_TBL);
		StopWatch.getInstance().printLog("ingest DB file");

		StopWatch.getInstance().start("sort DATA table");
		String sql = "select * from DATA order by \"w1mpro\"";
		EmbeddedDbUtil.execQuery(dbAdapter, dbFile, sql, MAIN_DB_TBL);
		StopWatch.getInstance().printLog("sort DATA table");

	}
}