package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.firefly.server.ws.WsException;
import edu.caltech.ipac.firefly.server.ws.WsResponse;
import edu.caltech.ipac.firefly.server.ws.WsUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.*;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.xml.Namespace;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
public abstract class WebDAVWorkspaceManagerBase implements WorkspaceManager {

    protected static String WS_HOST_URL = AppProperties.getProperty("workspace.host.url", "https://irsa.ipac.caltech.edu");

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    @Override
    public String getProp(PROPS prop) {
        // TODO: this method is used only in test - do we need it?
        throw new IllegalArgumentException("not implemented");
    }


    /**
     * returns the webdav's absolute path.  for IRSA, it's /partition/user_ws/relpath.
     *
     * @param relPath relative to the user's workspace and must starts with '/'
     * @return absolute path to a resource
     */
    private String getAbsPath(String relPath) {
        String s = WspaceMeta.ensureRelPath(relPath);
        return getWsHome() + s;
    }

    /**
     * return the url of this resource.
     *
     * @param relPath relative path to a resource
     * @return url to a resource
     */
    String getResourceUrl(String relPath) {
        String valid = null;
        try {
            valid = WsUtil.encode(relPath);
        } catch (URISyntaxException e) {
            LOG.error(e, "Continue with relative path as it is: "+relPath);
            e.printStackTrace();
        }
        return WS_HOST_URL + getAbsPath(valid);
    }

    private Namespace getNamespace() {
        return Namespace.getNamespace(getNamespacePrefix(), getNamespaceUri());
    }

//====================================================================
//  absract methods
//====================================================================

    public abstract WsCredentials getCredentials();

    public abstract String getWsHome();

    protected abstract String getNamespacePrefix();

    protected abstract String getNamespaceUri();


//====================================================================
//  WEBDAV functions
//====================================================================



    boolean exists(String relRemoteUri) {
        return getMeta(relRemoteUri, WspaceMeta.Includes.NONE) != null;
    }



    /**
     *
     * @param upload  upload file
     * @param relPath   expecting uri folder with file name attached optionally a/b
     * @param overwrite when true, put will overwrite existing file with the same name
     * @param contentType content type
     * @return WsResponse object
     */
    public WsResponse davPut(File upload, String relPath, boolean overwrite, String contentType) {
        try {

            int idx = relPath.lastIndexOf('/');
            String newFileName = relPath.substring(idx + 1);

            relPath = relPath.substring(0, idx+1);
            String parentPath = WsUtil.ensureUriFolderPath(relPath);
            //if (!exists(parentPath)) {
            WsResponse response = createParent(parentPath);

            String newPath;
            String newUrl;
            if (newFileName.length() == 0) {
                newPath = parentPath + upload.getName();
                newUrl =  getResourceUrl(parentPath) + WsUtil.encode(upload.getName());
            } else {
                newPath = parentPath + newFileName;
                newUrl =  getResourceUrl(parentPath) + WsUtil.encode(newFileName);
            }
            //}
            // If parent and file name exists already, stop
            if (!response.doContinue()) {
                return WsUtil.error(Integer.parseInt(response.getStatusCode()), response.getStatusText(), parentPath);
            }


            if (exists(newPath) && (!overwrite)) {
                //if (exists(relPath)) {
                return WsUtil.error(304, newUrl);// not modified, already exists
            }

            PutMethod put = new PutMethod(newUrl);

            // TODO Content Type doesn't seems to be passed on
            RequestEntity requestEntity = new InputStreamRequestEntity(new BufferedInputStream(
                    new FileInputStream(upload), HttpServices.BUFFER_SIZE), upload.length(), contentType);
            put.setRequestEntity(requestEntity);
            /* is to allow a client that is sending a request message with a request body
             *  to determine if the origin server is willing to accept the request
             * (based on the request headers) before the client sends the request body.
             * this require server supporting HTTP/1.1 protocol.
             */
            put.getParams().setBooleanParameter(
                    HttpMethodParams.USE_EXPECT_CONTINUE, true);

            if (!executeMethod(put)) {
                // handle error
                LOG.error("Unable to upload file:" + relPath + " -- " + put.getStatusText());
                return WsUtil.error(put.getStatusCode(), put.getStatusText());
            }

            return WsUtil.success(put.getStatusCode(), put.getStatusText(), newUrl);
        } catch (Exception e) {
            LOG.error(e, "Error while uploading file:" + upload.getPath());
        }
        return WsUtil.error(500);
    }

