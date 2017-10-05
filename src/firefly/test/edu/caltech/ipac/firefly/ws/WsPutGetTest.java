package edu.caltech.ipac.firefly.ws;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.http.entity.ContentType;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static edu.caltech.ipac.firefly.util.FileLoader.resolveFile;
import static org.junit.Assert.assertTrue;

/**
 * Created by ejoliet on 6/16/17.
 */
public class WsPutGetTest extends ConfigTest {


    private WsCredentials cred;
    private WorkspaceManager wsm;
    private String testUrl;
    private String testRelPathFolder;
    private String testFileName;
    private File testFile;
    private long sizeTest;

    @Before
    public void putInit() throws WsException {
        cred = new WsCredentials(WS_USER_ID);
        testFileName = "gaia-binary.vot";
        testFile = resolveFile(WsPutGetTest.class, testFileName);
        sizeTest = testFile.length();
        testRelPathFolder = "tmp-" + UUID.randomUUID().toString() + "/tmp1/tmp2/tmp3";//
        // TODO with special chars irt throws 301-move permanently status "/tmp1?/t2#$%^&*9"; ??
        wsm = WorkspaceFactory.getWorkspaceHandler().withCredentials(cred);
        LOG.info(wsm.getClass().getCanonicalName());

    }

    @After
    public void delRelFolder() throws WsException {
        wsm.delete(WsUtil.ensureUriFolderPath(testRelPathFolder.substring(0,testRelPathFolder.indexOf("/"))));
    }
    @Ignore
    @Test
    public void testPut() throws WsException {

        WsResponse wsResponse = wsm.putFile(testRelPathFolder,
                testFile,
                ContentType.DEFAULT_BINARY.getMimeType());

        assertTrue("Upload went wrong, status code <200 " + wsResponse.getStatusCode(), Integer.parseInt(wsResponse.getStatusCode()) == 201);
        assertTrue("Uploaded file name is wrong " + wsResponse.getResponse(), wsResponse.getStatusText().equals("Created"));


        testUrl = wsm.getMeta(WsUtil.ensureUriFolderPath(testRelPathFolder) + testFile.getName(), WspaceMeta.Includes.NONE).getUrl();
        try {
            byte[] bytesUploaded = jackrabbitGet(testUrl);
            assertTrue("Size uploaded " + sizeTest + " not the same found " + bytesUploaded.length, bytesUploaded.length == sizeTest);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    byte[] jackrabbitGet(String uri) throws Exception {
        org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
        org.apache.commons.httpclient.Credentials creds = new org.apache.commons.httpclient.UsernamePasswordCredentials(cred.getWsId(), cred.getPassword());
        client.getState().setCredentials(AuthScope.ANY, creds);
        MyGetMethod method = new MyGetMethod(uri);
        client.executeMethod(method);
        if (method.isSuccess(method.getStatusCode())) {
            byte[] resp = method.getResponseBody();
            LOG.info("Got response: " + resp.length + " bytes");
            return resp;
        }
        return new byte[0];
    }

    @Ignore
    @Test
    public void testGetAllLeaves() throws WsException {

        WsResponse wsResponse = wsm.putFile(testRelPathFolder,
                testFile,
                ContentType.DEFAULT_BINARY.getMimeType());


        WspaceMeta meta = wsm.getMeta("/", WspaceMeta.Includes.CHILDREN_PROPS);


        printPath(meta, false);


    }

    private void printPath(WspaceMeta parent, boolean avoidFolders) {

        if(avoidFolders && parent.getContentType()==null || (parent.getContentType()!=null && !parent.getContentType().contains("directory"))){
            System.out.println("File: "+parent.getUrl());
        }else if(!avoidFolders){
            System.out.println("Folder: "+parent.getUrl());
        }
        WspaceMeta pMeta = wsm.getMeta(parent.getRelPath(), WspaceMeta.Includes.CHILDREN_PROPS);

        if(pMeta.hasChildNodes()) {
            List<WspaceMeta> childs = pMeta.getChildNodes();
            Iterator<WspaceMeta> iterator =
                    childs.iterator();
            while (iterator.hasNext()) {
                WspaceMeta child = iterator.next();
                printPath(child, avoidFolders);
            }
        }
    }
    @Ignore
    @Test
    public void testGet() throws WsException {

        WsResponse wsResponse = wsm.putFile(testRelPathFolder,
                testFile,
                ContentType.DEFAULT_BINARY.getMimeType());

        assertTrue("Upload went wrong, status code <200 " + wsResponse.getStatusCode(), Integer.parseInt(wsResponse.getStatusCode()) == 201);
        assertTrue("Uploaded file name is wrong " + wsResponse.getResponse(), wsResponse.getStatusText().equals("Created"));


        String rel = wsm.getMeta(WsUtil.ensureUriFolderPath(testRelPathFolder) + testFile.getName(), WspaceMeta.Includes.NONE).getRelPath();
        try {
            File fDownloaded = managerGet(rel);
            fDownloaded.deleteOnExit();
            assertTrue("Size uploaded " + sizeTest + " not the same found " + fDownloaded.length(), fDownloaded.length() == sizeTest);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private File managerGet(String relFolder) throws Exception {
        File f = File.createTempFile("test-", "xyz");
        WsResponse wsResponse = wsm.getFile(relFolder, f);
        return f;
    }
    @Ignore
    @Test
    public void specialCharsUriException() throws WsException {

        testRelPathFolder = "tmp-" + UUID.randomUUID().toString() + "/tmp1?/t2#$%^&*9";

        WsResponse wsResponse = wsm.putFile(testRelPathFolder,
                testFile,
                ContentType.DEFAULT_BINARY.getMimeType());
        if (wsResponse.getStatusCode().endsWith("301")) {
            assertTrue(wsResponse.getStatusCode().equals("-1"));
            assertTrue(wsResponse.getResponse().startsWith("java.net.URISyntaxException"));
            assertTrue(wsResponse.getResponse().indexOf(testRelPathFolder) > 0);
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
