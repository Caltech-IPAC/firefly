package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.VoTableUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Test periodogram dev api
 * WARNING: might fail if bacchus not running
 * Created by ejoliet on 12/5/16.
 */
public class PostDownloadTest extends ConfigTest {

    private File tmpRawTable;

    @Before
    public void setUp() {
        try {
            // Download sample raw table to local unit test
            tmpRawTable = File.createTempFile("raw-temp-", ".tbl", new File("."));
            URL demo = new URL("http://web.ipac.caltech.edu/staff/ejoliet/demo/AllWISE-MEP-m82-2targets-10arsecs.tbl");
            URLConnection uc = URLDownload.makeConnection(demo);
            URLDownload.getDataToFile(uc, tmpRawTable);
            tmpRawTable.deleteOnExit();
        } catch (IOException e) {
            LOG.error(e);
        } catch (FailedRequestException e) {
            LOG.error(e);
        }

    }

    @Ignore
    @Test
    public void testMultipart() {
        //"curl -F upload=@/hydra/workarea/firefly/temp_files/upload_7350632974912517382.tbl -F x=mjd -F y=w1mpro_ep http://bacchus.ipac.caltech.edu:9027/cgi-bin/periodogram/nph-periodogram_api -o pepe.xml"
        try {
            URL url = new URL("https://irsa.ipac.caltech.edu/cgi-bin/periodogram/nph-periodogram_api");
            check(url);
            MultiPartPostBuilder posBuilder = new MultiPartPostBuilder(url.toString());
            String src = tmpRawTable.getAbsolutePath();
            posBuilder.addFile("upload", new File(src));
            posBuilder.addParam("x", "mjd");
            posBuilder.addParam("y", "w1mpro_ep");
            posBuilder.addParam("peaks", "55");
            File fileTmp = File.createTempFile("lc-result-temp-", ".xml", new File("."));
            fileTmp.deleteOnExit();
            BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(fileTmp), 10240);
            posBuilder.post(writer);
            writer.close();

            DataGroup[] dataGroups = VoTableUtil.voToDataGroups(fileTmp.getAbsolutePath());

            Assert.assertEquals("should be 2 tables but found " + dataGroups.length, dataGroups.length, 2);
        } catch (ConnectException e) {
            LOG.error(e.getMessage());
        } catch (DataAccessException e) {
            LOG.error(e, e.getMessage());
        } catch (FileNotFoundException e) {
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
    }

    void check(URL url) throws DataAccessException {
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();
        } catch (IOException e) {
            throw new DataAccessException(url.toString() + " not reachable ");
        }
    }
}