    private WsResponse davGet(File outfile, String fromPath) {
        String url = getResourceUrl(fromPath);
        WebDAVGetMethod get = new WebDAVGetMethod(url);
        InputStream in;
        try {
            if (!executeMethod(get, false)) {
                // handle error
                LOG.error("Unable to download file:" + fromPath + " , url " +url+" -- "+ get.getStatusText());
                return WsUtil.error(get.getStatusCode(), get.getStatusText());
            }
            if (get.getResponseContentLength() > 0) {
                in = get.getResponseBodyAsStream();
                FileUtils.copyInputStreamToFile(in, outfile);
            }
        } catch (Exception e) {
            LOG.error(e, "Error while downloading remote file:" + url);
            return WsUtil.error(e);
        } finally {
            get.releaseConnection();
        }
        return WsUtil.success(200, "File downloaded " + outfile.getAbsolutePath(), "");
    }

//====================================================================
//  for firefly use
//====================================================================


    @Override
    public PROTOCOL getProtocol() {
        return PROTOCOL.WEBDAV;
    }

    @Override
    public WsResponse getList(String parentUri, int depth) {

        WspaceMeta.Includes prop = WspaceMeta.Includes.CHILDREN_PROPS;

        if (depth < 0) {
            prop = WspaceMeta.Includes.ALL_PROPS;
        } else if (depth == 0) {
            prop = WspaceMeta.Includes.NONE_PROPS;
        }

        WsResponse resp = new WsResponse("200", "List", "");

        WspaceMeta meta = getMeta(parentUri, prop);
        if (meta == null) {
            return getResponse(parentUri);
        }
        List<WspaceMeta> childNodes = new ArrayList<>();
        childNodes.add(meta);

        //If no child, set the (self) meta in response:
        resp.setWspaceMeta(childNodes);

        if (meta.getChildNodes() != null) {
            // Childs replaced here:
            childNodes = meta.getChildNodes();
            resp.setWspaceMeta(childNodes);
            String respString = "";
            Iterator<WspaceMeta> it = childNodes.iterator();
            int child = 0;
            while (it.hasNext()) {
                WspaceMeta next = it.next();
                respString += next.getRelPath() + ", ";
                child++;
            }
            resp.setResponse(respString);
        }

        return resp;
    }

    @Override
    public WsResponse getFile(String fileUri, File outputFile) {
        return davGet(outputFile, fileUri);
    }

    @Override
    public WsResponse getFile(String fileUri, Consumer consumer) {
        throw new IllegalArgumentException("not implemented");
    }


    @Override
    public WsResponse putFile(String relPath, boolean overwrite, File item, String contentType) throws WsException {
        String ct = contentType;
        if (contentType == null) {
            //ct = ContentType.DEFAULT_BINARY.getMimeType();
            //ct="image/fits";
        }

        return davPut(item, relPath, overwrite, ct);
    }

    @Override
    public WsResponse delete(String uri) {
        if (WspaceMeta.ensureWsHomePath(uri).equals("")) {
            return WsUtil.error(304, "Attempt to delete home, skipping...");
        }
        String url = getResourceUrl(uri);
        WebDAVDeleteMethod rm = new WebDAVDeleteMethod(url);
        try {
            if (!executeMethod(rm, true)) {
                // handle error
                LOG.error("Unable to delete file uri:" + uri + " -- " + rm.getStatusText());
                return WsUtil.error(rm);
            }
        } catch (Exception e) {
            LOG.error(e, "Error while deleting remote file:" + url);
        }
        return WsUtil.success(200, "Deleted " + uri, uri);
    }

