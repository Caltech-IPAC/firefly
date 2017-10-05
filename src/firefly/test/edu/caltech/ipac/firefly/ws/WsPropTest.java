package edu.caltech.ipac.firefly.ws;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.ResourceNotFoundException;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.WorkspaceFactory;
import edu.caltech.ipac.firefly.server.ws.WorkspaceHandler;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.firefly.server.ws.WsException;
import edu.caltech.ipac.util.AppProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by ejoliet on 6/16/17.
 */
public class WsPropTest extends ConfigTest {

    private String urlHost;
    private String prot;
    private WorkspaceHandler ws;

    @Before
    public void init() throws WsException {

        urlHost = AppProperties.getProperty("workspace.host.url");
        prot = AppProperties.getProperty("workspace.protocol.irsa");
        ws = WorkspaceFactory.getWorkspaceHandler();
    }
    @Ignore
    @Test
    public void testProps() {
        try {
            WorkspaceManager.PROPS prop = WorkspaceManager.PROPS.ROOT_URL;
            WorkspaceManager m = ws.withCredentials(getWsCredentials());
            Assert.assertTrue(m.getProp(prop).equals(urlHost));
            Assert.assertTrue(m.getProp(WorkspaceManager.PROPS.PROTOCOL).equalsIgnoreCase(prot));


        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    @Ignore
    @Test
    public void testPoolWsManager() {
        try {

            // get 2 times the workspace manager, the object should be the same if the user key is the same
            WorkspaceManager ws1 = ws.withCredentials(getWsCredentials());

            WorkspaceManager ws2 = ws.withCredentials(getWsCredentials());

            Assert.assertTrue(ws1.getCredentials().getWsId().equals(ws2.getCredentials().getWsId()));
            Assert.assertTrue(ws1.getClass().equals(ws2.getClass()));

            // get 2 times the workspace manager for same user but different protocol, the object should be different
            WorkspaceManager ws3 = ws.withCredentials(getWsCredentials());

            WorkspaceManager ws4 = WorkspaceFactory.getWorkspaceHandler(WorkspaceManager.PROTOCOL.LOCAL).
                    withCredentials(getWsCredentials());

            Assert.assertFalse(ws3.getClass().equals(ws4.getClass()));


        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    @Ignore
    @Test(expected = ResourceNotFoundException.class)
    public void testNotImplementedVospace() {

        WorkspaceFactory.getWorkspaceHandler(WorkspaceManager.PROTOCOL.VOSPACE).withCredentials(getWsCredentials());

    }

}
