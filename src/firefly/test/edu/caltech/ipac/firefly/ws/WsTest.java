package edu.caltech.ipac.firefly.ws;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.*;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.apache.commons.io.FileUtils;
import org.apache.http.entity.ContentType;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by ejoliet on 6/15/17.
 */
public class WsTest extends ConfigTest {


    private static String relFolder = null;
    private static WorkspaceManager man;
    private static String userKey = WS_USER_ID;
    private SimpleDateFormat df;
    private static List<String> resourceList;

    @BeforeClass
    public static void init() {

        relFolder = "123/321/";
        resourceList = new ArrayList<>();
    }

    @Before
    public void setUp() {
        man = WorkspaceFactory.getWorkspaceHandler().withCredentials(getWsCredentials());
        df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
    }
    @Ignore
    @Test
    public void testHandler() throws WsException, ParseException {


//        UserInfo ui = UserInfo.newGuestUser();

        //create folder under home (.)
        WsResponse f = man.createParent(relFolder);
        LOG.info("new directory: " + String.valueOf(f));

        WspaceMeta m = new WspaceMeta(null, relFolder);
        m.setProperty("test1", "an awesome idea");
        m.setProperty("test", null);
        man.setMeta(m);

        WspaceMeta metaRetrieved = man.getMeta(WsUtil.ensureUriFolderPath(relFolder), WspaceMeta.Includes.ALL_PROPS);
        LOG.info(metaRetrieved.getNodesAsString());
        assertTrue("Doesn't contains meta ", metaRetrieved.getProperty("test1").equals("an awesome idea"));

        String name1 = "gaia-binary2.vot";
        File testFile = FileLoader.resolveFile(WsTest.class, name1);
        String ufilePath = putFile(name1, man);

        //  ufilePath = putFile("x.fits", man);// Fails for now: Request Entity Too Large in pubspace
        WspaceMeta meta = new WspaceMeta(null, ufilePath);
        meta.setProperty("added_by", userKey);
        man.setMeta(meta);

        metaRetrieved = man.getMeta(ufilePath, WspaceMeta.Includes.ALL_PROPS);
        LOG.info(metaRetrieved.getNodesAsString());
        assertTrue("Doesn't contains meta ", metaRetrieved.getProperty("added_by").equals(userKey));

        String renamed = "gaia-binary2-renamed.vot";
        man.renameFile(ufilePath, renamed, true);

        resourceList.add(ufilePath.replaceAll(name1, renamed));

        metaRetrieved = man.getMeta(WsUtil.ensureUriFolderPath(relFolder) + renamed, WspaceMeta.Includes.ALL_PROPS);
        LOG.info(metaRetrieved.getNodesAsString());

        //Date modDate = df.parse(metaRetrieved.getLastModified());

        assertTrue("Doesn't contains meta ", metaRetrieved != null);

        //Should remain unchanged
        String renamedSame = renamed;
        man.renameFile(ufilePath, renamedSame, false);
//        Date modDateRenamedSame = df.parse(man.getMeta(ufilePath, WspaceMeta.Includes.ALL_PROPS).getLastModified());
//        assertTrue(modDate.equals(modDateRenamedSame));
        assertTrue(man.getMeta(WsUtil.ensureUriFolderPath(relFolder) + renamedSame, WspaceMeta.Includes.ALL_PROPS).getLastModified().equals(man.getMeta(WsUtil.ensureUriFolderPath(relFolder) + renamed, WspaceMeta.Includes.ALL_PROPS).getLastModified()));
    }
    @Ignore
    @Test
    public void testGet() throws WsException {

        //Check what we put is what we get
        File original = FileLoader.resolveFile(WsTest.class, "gaia-binary.vot");
        String name2 = original.getName();

        // Make sure is uploaded first to test to get it
        String ufilePath = putFile(name2, man);

        WspaceMeta meta = man.getMeta("/", WspaceMeta.Includes.ALL_PROPS);

        // Change to INFO level in log4j-test.properties to see output.
        // log4j.logger.test=INFO, A1

        LOG.info(meta.getNodesAsString());

        meta = man.getMeta(ufilePath, WspaceMeta.Includes.ALL_PROPS);

        LOG.info(meta.toString());

        try {

            File gotFromWS = getFile(name2, man);

            //test.deleteOnExit();

            assertTrue("Empty!", gotFromWS.length() > 0);
            assertTrue(gotFromWS.length() == original.length());
            assertTrue(meta.getSize() == original.length());
            assertTrue("Empty!", FileUtils.contentEquals(gotFromWS, original));
            assertTrue("wrong name!", gotFromWS.getName().startsWith(name2));
        } catch (WsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Ignore
    @Test
    public void delete() throws WsException {

        //Check what we put is what we get
        File original = FileLoader.resolveFile(WsTest.class, "gaia-binary.vot");
        String name2 = original.getName();

        // Make sure is uploaded first to test to get it
        String ufilePath = putFile(name2, man);

        WspaceMeta metaRetrieved = man.getMeta(ufilePath, WspaceMeta.Includes.ALL_PROPS);

        assertTrue(metaRetrieved != null); //exists

        man.delete(ufilePath);

        metaRetrieved = man.getMeta(ufilePath, WspaceMeta.Includes.ALL_PROPS);

        assertTrue(metaRetrieved == null); //removed

    }

    @AfterClass
    public static void cleanUp() throws IOException {
        LOG.info("Cleaning " + userKey);
//
////      Clean up using resource list
//
//          WorkspaceManager wsm = WorkspaceFactory.getWorkspaceHandler().withCredentials(new WsCredentials(userKey));
//        Iterator<String> iterator = resourceList.iterator();
//        while (iterator.hasNext()) {
//            String path = iterator.next();
//            del(path);
//        }
//

        // OR using getSuggestedList from manager get children of root

        WsResponse response = man.getList("/", 1);
        List meta = response.getWspaceMeta();

        Iterator iterator1 = meta.iterator();
        while (iterator1.hasNext()) {
            WspaceMeta next = (WspaceMeta) iterator1.next();

            del(next.getRelPath());
        }

    }

    private static void del(String path) throws WsException {
        LOG.info("Deleting " + path);
        WsResponse response = man.delete(path);
        LOG.info(response.getStatusText());
        if (!response.getStatusCode().equals("200")) {
            return;
        }
//        int idx = path.lastIndexOf("/");
//        if (idx > 0) {
//            String parentPath = path.substring(0, idx+1);
//            del(parentPath);
//        }
    }

    private File getFile(String name, WorkspaceManager man) throws IOException, WsException {

        String ufilePath = WsUtil.ensureUriFolderPath(relFolder) + name;

        File tmpFile = File.createTempFile(name, "", new File("."));

        tmpFile.deleteOnExit();

        WsResponse responseFile = man.getFile(ufilePath, tmpFile);
        assertTrue("Not good code " + responseFile.getStatusCode(), responseFile.getStatusCode().equals("200"));
        return tmpFile;
    }

    private String putFile(String name, WorkspaceManager man) throws WsException {
        File testFile = FileLoader.resolveFile(WsTest.class, name);

        String ufilePath = WsUtil.ensureUriFolderPath(relFolder) + testFile.getName();

        if (!resourceList.contains(ufilePath)) {
            resourceList.add(ufilePath);
        }
        man.putFile(relFolder, testFile, ContentType.DEFAULT_BINARY.getMimeType());
        return ufilePath;
    }

}