    @Override
    public WsResponse createParent(String newRelPath) {
        String[] parts = newRelPath.split("/");
        String cdir = "/";
        for (String s : parts) {
            cdir += StringUtils.isEmpty(s) ? "" : s + "/";
            WspaceMeta m = getMeta(cdir, WspaceMeta.Includes.NONE);
            if (m == null) {
                try {
                    URI uri = new URI(getResourceUrl(cdir));

                    DavMethod mkcol = new MkColMethod(uri.toURL().toString());
                    if (!executeMethod(mkcol)) {
                        // handle error
                        LOG.error("Unable to create directory:" + newRelPath + " -- " + mkcol.getStatusText());
                        return WsUtil.error(mkcol, "Resource already exist");
                    }
                } catch (URISyntaxException|MalformedURLException e) {
                    return WsUtil.error(e);
                }
            }
        }
        return WsUtil.success(200, "Created", getResourceUrl(newRelPath));
    }

    public WsResponse moveFile(String originalFileRelPath, String newPath, boolean overwrite) {
        WspaceMeta meta = new WspaceMeta(newPath);
        String parent = meta.getParentPath();

        //WARNING: WEBDAv forces me to create new parent first! UGLY!
        createParent(parent);

        String newUrl = getResourceUrl(newPath);

        // This doesn't create parent , you must create the parent folder in order to move it
        MoveMethod move = new MoveMethod(getResourceUrl(originalFileRelPath), newUrl, overwrite);
        if (!executeMethod(move)) {
            // handle error
            LOG.error("Unable to move:" + originalFileRelPath + " based on url -- " +newUrl+" -- "+ move.getStatusText());
            return WsUtil.error(move.getStatusCode(), move.getStatusLine().getReasonPhrase());
        }
        return WsUtil.success(move.getStatusCode(), move.getStatusText(), newUrl);
    }

