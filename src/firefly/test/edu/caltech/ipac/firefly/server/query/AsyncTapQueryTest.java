/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.DataGroup;
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
			req.setParam("serviceUrl", "https://irsa.ipac.caltech.edu/TAP");
			req.setParam("QUERY", "SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',210.80225,54.34894,1.0))=1");
			req.setParam("PHASE","RUN");

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
            req.setParam("serviceUrl", "https://irsa.ipac.caltech.edu/TAP");
            req.setParam("QUERY", "SELECT * FROM some_dummy_table_name WHERE CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',210.80225,54.34894,1.0))=1");
            req.setParam("PHASE","RUN");

            DataGroup results = new AsyncTapQuery().fetchDataGroup(req);
			Assert.fail("testError did not produce exception on error");

        } catch (Exception e) {
            Assert.assertTrue("data access exception",e instanceof DataAccessException);
            Assert.assertTrue(e.getMessage().length() > 0);
        }
    }

}