/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.HsqlDbAdapter;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class EmbeddedDbUtilTest extends ConfigTest {

	public static File dbFile;
	public static File testFile;

	@BeforeClass
	public static void setUp() {
		try {
			// needed by test testGetSelectedData because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
			setupServerContext();

			dbFile = File.createTempFile("TestDb_", ".hsql");
			dbFile.deleteOnExit();

			HsqlDbAdapter dbAdapter = new HsqlDbAdapter();
			testFile = FileLoader.resolveFile(EmbeddedDbUtilTest.class, "/embedded_db_test.tbl");
			DataGroup data = IpacTableReader.readIpacTable(testFile, "test");
			EmbeddedDbUtil.createDbFile(dbFile,dbAdapter);
			EmbeddedDbUtil.ingestDataGroup(dbFile, data, dbAdapter, "data");
		} catch (IpacTableException | IOException e) {
			Assert.fail("Unable to ingest data into the database");
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
			DataGroupPart res = EmbeddedDbUtil.execRequestQuery(req, dbFile, "data");
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
			res = EmbeddedDbUtil.execRequestQuery(req, dbFile, "data");
			data = res.getData();
			Assert.assertEquals(0.0744, data.get(0).getDataElement("sigra"));
			Assert.assertEquals(0.0413, data.get(9).getDataElement("sigra"));

			// make sure meta information is still good after sorting
			testMeta(data);

			// test filter;  reset request object
			req = new TableServerRequest("n/a (id not used)");
			req.setSortInfo(new SortInfo(SortInfo.Direction.DESC, "sigra"));
			req.setFilters(Arrays.asList(String.format("\"%s\" in (%f, %f, %f)", "sigra", 0.0413, 0.0744, 12345.6)));
			res = EmbeddedDbUtil.execRequestQuery(req, dbFile, "data");
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
		DataGroup data = EmbeddedDbUtil.execQuery(dbAdapter, dbFile, sql, "data");

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

		IpacTableParser.MappedData selVals = EmbeddedDbUtil.getSelectedMappedData(treq, Arrays.asList(1, 3, 5), "dec", "clon+1", "RA(deg)");

		Assert.assertEquals("13h29m56.55s", selVals.get(1, "clon+1"));
		Assert.assertEquals(47.2321996, selVals.get(3, "dec"));
		Assert.assertEquals(202.4680189, selVals.get(5, "RA(deg)"));
	}

	private void testMeta(DataGroup data) {
		// test meta
		Assert.assertEquals("'ORIGIN value'", data.getAttribute("ORIGIN").getValue());
		Assert.assertEquals("'SQL value'", data.getAttribute("SQL").getValue());
		Assert.assertEquals("%.6g", data.getAttribute("col.dec.FmtDisp").getValue());

		// test column meta
		DataType dt = data.getDataDefintion("dec");
		Assert.assertEquals("deg", dt.getDataUnit());
		Assert.assertEquals(Double.class, dt.getDataType());
	}

}