    @Override
    public WsResponse renameFile(String originalFileRelPath, String newfileName, boolean overwrite) {
        WspaceMeta meta = new WspaceMeta(originalFileRelPath);
        String parent = meta.getParentPath();

        String newUrl;
        try {
            newUrl = getResourceUrl(parent) + WsUtil.encode(newfileName);
        } catch (URISyntaxException e) {
            LOG.error("Unable to convert to URI:" + newfileName);
            return WsUtil.error(e);
        }

        MoveMethod move = new MoveMethod(getResourceUrl(originalFileRelPath), newUrl, overwrite);
        if (!executeMethod(move)) {
            // handle error
            LOG.error("Unable to move:" + originalFileRelPath + " based on url -- " +newUrl+" -- "+ move.getStatusText());
            return WsUtil.error(move.getStatusCode(), move.getStatusLine().getReasonPhrase());
        }
        return WsUtil.success(move.getStatusCode(), move.getStatusText(), newUrl);
    }

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
                    LOG.error("Unable to find property:" + relPath + " -- " + pFind.getStatusText());
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
                if (res.getHref().equals(WsUtil.encode(root.getAbsPath()))) {
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
     *
     * @param relPath relative path
     * @param propName property name
     * @param value property value
     * @return true on success, false on error
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
     *
     * @param metas meta information
     * @return false on error, true otherwise
     */
    public boolean setMeta(WspaceMeta... metas) {
        if (metas == null) return false;
        Namespace namespace = getNamespace();
        for (WspaceMeta meta : metas) {

            Map<String, String> props = meta.getProperties();
            if (props != null && props.size() > 0) {
                DavPropertySet newProps = new DavPropertySet();
                DavPropertyNameSet removeProps = new DavPropertyNameSet();

                for (String key : props.keySet()) {
                    String v = props.get(key);
                    if (v == null) {
                        removeProps.add(DavPropertyName.create(key, namespace));
                    } else {
                        DavProperty p = new DefaultDavProperty(key, props.get(key), namespace);
                        newProps.add(p);
                    }
                }
                try {
                    PropPatchMethod proPatch = new PropPatchMethod(getResourceUrl(meta.getRelPath()), newProps, removeProps);
                    if (!executeMethod(proPatch)) {
                        // handle error
                        LOG.error("Unable to update property:" + newProps.toString() + " -- " + proPatch.getStatusText());
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

    private WspaceMeta convertToWspaceMeta(WspaceMeta meta, MultiStatusResponse res) throws UnsupportedEncodingException {
        if (meta == null) {
            meta = new WspaceMeta(getWsHome(), URLDecoder.decode(res.getHref().replaceFirst(getWsHome(), ""),"UTF-8"));
        }
        if (res.getHref() != null) {
            meta.setUrl(WS_HOST_URL + res.getHref());
        }
        DavPropertySet props = res.getProperties(200);
        if (props != null) {
            Namespace namespace = getNamespace();
            for (DavProperty p : props) {
                String name = (p == null || p.getName() == null) ? null : p.getName().getName();
                if (name != null) {
                    String v = String.valueOf(p.getValue());
                    if (name.equals(DavConstants.PROPERTY_GETLASTMODIFIED)) {
                        meta.setLastModified(v);
                    } else if (name.equals(DavConstants.PROPERTY_GETCONTENTLENGTH)) {
                        long size = Long.parseLong(v);
                        meta.setSize(size);
                        //meta.setIsFile(true); // WEBDAV/IRSA only set content length for files. // not WEBDav standard behaviour
                    } else if (name.equals(DavConstants.PROPERTY_GETCONTENTTYPE)) {
                        meta.setContentType(v);
                    } else if (p.getName().getNamespace().equals(namespace)) {
                        meta.setProperty(name, String.valueOf(p.getValue()));
                    } else if (name.equals(DavConstants.PROPERTY_CREATIONDATE)) {
                        meta.setCreationDate(v);
                    } else if (name.equals(DavConstants.PROPERTY_RESOURCETYPE)){
                        meta.setIsFile(v == null || !v.toLowerCase().contains("collection"));
                    }
                }
            }
        }
        return meta;
    }

    private void addToRoot(WspaceMeta root, MultiStatusResponse res) throws UnsupportedEncodingException {
        WspaceMeta meta = convertToWspaceMeta(null, res);
        WspaceMeta p = root.find(meta.getParentPath());
        if (p != null) {
            p.addChild(meta);
        } else {
            root.addChild(meta);
        }
    }
    boolean executeMethod(DavMethod method) {
        return executeMethod(method, true);
    }

    private boolean executeMethod(DavMethod method, boolean releaseConnection) {
        try {
            HttpServices.Status status = doExecuteMethod(method);
            return !status.isError();
        } catch (IOException e) {
            return false;
        } finally {
            if (releaseConnection && method != null) {
                method.releaseConnection();
            }
        }
    }

    protected HttpServices.Status doExecuteMethod(DavMethod method) throws IOException {
        HttpServiceInput input = HttpServiceInput.createWithCredential(WS_HOST_URL);
        return HttpServices.executeMethod(method, input);
    }


    private WsResponse getResponse(String relPath){
        DavMethod pFind = null;
        try {
            pFind = new PropFindMethod(getResourceUrl(relPath), DavConstants.PROPFIND_BY_PROPERTY, WspaceMeta.Includes.ALL_PROPS.depth);


            if (!executeMethod(pFind, false)) {
                // handle error
                if (pFind.succeeded()){// != 404) {
                    LOG.error("Unable to get property:" + relPath + " -- " + pFind.getStatusText());
                }
                return WsUtil.error(pFind.getStatusCode(), pFind.getStatusText());
            }
            return WsUtil.success(pFind.getStatusCode(), pFind.getStatusText(), pFind.getPath());
        } catch (Exception e) {
            LOG.error(e, "Error while getting meta for:" + relPath);
        } finally {
            if (pFind != null) {
                pFind.releaseConnection();
            }
        }
        return new WsResponse();
    }

    class WebDAVGetMethod extends DavMethodBase {
        WebDAVGetMethod(String uri) {
            super(uri);
        }

        public String getName() {
            return DavMethods.METHOD_GET;
        }

        public boolean isSuccess(int statusCode) {
            return statusCode == 200;
        }
    }

    class WebDAVDeleteMethod extends DavMethodBase {
        WebDAVDeleteMethod(String uri) {
            super(uri);
        }

        public String getName() {
            return DavMethods.METHOD_DELETE;
        }

        public boolean isSuccess(int statusCode) {
            return statusCode == 200;
        }
    }
}
