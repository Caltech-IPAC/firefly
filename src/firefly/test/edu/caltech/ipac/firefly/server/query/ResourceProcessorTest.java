/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbMonitor;
import edu.caltech.ipac.firefly.server.query.tables.IpacTableFromSource;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.JsonTableUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static edu.caltech.ipac.firefly.data.ServerParams.SOURCE;
import static edu.caltech.ipac.firefly.server.query.ResourceProcessor.*;

public class ResourceProcessorTest extends ConfigTest {

	@Before
	public void setUp() {
		setupServerContext(null);
	}

	@After
	public void tearDown() {
		DbMonitor.cleanup(true, true);
	}

	@Test
	public void testGlobalScope() {
		try {
			TableServerRequest tblReq = new TableServerRequest(IpacTableFromSource.PROC_ID);
			tblReq.setParam(SOURCE, "https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl");

			TableServerRequest req = new TableServerRequest(ResourceProcessor.PROC_ID);
			req.setParam(QueryUtil.SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(tblReq).toJSONString());
			req.setParam(SCOPE, GLOBAL);
			req.setParam(ACTION, CREATE);

			DataGroupPart results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("DB loaded", 80, results.getRowCount());
			Assert.assertEquals("Only 1 row returned", 1, results.getData().size());

			req.setParam(ACTION, QUERY);
			req.setPageSize(25);
			results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("returns 25 rows", 25, results.getData().size());

			req.setParam(ACTION, DELETE);
			results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("delete DB", 0, results.getData().size());

		} catch (Exception e) {
			Assert.fail("testGlobalScope failed with exception: " + e.getMessage());
		}
	}

	@Test
	public void testUserScope() {
		try {
			TableServerRequest tblReq = new TableServerRequest(IpacTableFromSource.PROC_ID);
			tblReq.setParam(SOURCE, "https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl");

			TableServerRequest req = new TableServerRequest(ResourceProcessor.PROC_ID);
			req.setParam(QueryUtil.SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(tblReq).toJSONString());
			req.setParam(SCOPE, USER);
			req.setParam(ACTION, CREATE);

			DataGroupPart results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("DB loaded", 80, results.getRowCount());

			ServerContext.getRequestOwner().setUserKey("someone without access");
			req.setParam(ACTION, QUERY);
			results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("Access denied", "Access denied", results.getErrorMsg());
			Assert.assertEquals("No Data Returned", 0, results.getRowCount());
		} catch (Exception e) {
			Assert.fail("testGlobalScope failed with exception: " + e.getMessage());
		}
	}

	@Test
	public void testProtectedScope() {
		try {
			TableServerRequest tblReq = new TableServerRequest(IpacTableFromSource.PROC_ID);
			tblReq.setParam(SOURCE, "https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl");

			TableServerRequest req = new TableServerRequest(ResourceProcessor.PROC_ID);
			req.setParam(QueryUtil.SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(tblReq).toJSONString());
			req.setParam(SCOPE, PROTECTED);
			req.setParam(SECRET, "top secret");
			req.setParam(ACTION, CREATE);

			DataGroupPart results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("DB loaded", 80, results.getRowCount());

			req.setParam(ACTION, QUERY);
			req.setPageSize(25);
			results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("access with password", 25, results.getData().size());


			req.setParam(SECRET, "bad secret");
			results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("Access denied", "Access denied", results.getErrorMsg());
			Assert.assertEquals("No Data Returned", 0, results.getRowCount());
		} catch (Exception e) {
			Assert.fail("testGlobalScope failed with exception: " + e.getMessage());
		}
	}

	@Test
	public void testRemoveResource() {
		try {
			TableServerRequest tblReq = new TableServerRequest(IpacTableFromSource.PROC_ID);
			tblReq.setParam(SOURCE, "https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl");

			TableServerRequest req = new TableServerRequest(ResourceProcessor.PROC_ID);
			req.setParam(QueryUtil.SEARCH_REQUEST, JsonTableUtil.toJsonTableRequest(tblReq).toJSONString());
			req.setParam(SCOPE, GLOBAL);
			req.setParam(SECRET, "top secret");
			req.setParam(ACTION, CREATE);

			DataGroupPart results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("DB loaded", 80, results.getRowCount());

			req.setParam(ACTION, DELETE);
			req.setParam(SECRET, "");		// try deleting without secret
			results = new SearchManager().getDataGroup(req);
			Assert.assertEquals("Access denied", "Access denied", results.getErrorMsg());

			req.setParam(ACTION, DELETE);
			req.setParam(SECRET, "top secret");		// try again with the correct secret
			results = new SearchManager().getDataGroup(req);
			Assert.assertNull(null, results.getErrorMsg());

		} catch (Exception e) {
			Assert.fail("testGlobalScope failed with exception: " + e.getMessage());
		}
	}

}