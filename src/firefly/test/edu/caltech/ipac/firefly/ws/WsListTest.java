package edu.caltech.ipac.firefly.ws;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.*;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Created by ejoliet on 6/26/17.
 */
public class WsListTest extends ConfigTest {

    private static WorkspaceManager m;
    private static File[] testFile = new File[2];
    private static String[] relFolder = new String[2];
    private static ArrayList testFolders;
    private static ArrayList testFiles;
    private static WorkspaceHandler handler;


    @Before
    public void init() throws WsException, ClassNotFoundException {
        assumeTrue(getWsCredentials()!=null);
        testFolders = new ArrayList<>();
        testFiles = new ArrayList<>();
        relFolder[0] = createFolder("/tmp1/");
        relFolder[1] = createFolder("/tmp2/");
        handler = WorkspaceFactory.getWorkspaceHandler();
        m = handler.withCredentials(getWsCredentials());

        assumeTrue(!m.getMeta("/", WspaceMeta.Includes.CHILDREN).hasChildNodes());
        if(m.getMeta("/", WspaceMeta.Includes.CHILDREN).hasChildNodes()){
            throw new RuntimeException("Workspace user location is not empty to complete the tests");
        }
        testFile[0] = pickFile(0);
        testFile[1] = pickFile(1);

        m.putFile(relFolder[0], testFile[0], null);

        m.putFile(relFolder[1], testFile[0], null);
        m.putFile(relFolder[1], testFile[1], null);
    }

    private String createFolder(String s) {
        testFolders.add(s);
        return s;
    }
    @Test
    public void testGetListDepth() throws IOException {

        //Get childs of user home
        WsResponse r = m.getList("/", 1);

        if (r.doContinue()) { //Success response
            List<WspaceMeta> childNodes = r.getWspaceMeta();
            printChildren(childNodes);

            assertTrue(childNodes.size() == relFolder.length);
            childNodes = r.getWspaceMeta();
            if (childNodes != null) {
                Iterator<WspaceMeta> it = childNodes.iterator();
                int childs = 0;
                while (it.hasNext()) {
                    WspaceMeta next = it.next();
                    assertTrue(testFolders.contains(next.getRelPath()));
                    childs++;
                }

                assertTrue(childs == childNodes.size());
            }
        }

        //Gets prop from file equivalent to get list of resource with depth = 0:
        String testPath = WsUtil.ensureUriFolderPath(WspaceMeta.ensureRelPath(relFolder[0])) + testFile[0].getName();
        WsResponse response = m.getList(testPath, 0);
        if (response.doContinue()) {
            List responseWspaceMeta = response.getWspaceMeta();
            assertTrue(responseWspaceMeta.size() == 1);
            WspaceMeta meta1 = (WspaceMeta) responseWspaceMeta.get(0);
            assertTrue(meta1.getRelPath().equals(testPath));
        }

        //Get children of folder: should be one file
        r = m.getList(WsUtil.ensureUriFolderPath(WspaceMeta.ensureRelPath(relFolder[0])), 1);
        if (r.doContinue()) { //Success response
            List<WspaceMeta> childNodes = r.getWspaceMeta();
            assertTrue(childNodes.size() == 1);
            WspaceMeta wspaceMeta = childNodes.get(0);
            assertTrue(wspaceMeta.getRelPath().equals(testPath));
        }

        //Get children of folder: should be 2 files
        r = m.getList(WsUtil.ensureUriFolderPath(WspaceMeta.ensureRelPath(relFolder[1])), 1);
        if (r.doContinue()) { //Success response
            List<WspaceMeta> childNodes = r.getWspaceMeta();
            assertTrue(childNodes.size() == 2);
            int i = 0;
            for (WspaceMeta m : childNodes) {
                WspaceMeta wspaceMeta = m;
                File f = testFile[i++];
                assertTrue(wspaceMeta.getSize() > 0);
                assertTrue(testFiles.contains(f.getName()));
            }
            assertTrue(i == childNodes.size());
        }

    }

    private void printChildren(List<WspaceMeta> childNodes) {
        if (childNodes != null) {
            Iterator<WspaceMeta> it = childNodes.iterator();
            if (childNodes.size() > 0) {
                while (it.hasNext()) {
                    WspaceMeta next = it.next();
                    LOG.info(next.getRelPath());
                    printChildren(next.getChildNodes());
                }
            }
        }
    }

    @After
    public void tearDown() throws IOException {
        if(m!=null) {
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
}
