package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
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
    public static final String SEARCH_DIR = "searches";
    public static final String STAGING_DIR = "staging";
    public static final String WS_ROOT_DIR = AppProperties.getProperty("workspace.root.dir", "/hydra/ws/");
    public static final String WS_HOST_URL = AppProperties.getProperty("workspace.host.url", "http://***REMOVED***.ipac.caltech.edu:9200");

    private static final Namespace IRSA_NS = Namespace.getNamespace("irsa", "http://irsa.ipac.caltech.edu/namespace/");
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    public enum Partition { PUBSPACE, SSOSPACE;
                    public String toString() {return name().toLowerCase();}
                }

    private Partition partition;
    private String userHome;
    private Map<String,String> cookies;

    public WorkspaceManager(Partition partition, String userHome) {
        this.partition = partition;
        this.userHome = WspaceMeta.ensureWsHomePath(userHome);
        if (partition == Partition.SSOSPACE) {
            cookies = ServerContext.getRequestOwner().getIdentityCookies();
        }
        File f = new File(getLocalFsPath(""));
        if (!f.exists()) {
            davMakeDir("");
        }
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
        try {
            return File.createTempFile(fname, fext, new File(getLocalFsPath(parent)));
        } catch (IOException e) {
            LOG.error(e, "Fail to create workspace file: " + e.getMessage());
        }
        return null;
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
            DavMethod mkcol = new MkColMethod(getResourceUrl(relPath));
            if ( !HttpServices.executeMethod(mkcol, cookies)) {
                // handle error
                return null;
            }

            return new File(getLocalFsPath(relPath));
        } catch (Exception e) {
            LOG.error(e, "Error while makeCollection:" + relPath);
        }
        return null;
    }

    public boolean davPut(File upload, String toPath) {
        try {
            HttpServices.init();
            PutMethod put = new PutMethod(getResourceUrl(toPath));
            RequestEntity requestEntity = new InputStreamRequestEntity(new BufferedInputStream(
                                new FileInputStream(upload), HttpServices.BUFFER_SIZE), upload.length());
            put.setRequestEntity(requestEntity);
            if (!HttpServices.executeMethod(put, cookies)) {
                // handle error
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
        try {

            // this can be optimized by retrieving only the props we care for.
            DavMethod pFind = null;
            if (includes.inclProps) {
                pFind = new PropFindMethod(getResourceUrl(relPath), DavConstants.PROPFIND_ALL_PROP, includes.depth);
            } else {
                pFind = new PropFindMethod(getResourceUrl(relPath), DavConstants.PROPFIND_BY_PROPERTY, includes.depth);
            }

            if (!HttpServices.executeMethod(pFind, cookies)) {
                // handle error
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
        }
        return null;
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
                    return HttpServices.executeMethod(proPatch, cookies);
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


    public static void main(String[] args) {
        WorkspaceManager man = new WorkspaceManager(Partition.PUBSPACE, "111test");

        File f = man.davMakeDir("123/");
        System.out.println("new directory: " + String.valueOf(f));

        WspaceMeta m = new WspaceMeta(null, "123/");
        m.setProperty("test1", "an awesome idea");
        m.setProperty("test", null);
        man.setMeta(m);

        man.davPut(new File("/Users/loi/dia.tiff"), "/123/uploaded.tiff");

        WspaceMeta meta = man.getMeta("/", WspaceMeta.Includes.ALL_PROPS);
        System.out.println(meta.getNodesAsString());
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
