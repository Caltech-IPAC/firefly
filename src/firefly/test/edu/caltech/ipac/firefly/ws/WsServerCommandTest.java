package edu.caltech.ipac.firefly.ws;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.*;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.jackrabbit.webdav.client.methods.CheckinMethod;
import org.json.simple.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static edu.caltech.ipac.firefly.server.network.HttpServices.executeMethod;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class WsServerCommandTest extends ConfigTest {

    private static WorkspaceManager m;
    private static File[] testFile = new File[2];
    private static String[] relFolder = new String[2];
    private static ArrayList testFolders;
    private static ArrayList testFiles;
    private static WorkspaceHandler handler;


    @Before
    public void init() throws WsException, ClassNotFoundException {
        assumeTrue(getWsCredentials() != null);
        testFolders = new ArrayList<>();
        testFiles = new ArrayList<>();
        relFolder[0] = createFolder("/tmp-with-1file/");
        relFolder[1] = createFolder("/tmp-with-2files/");
        handler = WorkspaceFactory.getWorkspaceHandler();
        m = handler.withCredentials(getWsCredentials());

        testFile[0] = pickFile(0);
        testFile[1] = pickFile(1);

        m.putFile(relFolder[0], testFile[0], null);

        m.putFile(relFolder[1], testFile[0], null);
        WsResponse response = m.putFile(relFolder[1], testFile[1], "binary");

        String path = WsUtil.ensureUriFolderPath(relFolder[1]) + testFile[1].getName();

        //Example to set prop
        WspaceMeta meta = new WspaceMeta(null, path);
        meta.setProperty("tooltip", testFile[1].getName() + " -url:" + response.getResponse());
        assertTrue(m.setMeta(meta));

        // Overwrite the server utils to use own credentials for testing:
        WsServerCommands.utils = new WsServerUtils() {
            @Override
            public WsCredentials getCredentials(){
                return ConfigTest.getWsCredentials();
            }
        };
    }

    private String createFolder(String s) {
        testFolders.add(s);
        return s;
    }

    /**
     * Gets entire list of content
     *
     * @throws Exception
     */
    @Test
    public void wsServerCommandGetEntireList() throws Exception {

        Map<String, String[]> map = new HashMap<>();

        SrvParam sp = new SrvParam(map);

        //For testing purposes, overwrite how to get credential and return the one from the test ConfigTest.getCredentials
        String s = new WsServerCommands.WsList().doCommand(sp);

        LOG.info("JSON1:\r\n" + s);


        // Manually calling pieces instead:
        WsResponse r = m.getList("/", -1);
        JSONArray arr = new JSONArray();

        if (r.doContinue()) { //Success response
            List<WspaceMeta> childNodes = r.getWspaceMeta();
            addToJson(arr, childNodes);
            LOG.info("JSON2:\r\n" + arr.toJSONString());
        }

        //Should be identical

        assertTrue(arr.toJSONString().equals(s));
    }

    /**
     * Gets list on a particular folder
     *
     * @throws Exception
     */
    @Test
    public void wsServerCommandGetPartialList() throws Exception {

        Map<String, String[]> map = new HashMap<>();
        map.put(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH.getKey(), new String[]{relFolder[1]});

        SrvParam sp = new SrvParam(map);
        WsServerParams params = WsServerCommands.convertToWsServerParams(sp);
        WsServerUtils utils = new WsServerUtils() {
            @Override
            public WsCredentials getCredentials() {
                return ConfigTest.getWsCredentials();
            }
        };

        params.set(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH, "/");
        // should give only 2 folders below "/"
        List<WspaceMeta> childNodes = utils.getList(params, 1);
        JSONArray jsonObject = WsServerUtils.toJson(childNodes);

        assertTrue(jsonObject.size() == relFolder.length); // we've added 2 folders

        params.set(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH, relFolder[1]);

        childNodes = utils.getList(params, -1);
        jsonObject = WsServerUtils.toJson(childNodes);

        assertTrue(jsonObject.size() == testFile.length); // we've added 2 files to relFolder[1]

        childNodes = utils.getList(params, 0);
        jsonObject = WsServerUtils.toJson(childNodes);

        assertTrue(jsonObject.size() == 1); // only the node


        childNodes = utils.getList(params, 1);
        jsonObject = WsServerUtils.toJson(childNodes);

        assertTrue(jsonObject.size() == 2); // only one level below: should return 2 files node

    }

    @Test
    public void wsServerCreateParentTest() throws Exception {

        String relPath = "/a/b/c";
        relPath = WsUtil.ensureUriFolderPath(relPath);
        Map<String, String[]> map = new HashMap<>();
        map.put(WsServerParams.WS_SERVER_PARAMS.NEWPATH.getKey(), new String[]{relPath});
        SrvParam sp = new SrvParam(map);

        //For testing purposes, overwrite how to get credential and return the one from the test ConfigTest.getCredentials
        String s = new WsServerCommands.WsCreateParent().doCommand(sp);


        //Check that the path exist by checking the metatdata
        map.put(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH.getKey(), new String[]{relPath});
        sp = new SrvParam(map);

        List<WspaceMeta> childNodes = WsServerCommands.utils.getMeta(WsServerCommands.convertToWsServerParams(sp), WspaceMeta.Includes.ALL_PROPS);
        assertTrue(childNodes.get(0).getRelPath().equals(relPath));

    }

    @Test
    public void wsServerMoveTest() throws Exception {

        String newPath = relFolder[1]+"/a/b/c/"+testFile[0].getName()+"bis";
        String relPath = relFolder[0]+"/"+testFile[0].getName();
        Map<String, String[]> map = new HashMap<>();
        map.put(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH.getKey(), new String[]{relPath});
        map.put(WsServerParams.WS_SERVER_PARAMS.NEWPATH.getKey(), new String[]{newPath});
        map.put(WsServerParams.WS_SERVER_PARAMS.SHOULD_OVERWRITE.getKey(), new String[]{"true"});
        SrvParam sp = new SrvParam(map);

        //For testing purposes, overwrite how to get credential and return the one from the test ConfigTest.getCredentials
        String s = new WsServerCommands.WsMoveFile().doCommand(sp);

//Check that on the new path (the current path) the file exists
        map.put(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH.getKey(), new String[]{newPath});
        sp = new SrvParam(map);
        List<WspaceMeta> childNodes = WsServerCommands.utils.getMeta(WsServerCommands.convertToWsServerParams(sp), WspaceMeta.Includes.CHILDREN_PROPS);
        assertTrue(childNodes.get(0).getRelPath().equals(newPath));
        assertTrue(s.length()>0);

        LOG.info("JSON1:\r\n" + s);

    }

    private void addToJson(JSONArray arr, List<WspaceMeta> childNodes) {
        if (childNodes != null) {
            Iterator<WspaceMeta> it = childNodes.iterator();
            if (childNodes.size() > 0) {
                while (it.hasNext()) {
                    WspaceMeta next = it.next();
                    serializeWsMeta(next).to(arr);
                    addToJson(arr, next.getChildNodes());
                }
            }
        }
    }

    private WsServerUtils.WsJson serializeWsMeta(WspaceMeta next) {
        WsServerUtils.WsJson map = new WsServerUtils.WsJson(next);
        return map;
    }


    @After
    public void tearDown() throws IOException {
        if (m != null) {
            // Gets children folder meta under user home ws.
            // Equivalent to WsResponse wsResponse = m.getSuggestedList("/", 1);
            //
            WspaceMeta mMeta = m.getMeta("/", WspaceMeta.Includes.CHILDREN);
            if (mMeta != null) {
                List<WspaceMeta> metas = mMeta.getChildNodes();//wsResponse.getWspaceMeta();

                for (WspaceMeta meta : metas) {
                    WsResponse response = m.delete(meta.getRelPath());
                    if (response.getStatusCode().equals("304")) { //Parent is home, try to delete file then
                        m.delete(meta.getRelPath());
                    }
                }
            }
        }
    }

    private static File pickFile(int idx) throws ClassNotFoundException {
        File testPath = new File(FileLoader.getDataPath(WsListTest.class));
        File file = testPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.endsWith(".fits"); // Can't use fits in pubspace because > 1Mb irsa policy storage
            }
        })[idx];
        testFiles.add(file.getName());
        return file;
    }

    public String checkin(String uri) throws Exception {

            LOG.info("checkin '" + uri + "'");

        CheckinMethod httpMethod = new CheckinMethod(uri);
        executeMethod(httpMethod);

        String objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    private String processResponse(HttpMethod httpMethod, boolean getResponseAsString) {

        String statusCode = "-1";
        if (httpMethod.getStatusCode() > 0) {
            statusCode = String.valueOf(httpMethod.getStatusCode());
        }
        String statusText = httpMethod.getStatusText();

        String responseString = "";
        if (getResponseAsString) {
            try {
                responseString = httpMethod.getResponseBodyAsString();
                if (responseString == null) {
                    responseString = "";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

            LOG.info("status CODE: " + statusCode + ", status TEXT: " + statusText + "\n response string: " + responseString);

        return "status CODE: " + statusCode + ", status TEXT: " + statusText + "\n response string: " + responseString;
    }
}
