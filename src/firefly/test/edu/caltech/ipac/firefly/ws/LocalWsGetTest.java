package edu.caltech.ipac.firefly.ws;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.LocalFSWorkspace;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.firefly.server.ws.WsException;
import edu.caltech.ipac.firefly.server.ws.WsResponse;
import org.apache.commons.io.FileUtils;
import org.apache.http.entity.ContentType;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static edu.caltech.ipac.firefly.util.FileLoader.resolveFile;

/**
 * Created by ejoliet on 6/16/17.
 */
public class LocalWsGetTest extends ConfigTest {


    private WsCredentials cred;
    private WorkspaceManager wsm;
    private String testUri;
    private String testFileName;
    private File testFile;
    private long size;

    @Before
    public void putInit() throws WsException {
        cred = new WsCredentials(WS_USER_ID);
        testFileName = "gaia-binary.vot";
        testFile = resolveFile(LocalWsGetTest.class, testFileName);
        size = testFile.length();
        testUri = "tmp1/";
    }

    @Test
    public void testPutGet() throws WsException {


        try {
            //Test class impl
            put(LocalFSWorkspace.class);
            byte[] bytes = get(testUri + "/" + testFile.getName());
            Assert.assertTrue("Size uploaded " + size + " not the same found " + bytes.length, bytes.length == size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void put(Class cls) throws IOException {

        wsm = new LocalFSWorkspace(cred);

        LOG.info(wsm.getClass().getCanonicalName());


        WsResponse wsResponse = wsm.putFile(testUri,
                testFile,
                ContentType.DEFAULT_BINARY.getMimeType());

        Assert.assertTrue("Upload went wrong, status code <200 " + wsResponse.getStatusCode(), Integer.parseInt(wsResponse.getStatusCode()) >= 200);
        Assert.assertTrue("Uploaded file name is wrong " + wsResponse.getResponse(), wsResponse.getResponse().endsWith(testFile.getName()));


        File tmp = File.createTempFile("test", "vot");
        tmp.deleteOnExit();
        wsResponse = wsm.getFile(testUri + File.separator +
                testFile.getName(), tmp);

        Assert.assertTrue(tmp.length() == testFile.length());

        Assert.assertTrue(tmp.getAbsolutePath().equals(wsResponse.getResponse()));


    }


    byte[] get(String uri) throws Exception {
        return Files.readAllBytes(Paths.get("", uri));
    }

    @After
    public void clean() {
        try {
            FileUtils.deleteDirectory(new File(wsm.getWsHome() + File.separator + testUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MyGetMethod extends DavMethodBase {
        public MyGetMethod(String uri) {
            super(uri);
        }

        public String getName() {
            return DavMethods.METHOD_GET;
        }

        public boolean isSuccess(int statusCode) {
            return statusCode == 200;
        }
    }
}
