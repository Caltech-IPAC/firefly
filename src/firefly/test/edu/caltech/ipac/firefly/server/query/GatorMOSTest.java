/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.catquery.GatorMOS;
import edu.caltech.ipac.table.DataGroup;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GatorMOSTest extends ConfigTest {

	@BeforeClass
	public static void setUp() {
		// needed because it's dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
		setupServerContext(null);
	}

	@Test
	public void testMPC() throws DataAccessException {

		// Asteroid example with MPC input at time of WISE 4-band observation of Bamberga:
		TableServerRequest treq = new TableServerRequest(GatorMOS.PROC_ID);
		treq.setParam(GatorMOS.Param.catalog.name(), "allsky_4band_p1bs_psd");
		treq.setParam(GatorMOS.Param.moradius.name(), "5");
		treq.setParam(GatorMOS.Param.mobj.name(), "mpc");
		treq.setParam(GatorMOS.Param.mobjtype.name(), "Asteroid");
		treq.setParam(GatorMOS.Param.mpc.name(), "00324 6.82 0.09 K103I 64.7389713 43.9047086 327.9898280 11.1080776 0.33709263 0.2240550 2.684723515 0 MPO344295 2289 63 1892-2015 0.50 M-v 38h MPCLINUX 0000 (324) Bamberga 20150614");

		DataGroup results = new GatorMOS().fetchDataGroup(treq);
		Assert.assertNotNull(results);
		Assert.assertTrue(results.size() > 0);

	}

	@Test
	public void testOBT() throws DataAccessException {

		// Asteroid example with orbital elements at time of WISE 4-band observation of Bamberga:
		TableServerRequest treq = new TableServerRequest(GatorMOS.PROC_ID);
		treq.setParam(GatorMOS.Param.catalog.name(), "allsky_4band_p1bs_psd");
		treq.setParam(GatorMOS.Param.moradius.name(), "5");
		treq.setParam(GatorMOS.Param.mobj.name(), "obt");
		treq.setParam(GatorMOS.Param.mobjtype.name(), "Comet");
		treq.setParam(GatorMOS.Param.perih_time.name(), "2455424.64772");
		treq.setParam(GatorMOS.Param.mobjper.name(), "153.49099443");
		treq.setParam(GatorMOS.Param.mobjasc.name(), "113.21175889");
		treq.setParam(GatorMOS.Param.mobjinc.name(), "12.87619139");
		treq.setParam(GatorMOS.Param.mobjecc.name(), "0.53379824");
		treq.setParam(GatorMOS.Param.perih_dist.name(), "1.49452503");
		treq.setParam(GatorMOS.Param.mobjepo.name(), "55368.0");
		treq.setParam(GatorMOS.Param.mobjdsg.name(), "P/2010 N1 (WISE)");

		DataGroup results = new GatorMOS().fetchDataGroup(treq);
		Assert.assertNotNull(results);
		Assert.assertTrue(results.size() > 0);

	}

	@Test
	public void testBadRequest() throws DataAccessException {

		// Asteroid example with orbital elements at time of WISE 4-band observation of Bamberga:
		TableServerRequest treq = new TableServerRequest(GatorMOS.PROC_ID);
		treq.setParam(GatorMOS.Param.catalog.name(), "allsky_4band_p1bs_psd");
		treq.setParam(GatorMOS.Param.moradius.name(), "5");
		treq.setParam(GatorMOS.Param.mobj.name(), "BAD_INPUT_TYPE");

		try {
			new GatorMOS().fetchDataGroup(treq);
			Assert.fail("This request should fail due to bad input type, but is not");
		} catch (Exception ignored) {}

	}
}