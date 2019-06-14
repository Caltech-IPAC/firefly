/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.firefly.server.ws.WsResponse;
import edu.caltech.ipac.util.StringUtils;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;


import java.io.*;
import java.util.Map;

/**
 * WebDAV implementation for IRSA
 * Everything relative to ws home (as URI remote)
 * Date: 6/12/14
 *
 * @author loi
 * @version $Id: $
 */
public class WebDAVWorkspaceManager extends WebDAVWorkspaceManagerBase {

    /**
     * TODO is this used currently or should/will it be used in the future?
     */
    private static final String IRSA_NS_PREFIX = "irsa";
    private static final String IRSA_NS_URI = "https://irsa.ipac.caltech.edu/namespace/";
    //private static final Namespace IRSA_NS = Namespace.getNamespace("irsa", "https://irsa.ipac.caltech.edu/namespace/");
    //TODO We might need to change those properties if we need to deal with 2 or more workspaces at the same time
    // //WS_ROOT_DIR is used only for testing and is confusing things, getWsHome() should be used instead
    //private static String WS_ROOT_DIR = AppProperties.getProperty("workspace.root.dir", "/work");

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private WsCredentials creds;

    @Override
    public String getProp(PROPS prop) {
        // TODO: this method is used only in test - do we need it?
        PROPS propFound = null;
        for (PROPS p : PROPS.values()) {
            if (p.name().equalsIgnoreCase(prop.name())) {
                propFound = p;
                break;
            }
        }
        String propVal = prop + " doesn't exist";
        if (propFound == null) {
            return propVal;
        }
        if (propFound.equals(PROPS.ROOT_URL)) {
            propVal = WS_HOST_URL;
        } else if (propFound.equals(PROPS.ROOT_DIR)) {
            propVal = getWsHome();
        } else if (propFound.equals(PROPS.AUTH)) {
            propVal = this.partition.name();
        } else if (propFound.equals(PROPS.PROTOCOL)) {
            propVal = PROTOCOL.WEBDAV.name();
        }

        return propVal;
    }

    public enum Partition {
        PUBSPACE, SSOSPACE;

        public String toString() {
            return name().toLowerCase();
        }
    }

    private Partition partition;
    private String userHome;

    /**
     * Used by {@link edu.caltech.ipac.firefly.server.ws.WebDAVWorkspaceHandler#withCredentials(WsCredentials)}}
     * @param cred {@link WsCredentials}
     */
    public WebDAVWorkspaceManager(WsCredentials cred) {
        this(ServerContext.getRequestOwner().isAuthUser() ? Partition.SSOSPACE : Partition.PUBSPACE, cred, true);
    }

    public WebDAVWorkspaceManager(String pubspaceId) {
        this(Partition.PUBSPACE, new WsCredentials(pubspaceId), true);
    }

    public WebDAVWorkspaceManager(String ssospaceId, Map<String, String> cookies) {
        this(Partition.SSOSPACE, new WsCredentials(ssospaceId, cookies), false);

    }

    public WebDAVWorkspaceManager(String ssospaceId, String pass) {
        this(Partition.SSOSPACE, new WsCredentials(ssospaceId, pass), false);
    }

    public WebDAVWorkspaceManager(Partition partition, WsCredentials cred, boolean initialize) {
        this(partition, cred.getWsId(), initialize);
        this.creds = cred;
    }

    public WebDAVWorkspaceManager(Partition partition, String wsId, boolean initialize) {

        Map<String, String> cookies = HttpServiceInput.createWithCredential(WS_HOST_URL).getCookies();          // should look at this again.

        this.creds = new WsCredentials(wsId, cookies);
        this.partition = partition;
        this.userHome = WspaceMeta.ensureWsHomePath(wsId);
        if (initialize) {
            davMakeDir("");
        }
    }


    //====================================================================
//  overrides and abstract methods implementations
//====================================================================
    @Override
    public WsCredentials getCredentials() {
        return this.creds;
    }

    @Override
    public String getWsHome() {
        return "/" + partition + userHome;
    }

    @Override
    protected HttpServices.Status doExecuteMethod(DavMethod method) throws IOException {
        if (partition.equals(Partition.PUBSPACE)) {
            return HttpServices.executeMethod(method);
        } else {
            HttpServiceInput input = new HttpServiceInput().setUserId(creds.getWsId()).setPasswd(creds.getPassword());
            if (creds.getCookies() != null) {
                creds.getCookies().forEach(input::setCookie);
            }
            return HttpServices.executeMethod(method, input);
        }
    }

    @Override
    protected String getNamespacePrefix() {
        return IRSA_NS_PREFIX;
    }

    @Override
    protected String getNamespaceUri() {
        return IRSA_NS_URI;
    }


//====================================================================
//  testing
//====================================================================

    public static void main(String[] args) {

//        WebDAVWorkspaceManager man = (WebDAVWorkspaceManager) ServerContext.getRequestOwner().getWsManager();

        WebDAVWorkspaceManager man = new WebDAVWorkspaceManager("ejoliet-tmp2");

        simpleTest(man);

//        AppProperties.setProperty("sso.server.url", "http://irsa.ipac.caltech.edu/account/");
//        String session = JOSSOAdapter.createSession("", "");
//        Map<String, String> cookies = new HashMap<String, String>();
//        cookies.put(WebAuthModule.AUTH_KEY, session);
//        WorkspaceManager man = new WorkspaceManager("<someuser>@ipac.caltech.edu", cookies);
//        simpleTest(man);
    }

    /**
     * //TODO: can I use super.createParent instead of this one: they seem to be virtually identical
     * create a directory given by the relPath parameter.
     *
     * @param relPath relative to the user's workspace
     */
    private void davMakeDir(String relPath) {
        try {
            String[] parts = relPath.split("/");
            String cdir = "/";
            for (String s : parts) {
                cdir += StringUtils.isEmpty(s) ? "" : s + "/";
                WspaceMeta m = getMeta(cdir, WspaceMeta.Includes.NONE);
                if (m == null) {
                    DavMethod mkcol = new MkColMethod(getResourceUrl(cdir));
                    if (!executeMethod(mkcol)) {
                        // handle error
                        LOG.error("Unable to create directory:" + relPath + " -- " + mkcol.getStatusText());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e, "Error while makeCollection:" + relPath);
        }
    }

    private static void simpleTest(WebDAVWorkspaceManager man) {
        String relPath = "124/";
        man.davMakeDir(relPath);
        System.out.println("new directory: " + String.valueOf(relPath));

        WspaceMeta m = new WspaceMeta(null, relPath);
        m.setProperty("test1", "an awesome idea");
        m.setProperty("test", null);
        man.setMeta(m);

        String ufilePath = relPath + "gaia-binary.vot";
        WsResponse wsResponse = man.davPut(new File("/Users/ejoliet/devspace/branch/dev/firefly_test_data/edu/caltech/ipac/firefly/ws/gaia-binary.vot"),
                                           ufilePath, false, null);

        System.out.println(wsResponse);

        WspaceMeta meta = new WspaceMeta(null, ufilePath);
        meta.setProperty("added_by", man.userHome);
        man.setMeta(meta);
        man.setMeta(ufilePath, "added_by", man.userHome);

        try {
            WspaceMeta meta2 = man.getMeta("/", WspaceMeta.Includes.ALL_PROPS);
            System.out.println(meta2.getNodesAsString());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
