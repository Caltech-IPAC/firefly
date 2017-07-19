package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.ptf.PtfFileRetrieve;
import edu.caltech.ipac.firefly.server.query.ptf.PtfIbeResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by ejoliet on 7/20/17.
 */
public class PtfIbeTest extends ConfigTest {

    @Before
    public void setUp() {

        PtfIbeResolver.isTestMode = true;
    }

    @Test
    public void testPid2FilenameResolver() throws IOException, InterruptedException {

        long[] pids = new long[]{16406820, 16406821};
        File f = File.createTempFile("tmp", ".tbl", new File("."));
        f.deleteOnExit();
        PtfIbeResolver res = new PtfIbeResolver() {
            /**
             * @return
             * @throws IOException
             */
            protected File getTempFile() throws IOException {
                return f;
            }
        };
        String[] fs = res.getValuesFromColumn(pids, "pfilename");

        Assert.assertTrue(fs.length == pids.length);

    }


    @Test
    public void testPtfFileRetrive() throws IOException, DataAccessException {

        ServerRequest sr = new ServerRequest("PtfFileRetrieve");
        sr.setParam("mission", "ptf");
        sr.setParam("pid", "16406820");
        sr.setParam("schema", "images");
        sr.setParam("table", "level1");
        sr.setParam("ProductLevel", "l1");
        sr.setParam("ra_obj", "341.9706450");
        sr.setParam("dec_obj", "65.0620120");

        URL res = new PtfFileRetrieve().getURL(sr);

        LOG.debug(res.toString());
        Assert.assertTrue(new URL("https://irsa.ipac.caltech.edu/ibe/data/ptf/images/level1/proc/2013/06/16/f2/c4/p5/v1/PTF_201306163445_i_p_scie_t081605_u016406820_f02_p005137_c04.fits").
                equals(res));


        sr.setParam("doCutout", "cut");
        sr.setParam("subsize", "0.083333");

        res = new PtfFileRetrieve().getURL(sr);

        LOG.debug(res.toString());
        Assert.assertTrue(new URL("https://irsa.ipac.caltech.edu/ibe/data/ptf/images/level1/proc/2013/06/16/f2/c4/p5/v1/PTF_201306163445_i_p_scie_t081605_u016406820_f02_p005137_c04.fits?center=341.9706450,65.0620120&size=0.083333&gzip=false").
                equals(res));
    }
}
