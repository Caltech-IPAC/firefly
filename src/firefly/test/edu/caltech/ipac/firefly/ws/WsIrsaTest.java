package edu.caltech.ipac.firefly.ws;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.WorkspaceFactory;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.firefly.server.ws.WsException;
import edu.caltech.ipac.firefly.server.ws.WsResponse;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Test SSO @ IRSA
 * Created by ejoliet on 6/26/17.
 */
public class WsIrsaTest extends ConfigTest {


    private static File f;
    private static WorkspaceManager workspaceManager = null;

    @Before
    public void init() {
        //Check first if i can run test
        assumeTrue(getWsCredentials()!=null);
        WsCredentials cred = getWsCredentials();
        try {
            f = pickFile(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //For TESTING SSOspace,
      //f = FileLoader.resolveFile(WsIrsaTest.class, "wstest.fits"); // Test adding fits >1Mb (only SSO quota ok)


        //TODO this doesn't overwrite, use ./config/app-test.prop if you need
        //AppProperties.setProperty("workspace.host.url", "https://irsadev.ipac.caltech.edu");

        workspaceManager = WorkspaceFactory.getWorkspaceHandler().withCredentials(cred);

    }

    @Test
    public void testSSOAuth() throws ClassNotFoundException, IOException {
        LOG.info(workspaceManager.getWsHome());

        workspaceManager.putFile("", f, null);
        WsResponse r = workspaceManager.getList("", 1);
        if (!r.getStatusCode().equals("200")) {
            LOG.error(r.getStatusText());
        }
        List<WspaceMeta> wsmeta = r.getWspaceMeta();

        if (wsmeta != null) {
            for (WspaceMeta meta : wsmeta) {
                LOG.info(meta.getNodesAsString());
                if (meta.getRelPath().endsWith(f.getName())) {
                    assertTrue(meta.getSize() == f.length());
                }
            }
        }
    }


    @After
    public void clean() throws WsException {
        if(workspaceManager!=null){
            LOG.info("deleting " + f.getName());
            workspaceManager.delete(f.getName());
        }

    }

    private static File pickFile(int idx) throws Exception {
        File testPath = new File(FileLoader.getDataPath(WsIrsaTest.class));
        File file = testPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.endsWith(".fits"); // Can't use fits in pubspace because > 1Mb irsa policy storage
            }
        })[idx];
        return file;
    }
}
