/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.query.lsst.LSSTQuery;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Test that we can get catalogs and image metadata from DAX dbserv, when it's available 
 */
public class LSSTDbServTest extends ConfigTest {

	private static String [] queries = {
			//catalog searches
			"SELECT * FROM W13_sdss_v2.sdss_stripe82_01.RunDeepSource WHERE qserv_areaspec_circle(9.469,-1.152,0.01)",
			"SELECT * FROM \"//lsst-qserv-master01:4040\".wise_00.allwise_p3as_mep WHERE qserv_areaspec_circle(9.469,-1.152,0.01)",
			"SELECT * FROM W13_sdss_v2.sdss_stripe82_01.RunDeepSource WHERE qserv_areaspec_box(9.5,-1.23,9.6,-1.22)",
			"SELECT * FROM W13_sdss_v2.sdss_stripe82_01.RunDeepSource WHERE qserv_areaspec_ellipse(9.469,-1.152,58,36,0)",
			"SELECT * FROM W13_sdss_v2.sdss_stripe82_01.RunDeepForcedSource WHERE qserv_areaspec_poly(9.4,-1.2,9.5,-1.2,9.4,-1.1)",
			// image metadata searches
			"SELECT * FROM W13_sdss_v2.sdss_stripe82_01.DeepCoadd WHERE scisql_s2PtInCPoly(9.462, -1.152, corner1Ra, corner1Decl, corner2Ra, corner2Decl, corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1",
			"SELECT * FROM \"//lsst-qserv-master01:4040\".wise_00.allwise_p3am_cdd WHERE scisql_s2PtInCPoly(9.462, -1.152, ra1, dec1, ra2, dec2, ra3, dec3, ra4, dec4)=1",
			"SELECT * FROM W13_sdss_v2.sdss_stripe82_01.Science_Ccd_Exposure WHERE " +
					"(scisql_s2PtInCPoly(9.5, -1.23, corner1Ra, corner1Decl, corner2Ra, corner2Decl, corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1) AND " +
					"(scisql_s2PtInCPoly(9.5, -1.22, corner1Ra, corner1Decl, corner2Ra, corner2Decl, corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1) AND " +
					"(scisql_s2PtInCPoly(9.6, -1.23, corner1Ra, corner1Decl, corner2Ra, corner2Decl, corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1) AND " +
					"(scisql_s2PtInCPoly(9.6, -1.22, corner1Ra, corner1Decl, corner2Ra, corner2Decl, corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1)",
			"SELECT * FROM \"//lsst-qserv-master01:4040\".wise_00.allsky_2band_p1bm_frm WHERE " +
					"(scisql_s2PtInCPoly(9.5, -1.23, ra1, dec1, ra2, dec2, ra3, dec3, ra4, dec4)=1) AND " +
					"(scisql_s2PtInCPoly(9.5, -1.22, ra1, dec1, ra2, dec2, ra3, dec3, ra4, dec4)=1) AND " +
					"(scisql_s2PtInCPoly(9.6, -1.23, ra1, dec1, ra2, dec2, ra3, dec3, ra4, dec4)=1) AND " +
					"(scisql_s2PtInCPoly(9.6, -1.22, ra1, dec1, ra2, dec2, ra3, dec3, ra4, dec4)=1)",
            "SELECT * FROM W13_sdss_v2.sdss_stripe82_01.Science_Ccd_Exposure WHERE " +
                    "(scisql_s2PtInBox(corner1Ra, corner1Decl, 0, -1, 5, 1)=1) AND " +
                    "(scisql_s2PtInBox(corner2Ra, corner2Decl,0, -1, 5, 1) =1) AND " +
                    "(scisql_s2PtInBox(corner3Ra, corner3Decl, 0, -1, 5, 1)=1) AND " +
                    "(scisql_s2PtInBox(corner4Ra, corner4Decl, 0, -1, 5, 1)=1)",
			"SELECT * FROM \"//lsst-qserv-master01:4040\".wise_00.allwise_p3am_cdd WHERE " +
                    "(scisql_s2PtInBox(ra1, dec1, 0, -1, 5, 1)=1) AND " +
                    "(scisql_s2PtInBox(ra2, dec2,0, -1, 5, 1) =1) AND " +
                    "(scisql_s2PtInBox(ra3, dec3, 0, -1, 5, 1)=1) AND " +
                    "(scisql_s2PtInBox(ra4, dec4, 0, -1, 5, 1)=1)",
            "SELECT count(*) FROM W13_sdss_v2.sdss_stripe82_01.RunDeepForcedSource WHERE objectId=3448068867358968"
            
	};
	

	/**
	 * test that we can obtain the data for all catalog queries
	 */
	@Test
	public void testCatalogQueries() {
		try {

		    if (daxAvailable() && dbservAvailable()) {
                boolean passed;
                for (String query : queries) {
                    passed = getJsonData(query);
                    Assert.assertTrue("FAILED: "+query, passed);
                }
            }
		} catch (Exception e) {
			Assert.fail("testCatalogQueries failed with exception: " + e.getMessage());
		}
	}


	private static boolean getJsonData(String query) {
		try {
			String sql = "query=" + URLEncoder.encode(query, "UTF-8");

			String url = LSSTQuery.DBSERVURL;
			File file = File.createTempFile("lssttest", "json");
			file.deleteOnExit();
			Map<String, String> requestHeader = new HashMap<>();
			requestHeader.put("Accept", "application/json");

			long cTime = System.currentTimeMillis();
			FileInfo fileData = URLDownload.getDataToFileUsingPost(new URL(url), sql, null, requestHeader, file, null, 180);
			LOG.info("SQL query took " + (System.currentTimeMillis() - cTime) + "ms");
			LOG.info(query);

			if (fileData.getResponseCode() >= 400) {
				String err = LSSTQuery.getErrorMessageFromFile(file);
				err = "[DAX] " + (err == null ? fileData.getResponseCodeMsg() : err);
				LOG.error(err, query);
				return false;
			} else {
				return true;
			}
		} catch (FailedRequestException | IOException e) {
			LOG.error(e, query);
			return false;
		}
	}


	/**
	 * Is dbserv running?
	 * @return true if dbserv is accessible and running
	 */
	private static boolean dbservAvailable() {
		try {
			URL urlServer = new URL(LSSTQuery.DBSERVURL);
			HttpURLConnection urlConn = (HttpURLConnection) urlServer.openConnection();
			urlConn.setConnectTimeout(3000); // 3 seconds timeout
			urlConn.connect();
			return urlConn.getResponseCode() == 405; //HTTP 405 Method Not Allowed
		} catch (IOException e) {
		    LOG.info("dbserv is not available "+e.getMessage());
			return false;
		}
	}

	/**
	 * Is DAX running?
	 * @return true if DAX is accessible and running
	 */
	public static boolean daxAvailable() {
		try {
			URL urlServer = new URL("http://"+ LSSTQuery.HOST +":"+LSSTQuery.PORT+"/api");
			HttpURLConnection urlConn = (HttpURLConnection) urlServer.openConnection();
			urlConn.setConnectTimeout(3000); // 3 seconds timeout
			urlConn.connect();
			return urlConn.getResponseCode() == 200;
		} catch (IOException e) {
            LOG.info("DAX is not available "+e.getMessage());
			return false;
		}
	}
}