/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static edu.caltech.ipac.firefly.server.db.DbAdapter.getAdapter;

public class EmbeddedDbUtilTest extends ConfigTest {

	public static File dbFile;
	public static File testFile;
	public static File csvFile;

	@BeforeClass
	public static void setUp() {
		try {
			// needed by test testGetSelectedData because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
			setupServerContext(null);
			File tmp = new File(System.getProperty("java.io.tmpdir"));
			var dbAdapter = DbAdapter.getAdapter("", (ext) -> new File(tmp, "%d.%s".formatted(System.currentTimeMillis(), ext)));
			dbFile = dbAdapter.initDbFile();

			testFile = FileLoader.resolveFile(EmbeddedDbUtilTest.class, "/embedded_db_test.tbl");
			DataGroup data = IpacTableReader.read(testFile);
			dbAdapter.ingestData(() -> data, dbAdapter.getDataTable());
		} catch (IOException|DataAccessException e) {
			Assert.fail("Unable to ingest data into the database");
		}
	}

	@AfterClass
	public static void cleanup() {
		DbAdapter.getAdapter(dbFile).close(true);
		System.out.println("DbAdapter closed.");
	}

	/**
	 * test add new column
	 */
	@Test
	public void testAddColumn() throws DataAccessException {
		DbAdapter dbAdapter = getAdapter(dbFile);

		DataType nCol = new DataType("NEW_COL_1", Double.class, "label", "units", null, "desc");
		nCol.setUCD("ucd");
		EmbeddedDbUtil.addColumn(dbAdapter, nCol, "\"dec\" + 3");
		DataGroup res = dbAdapter.execQuery(String.format("select * from %s", dbAdapter.getDataTable()), dbAdapter.getDataTable());

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

		// bad expression:  decx + 3 is a bad expression since decx is not a column
		nCol.setKeyName("NEW_COL_2");
		try {
			EmbeddedDbUtil.addColumn(dbAdapter, nCol, "decx + 3");
		} catch (Exception ignored){}
		res = dbAdapter.execQuery(String.format("select * from %s", dbAdapter.getDataTable()), dbAdapter.getDataTable());
		Assert.assertEquals("same number of columns as before", 10, res.getDataDefinitions().length);
		Assert.assertNull("new column not added", res.getDataDefintion("NEW_COL_2"));

		// bad column name:  exceeded 128 characters; this is a hsqldb limitation
		if (DbAdapter.DEF_DB_TYPE.equals(HsqlDbAdapter.NAME)) {
			nCol.setKeyName("c".repeat(130));
			try {
				EmbeddedDbUtil.addColumn(dbAdapter, nCol , "\"dec\" + 3");
			} catch (Exception ignored){}
			res = dbAdapter.execQuery(String.format("select * from %s", dbAdapter.getDataTable()), dbAdapter.getDataTable());
			Assert.assertEquals("same number of columns as before", 10, res.getDataDefinitions().length);
		}
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
			var dbAdapter = getAdapter(dbFile);
			DataGroupPart res = dbAdapter.execRequestQuery(req, dbAdapter.getDataTable());
			DataGroup data = res.getData();

			// test total rows, returned rows, and cols
			Assert.assertEquals(10, res.getRowCount());
			Assert.assertEquals(9, data.size());
			Assert.assertEquals(3, data.getDataDefinitions().length);

			// random tests of returned values
			Assert.assertEquals("J132955.35+471336.8", data.get(0).getDataElement("designation"));
			Assert.assertEquals(0.0476, data.get(8).getDataElement("sigdec"));

			// make sure meta information is passed back
			testMeta(data);

			// test sort;  reset request object
			req = new TableServerRequest("n/a (id not used)");
			req.setSortInfo(new SortInfo(SortInfo.Direction.DESC, "sigra"));
			res = dbAdapter.execRequestQuery(req, dbAdapter.getDataTable());
			data = res.getData();
			Assert.assertEquals(0.0744, data.get(0).getDataElement("sigra"));
			Assert.assertEquals(0.0413, data.get(9).getDataElement("sigra"));

			// make sure meta information is still good after sorting
			testMeta(data);

			// test filter;  reset request object
			req = new TableServerRequest("n/a (id not used)");
			req.setSortInfo(new SortInfo(SortInfo.Direction.DESC, "sigra"));
			req.setFilters(List.of(String.format("\"%s\" in (%f, %f, %f)", "sigra", 0.0413, 0.0744, 12345.6)));
			res = dbAdapter.execRequestQuery(req, dbAdapter.getDataTable());
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
	public void testExecQuery() throws DataAccessException{
		DbAdapter dbAdapter = DbAdapter.getAdapter(dbFile);
		String sql = "select d.* from (select \"designation\", \"RA(deg)\"+1 as ABC from data where \"sigdec\" > 0.0750) as d order by d.ABC";
		DataGroup data = dbAdapter.execQuery(sql, dbAdapter.getDataTable());

		Assert.assertEquals(203.4851848, data.get(0).getDataElement("ABC"));
		Assert.assertEquals(203.4874371, data.get(1).getDataElement("ABC"));

	}

	/**
	 * test data retrieval based on selected rows
	 */
	@Test
	public void testGetSelectedData() throws DataAccessException {
		TableServerRequest treq = new TableServerRequest(IpacTableFromSource.PROC_ID);
		treq.setParam(ServerParams.SOURCE, testFile.toString());
		treq.setPageSize(Integer.MAX_VALUE);
		treq.setSortInfo(new SortInfo("sigra"));
		treq.setFilters(List.of("\"sigra\" < 0.06"));

		MappedData selVals = EmbeddedDbUtil.getSelectedMappedData(treq, List.of(1, 3, 5), "dec", "clon+1", "RA(deg)");

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

		DbAdapter dbAdapter = DbAdapter.getAdapter(dbFile);

		Logger.setLogLevel(Level.TRACE, "edu.caltech");
		DataGroup data = IpacTableReader.read(testFile);
		StopWatch.getInstance().start("ingest DB file");
		dbAdapter.initDbFile();
		dbAdapter.ingestData(() -> data, dbAdapter.getDataTable());
		StopWatch.getInstance().printLog("ingest DB file");

		StopWatch.getInstance().start("sort DATA table");
		String sql = "select * from DATA order by \"w1mpro\"";
		dbAdapter.execQuery(sql, dbAdapter.getDataTable());
		StopWatch.getInstance().printLog("sort DATA table");

	}

	@Ignore
	@Test
	public void testSqlErrors() {
		DbAdapter dbAdapter = getAdapter(dbFile);
		try {
			dbAdapter.execQuery("select * from xyz", dbAdapter.getDataTable());
			Assert.fail("test bad table name failed");
		}catch (DataAccessException e) {
			Assert.assertEquals("[XYZ] not found; SQL=[select * from xyz]", e.getMessage());
		}

		try {
			dbAdapter.execQuery("select xyz from data", dbAdapter.getDataTable());
			Assert.fail("test bad column name failed");
		}catch (DataAccessException e) {
			Assert.assertEquals("[XYZ] not found; SQL=[select xyz from data]", e.getMessage());
		}

		try {
			dbAdapter.execQuery("not an sql statement", dbAdapter.getDataTable());
			Assert.fail("test invalid sql statement failed");
		}catch (DataAccessException e) {
			Assert.assertEquals("Unexpected token [NOT]; SQL=[not an sql statement]", e.getMessage());
		}

		try {
			dbAdapter.execQuery("select * from data where \"dec\" = 'ac'", dbAdapter.getDataTable());
			Assert.fail("test type mismatched(comparing char to double) failed");
		}catch (DataAccessException e) {
			Assert.assertEquals("Type mismatch; SQL=[select * from data where \"dec\" = 'ac']", e.getMessage());
		}
	}

	@Ignore
	@Test
	public void testAddColumnErrors() {
		DbAdapter dbAdapter = getAdapter(dbFile);
		try {
			DataType aCol = new DataType("col", Double.class);
			EmbeddedDbUtil.addColumn(dbAdapter, aCol, "\"dummy\" + 3");
			Assert.fail("test bad column name in expression failed");
		}catch (DataAccessException e) {
			Assert.assertEquals("[dummy] not found; SQL=[UPDATE DATA SET \"col\" = \"dummy\" + 3]", e.getMessage());
		}

		try {
			DataType aCol = new DataType("col", Double.class);
			EmbeddedDbUtil.addColumn(dbAdapter, aCol, "'abc'");
			Assert.fail("test column data type mismatch failed");		// setting string 'abc' into a double column
		}catch (DataAccessException e) {
			Assert.assertEquals("Type mismatch; SQL=[UPDATE DATA SET \"col\" = 'abc']", e.getMessage());
		}

		try {
			dbAdapter.close(true); 	// close the database and delete the file.  this should not happen under normal operation
			DataType aCol = new DataType("col", Double.class);
			EmbeddedDbUtil.addColumn(dbAdapter, aCol, "'abc'");
			Assert.fail("test stale database file failed");
		}catch (DataAccessException e) {
			Assert.assertEquals("TABLE out-of-sync; Reload table to resume", e.getMessage());
		}
	}

	@Test
	public void testSerializer() {
		// array of boolean
		Object in = new Boolean[]{true, false, true};
		String s = EmbeddedDbUtil.serialize(in);
		Object d = EmbeddedDbUtil.deserialize(s);
		if (d instanceof Boolean[] v) {
			Assert.assertArrayEquals(v, (Boolean[])in);
		} else Assert.fail("Deserialized type Boolean mismatch");

		// array of double
		in = new Double[]{1.0, 2.0, 3.0};
		s = EmbeddedDbUtil.serialize(in);
		d = EmbeddedDbUtil.deserialize(s);
		if (d instanceof Double[] v) {
			Assert.assertArrayEquals(v, (Double[])in);
		} else Assert.fail("Deserialized type Double mismatch");

		// array of integer
		in = new Integer[]{1, 2, 3};
		s = EmbeddedDbUtil.serialize(in);
		d = EmbeddedDbUtil.deserialize(s);
		if (d instanceof Integer[] v) {
			Assert.assertArrayEquals(v, (Integer[])in);
		} else Assert.fail("Deserialized type Integer mismatch");
	}
}