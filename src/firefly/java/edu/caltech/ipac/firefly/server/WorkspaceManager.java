/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.Namespace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Date: 6/12/14
 *
 * @author loi
 * @version $Id: $
 */
public class WorkspaceManager {
    public static final String SEARCH_DIR = WspaceMeta.SEARCH_DIR;
    public static final String STAGING_DIR = WspaceMeta.STAGING_DIR;
    public static final String WS_ROOT_DIR = AppProperties.getProperty("workspace.root.dir", "/work");
    public static final String WS_HOST_URL = AppProperties.getProperty("workspace.host.url", "https://irsa.ipac.caltech.edu");

    private static final Namespace IRSA_NS = Namespace.getNamespace("irsa", "http://irsa.ipac.caltech.edu/namespace/");
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    public enum Partition { PUBSPACE, SSOSPACE;
                    public String toString() {return name().toLowerCase();}
                }

    private Partition partition;
    private String userHome;
    private Map<String,String> cookies;

    public WorkspaceManager(String pubspaceId) {
        this(Partition.PUBSPACE, pubspaceId, null);
    }

    public WorkspaceManager(String ssospaceId, Map<String, String> cookies) {
        this(Partition.SSOSPACE, ssospaceId, cookies);

    }

    public WorkspaceManager(Partition partition, String wsId, Map<String, String> cookies) {
        this.partition = partition;
        this.userHome = WspaceMeta.ensureWsHomePath(wsId);
        this.cookies = cookies;
        davMakeDir("");
    }

    /**
     * returns the local filesystem path of the given relpath.
     * @param relPath  relative to the user's workspace and must starts with '/'
     * @return
     */
    public String getLocalFsPath(String relPath) {
        return WS_ROOT_DIR  + getAbsPath(relPath);
    }

    /**
     * returns the webdav's absolute path.  for IRSA, it's /partition/user_ws/relpath.
     * @param relPath  relative to the user's workspace and must starts with '/'
     * @return
     */
    public String getAbsPath(String relPath) {
        String s = WspaceMeta.ensureRelPath(relPath);
        return getWsHome() + s;
    }

    /**
     * returns the relative path given the file.  for IRSA, it's /partition/user_ws/relpath.
     * @param file  relative to the user's workspace and must starts with '/'
     * @return
     */
    public String getRelPath(File file) {
        String s = file.getAbsolutePath().replaceFirst(WS_ROOT_DIR, "");
        s = s.replaceFirst(getWsHome(), "");
        return s;
    }

    /**
     * return the url of this resource.
     * @param relPath
     * @return
     */
    public String getResourceUrl(String relPath) {
        return WS_HOST_URL + getAbsPath(relPath);
    }

    public String getWsHome() {
        return "/" + partition + userHome;
    }

    public File createFile(String parent, String fname, String fext) {
        davMakeDir(parent);
        return new File(new File(getLocalFsPath(parent)), fname + "-" + System.currentTimeMillis()%1000000 + fext);
    }

    /**
     * convenience method to create meta for a local file in the workspace
     * @param file
     * @param propName
     * @param value
     * @return
     */
    public WspaceMeta newMeta(File file, String propName, String value) {
        WspaceMeta meta = new WspaceMeta(getWsHome(), getRelPath(file));
        meta.setProperty(propName, value);
        return meta;
    }

//====================================================================
//  WEBDAV functions
//====================================================================

    /**
     * create a directory given by the relPath parameter.
     * @param relPath relative to the user's workspace
     * @return  the absolute path to the directory on the filesystem.
     */
    public File davMakeDir(String relPath) {
        try {
            String[] parts = relPath.split("/");
            String cdir = "/";
            for (String s : parts) {
                cdir += StringUtils.isEmpty(s) ? "" :  s + "/";
                WspaceMeta m = getMeta(cdir, WspaceMeta.Includes.NONE);
                if (m == null) {
                    DavMethod mkcol = new MkColMethod(getResourceUrl(cdir));
                    if ( !executeMethod(mkcol)) {
                        // handle error
                        System.out.println("Unable to create directory:" + relPath + " -- " + mkcol.getStatusText());
                        return null;
                    }
                }
            }
            return new File(getLocalFsPath(relPath));
        } catch (Exception e) {
            LOG.error(e, "Error while makeCollection:" + relPath);
        }
        return null;
    }

