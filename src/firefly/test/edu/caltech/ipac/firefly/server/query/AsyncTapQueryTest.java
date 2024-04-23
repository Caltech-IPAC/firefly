/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.firefly.core.background.JobManager;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.messaging.JsonHelper;
import edu.caltech.ipac.table.DataGroup;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsyncTapQueryTest extends ConfigTest {

	@BeforeClass
	public static void setUp() {
		// needed by test irsa2massTapTest because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
		setupServerContext(null);
	}

	/**
	 * test
	 */
	@Test
	public void testSuccess() {
		try {
			TableServerRequest req = new TableServerRequest(AsyncTapQuery.ID);
			req.setParam(AsyncTapQuery.SVC_URL, "https://irsa.ipac.caltech.edu/TAP");
			req.setParam(AsyncTapQuery.QUERY, "SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',210.80225,54.34894,1.0))=1");

			DataGroup results = new AsyncTapQuery().fetchDataGroup(req);

			Assert.assertTrue("has results", results.size() > 0);
			Assert.assertNotNull("has ra", results.getDataDefintion("ra"));
			Assert.assertNotNull("has dec", results.getDataDefintion("dec"));

		} catch (Exception e) {
			Assert.fail("testExecRequestQuery failed with exception: " + e.getMessage());
		}
	}

    /**
     * test results based on the constructed TableServerRequest
     */
    @Test
    public void testError() {
        try {
            TableServerRequest req = new TableServerRequest(AsyncTapQuery.ID);
            req.setParam(AsyncTapQuery.SVC_URL, "https://irsa.ipac.caltech.edu/TAP");
            req.setParam(AsyncTapQuery.QUERY, "SELECT * FROM some_dummy_table_name WHERE CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',210.80225,54.34894,1.0))=1");

            DataGroup results = new AsyncTapQuery().fetchDataGroup(req);
			Assert.fail("testError did not produce exception on error");

        } catch (Exception e) {
            Assert.assertTrue("data access exception",e instanceof DataAccessException);
            Assert.assertTrue(e.getMessage().length() > 0);
        }
    }

	/**
	 * test results based on the constructed TableServerRequest
	 */
	@Test
	public void testUwsJobInfo() {
		try {
			String query = "SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',210.80225,54.34894,1.0))=1";
			TableServerRequest req = new TableServerRequest(AsyncTapQuery.ID);
			req.setParam(AsyncTapQuery.SVC_URL, "https://irsadev.ipac.caltech.edu/TAP");
			req.setParam(AsyncTapQuery.QUERY, query);

			String jobUrl = new AsyncTapQuery().submitJob(req);

			Assert.assertNotNull(jobUrl);

			JobInfo jobInfo = AsyncTapQuery.getUwsJobInfo(jobUrl);
			JsonHelper json = JsonHelper.parse(JobManager.toJsonObject(jobInfo).toJSONString());

			Assert.assertNotNull("has jobInfo", jobInfo);
			Assert.assertNotNull("has jobId", json.getValue(null, "jobId"));
			Assert.assertNotNull("has parameters", json.getValue(null, "parameters"));
			Assert.assertNotNull("has phase", json.getValue(null, "phase"));
			Assert.assertNotNull("has executionDuration", json.getValue(null, "executionDuration"));
			Assert.assertEquals(query, json.getValue("", "parameters", "query"));


			// test bad job
			try {
				req.setParam("QUERY", "SELECT * FROM dumm_table where dummy = 'dummy'");
				new AsyncTapQuery().submitJob(req);
				Assert.fail("should have thrown exception");
			} catch (Exception e) {
				Assert.assertNotNull("should fail with error message", e.getMessage());
			}

		} catch (Exception e) {
			Assert.fail("testExecRequestQuery failed with exception: " + e.getMessage());
		}
	}

}