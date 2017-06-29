/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.ws.Workspaces;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.util.AppProperties;

import java.io.File;

/**
 * Workspace manager interface
 */
public interface WorkspaceManager extends Workspaces {

    String SEARCH_DIR = WspaceMeta.SEARCH_DIR;

    //TODO We might need to change those properties if we need to deal with 2 or more workspaces at the same time
    String WS_ROOT_DIR = AppProperties.getProperty("workspace.root.dir", "/work");
    String WS_HOST_URL = AppProperties.getProperty("workspace.host.url", "https://irsa.ipac.caltech.edu");


    WsCredentials getCredentials();

    String getWsHome();

    public enum PROTOCOL {LOCAL, WEBDAV, VOSPACE}

    enum PROPS {PROTOCOL, AUTH, ROOT_URL, ROOT_DIR}

    public PROTOCOL getProtocol();

    public String getProp(PROPS ps);

    /**
     * TODO kept this because somehow we used it elsewhere
     * TODO but i think we should be using http and not local access
     * Create file in ws relative path
     *
     * @param wspaceRelDir
     * @param filePrefix
     * @param fileExt
     * @return File created
     * @deprecated should be using {@link Workspaces#putFile(String, File, String)}
     */
    File createWsLocalFile(String wspaceRelDir, String filePrefix, String fileExt);

    /**
     * build ws meta from local file in ws fs
     *
     * @param file     file located in ws fs
     * @param propName
     * @param value
     * @return WspaceMeta
     * ¬
     */
    WspaceMeta newLocalWsMeta(File file, String propName, String value);
}