    public boolean davPut(File upload, String toPath) {
        try {
            PutMethod put = new PutMethod(getResourceUrl(toPath));
            RequestEntity requestEntity = new InputStreamRequestEntity(new BufferedInputStream(
                                new FileInputStream(upload), HttpServices.BUFFER_SIZE), upload.length());
            put.setRequestEntity(requestEntity);
            /** is to allow a client that is sending a request message with a request body
             *  to determine if the origin server is willing to accept the request
             * (based on the request headers) before the client sends the request body.
             * this require server supporting HTTP/1.1 protocol.
             */
            put.getParams().setBooleanParameter(
                    HttpMethodParams.USE_EXPECT_CONTINUE, true);

            if (!executeMethod(put)) {
                // handle error
                System.out.println("Unable to upload file:" + toPath + " -- " + put.getStatusText());
                return false;
            }

            return true;
        } catch (Exception e) {
            LOG.error(e, "Error while uploading file:" + upload.getPath());
        }
        return false;
    }


//====================================================================
//  for firefly use
//====================================================================

    public WspaceMeta getMeta(String relPath) {
        return getMeta(relPath, WspaceMeta.Includes.ALL);
    }


    public WspaceMeta getMeta(String relPath, WspaceMeta.Includes includes) {

        // this can be optimized by retrieving only the props we care for.
        DavMethod pFind = null;
        try {
            if (includes.inclProps) {
                pFind = new PropFindMethod(getResourceUrl(relPath), DavConstants.PROPFIND_ALL_PROP, includes.depth);
            } else {
                pFind = new PropFindMethod(getResourceUrl(relPath), DavConstants.PROPFIND_BY_PROPERTY, includes.depth);
            }

            if (!executeMethod(pFind, false)) {
                // handle error
                if (pFind.getStatusCode() != 404) {
                    System.out.println("Unable to find property:" + relPath + " -- " + pFind.getStatusText());
                }
                return null;
            }

            MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] resps = multiStatus.getResponses();
            if (resps == null || resps.length == 0) {
                return null;
            }
            WspaceMeta root = new WspaceMeta(getWsHome(), relPath);

            for (MultiStatusResponse res : resps) {
                if (res.getHref().equals(root.getAbsPath())) {
                    convertToWspaceMeta(root, res);
                } else {
                    addToRoot(root, res);
                }
            }
            return root;
        } catch (Exception e) {
            LOG.error(e, "Error while getting meta for:" + relPath);
        } finally {
            if (pFind != null) {
                pFind.releaseConnection();
            }
        }
        return null;
    }

    /**
     * convenience method to set one property on a resource.
     * if the property value is null, that property will be removed.
     * otherwise, the property will be either added or updated.
     * @param relPath
     * @param propName
     * @param value
     * @return
     */
    public boolean setMeta(String relPath, String propName, String value) {
        WspaceMeta meta = new WspaceMeta(null, relPath);
        meta.setProperty(propName, value);
        return setMeta(meta);
    }

    /**
     * set meta information on this dav's resource.
     * if the property value is null, that property will be removed.
     * otherwise, the property will be either added or updated.
     * @param metas
     * @return
     */
    public boolean setMeta(WspaceMeta ... metas) {
        if (metas == null) return false;
        for(WspaceMeta meta : metas) {

            Map<String, String> props = meta.getProperties();
            if (props != null && props.size() > 0) {
                DavPropertySet newProps=new DavPropertySet();
                DavPropertyNameSet removeProps=new DavPropertyNameSet();

                for (String key : props.keySet()) {
                    String v = props.get(key);
                    if (v == null) {
                        removeProps.add(DavPropertyName.create(key, IRSA_NS));
                    } else {
                        DavProperty p = new DefaultDavProperty(key, props.get(key), IRSA_NS);
                        newProps.add(p);
                    }
                }
                try {
                    PropPatchMethod proPatch=new PropPatchMethod(getResourceUrl(meta.getRelPath()), newProps, removeProps);
                    if ( !executeMethod(proPatch)) {
                        // handle error
                        System.out.println("Unable to update property:" + newProps.toString() +  " -- " + proPatch.getStatusText());
                        return false;
                    }
                    return true;
                } catch (IOException e) {
                    LOG.error(e, "Error while setting property: " + meta);
                    e.printStackTrace();
                }

            }

        }
        return false;
    }

//====================================================================
//
//====================================================================

    private WspaceMeta convertToWspaceMeta(WspaceMeta meta, MultiStatusResponse res) {
        if (meta == null) {
            meta = new WspaceMeta(getWsHome(), res.getHref().replaceFirst(getWsHome(), ""));
        }
        DavPropertySet props = res.getProperties(200);
        if (props != null) {
            for (DavProperty p : props) {
                String name = (p == null || p.getName() == null) ? null : p.getName().getName();
                if (name != null) {
                    String v = String.valueOf(p.getValue());
                    if (name.equals(DavConstants.PROPERTY_GETLASTMODIFIED)) {
                            meta.setLastModified(v);
                    } else if (name.equals(DavConstants.PROPERTY_GETCONTENTLENGTH)) {
                        try {
                            meta.setSize(Long.parseLong(v));
                        } catch (Exception e) {}
                    } else if (name.equals(DavConstants.PROPERTY_GETCONTENTTYPE)) {
                        meta.setContentType(v);
                    } else if (p.getName().getNamespace().equals(IRSA_NS)) {
                        meta.setProperty(name, String.valueOf(p.getValue()));
                    }
                }
            }
        }
        return meta;
    }

    private void addToRoot(WspaceMeta root, MultiStatusResponse res) {
        WspaceMeta meta = convertToWspaceMeta(null, res);
        WspaceMeta p = root.find(meta.getParentPath());
        if (p != null) {
            p.addChild(meta);
        } else {
            root.addChild(meta);
        }
    }
    private boolean executeMethod(DavMethod method) {
        return executeMethod(method, true);
    }

    private boolean executeMethod(DavMethod method, boolean releaseConnection) {
        try {
            return partition.equals(Partition.PUBSPACE) ? HttpServices.executeMethod(method) :
                    HttpServices.executeMethod(method, "xxx", "xx", cookies);
        } finally {
            if (releaseConnection && method != null) {
                method.releaseConnection();
            }
        }



    }

    public static void main(String[] args) {
        WorkspaceManager man = ServerContext.getRequestOwner().getWsManager();
        simpleTest(man);

//        AppProperties.setProperty("sso.server.url", "http://irsa.ipac.caltech.edu/account/");
//        String session = JOSSOAdapter.createSession("", "");
//        Map<String, String> cookies = new HashMap<String, String>();
//        cookies.put(WebAuthModule.AUTH_KEY, session);
//        WorkspaceManager man = new WorkspaceManager("loi@ipac.caltech.edu", cookies);
//        simpleTest(man);
    }
     private static void simpleTest(WorkspaceManager man) {
         File f = man.davMakeDir("123/");
         System.out.println("new directory: " + String.valueOf(f));

         WspaceMeta m = new WspaceMeta(null, "123/");
         m.setProperty("test1", "an awesome idea");
         m.setProperty("test", null);
         man.setMeta(m);

         String ufilePath = "/123/uploaded" + System.currentTimeMillis()%1000 + ".tiff";
         man.davPut(new File("/Users/loi/dia.tiff"), ufilePath);
         man.setMeta(ufilePath, "added_by", man.userHome);

         WspaceMeta meta = man.getMeta("/", WspaceMeta.Includes.ALL_PROPS);
         System.out.println(meta.getNodesAsString());
     }
}
