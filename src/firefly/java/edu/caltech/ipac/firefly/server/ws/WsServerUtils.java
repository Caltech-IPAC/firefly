package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.server.ws.WsServerCommands.WS_PARAM.*;

/**
 * WS utils class for server commands to Josn and trigger action to workspace
 */
public class WsServerUtils {


    /**
     * Get list of content (see {@link edu.caltech.ipac.firefly.server.ws.WsServerCommands.WS_PARAM} fromm workspace action {@link WorkspaceManager#getList(String, int)}
     *
     * @param wsParams see {@link edu.caltech.ipac.firefly.server.ws.WsServerParams}
     * @param depth    if -1, give all the folders/files below the parent, 0 return the parent, 1 return children only (one level)
     * @return list of nodes, see depth
     * @throws IOException
     */
    public List<WspaceMeta> getList(WsServerParams wsParams, int depth) throws IOException {
        WorkspaceManager wsm = getWsManager(wsParams);
        WsResponse wsResp = wsm.getList(wsParams.getRelPath(), depth);

        return wsResp.getWspaceMeta();
    }

    /**
     * Put file into the workspace {@link WorkspaceManager#putFile(String, File, String)}
     *
     * @param wsParams see {@link edu.caltech.ipac.firefly.server.ws.WsServerParams}
     * @return list of nodes, see depth
     * @throws IOException
     */
    public List<WspaceMeta> putFile(WsServerParams wsParams, File item) throws IOException {
        WorkspaceManager wsm = getWsManager(wsParams);
        WsResponse wsResp = wsm.putFile(wsParams.getRelPath(), item, null);

        return wsResp.getWspaceMeta();
    }

    /**
     * Delete in workspace {@link WorkspaceManager#delete(String)}
     *
     * @param wsParams see {@link edu.caltech.ipac.firefly.server.ws.WsServerParams}
     * @return list of nodes, see depth
     * @throws IOException
     */
    public List<WspaceMeta> deleteFile(WsServerParams wsParams) throws IOException {
        WorkspaceManager wsm = getWsManager(wsParams);
        WsResponse wsResp = wsm.delete(wsParams.getRelPath());

        return wsResp.getWspaceMeta();
    }

    /**
     * Move (or rename) expect old and new path. By default overwrite is nor allowed, see {@link WsServerParams#shouldOverwrite()}
     *
     * @param wsParams see {@link edu.caltech.ipac.firefly.server.ws.WsServerParams}
     * @return list of metadata depending on depth
     * @throws WsException
     */
    public List<WspaceMeta> move(WsServerParams wsParams) throws IOException {
        WorkspaceManager wsm = getWsManager(wsParams);

        WsResponse wsResp = wsm.moveFile(wsParams.getRelPath(), wsParams.getNewPath(), wsParams.shouldOverwrite());

        return wsResp.getWspaceMeta();
    }

    /**
     * Gets all props children from rel path
     *
     * @param wsParams see {@link edu.caltech.ipac.firefly.server.ws.WsServerParams}
     * @param level    see {@link WspaceMeta.Includes}
     * @return list of metadata
     * @throws IOException
     */
    public List<WspaceMeta> getMeta(WsServerParams wsParams, WspaceMeta.Includes level) throws IOException {
        WorkspaceManager wsm = getWsManager(wsParams);

        WspaceMeta meta = wsm.getMeta(wsParams.getRelPath(), level);

        return meta.getAllNodes();
    }

    /**
     * Get ALL metadata from a node
     * @param wsParams
     * @return
     * @throws IOException
     */
    public WspaceMeta getMeta(WsServerParams wsParams) throws IOException {
        WorkspaceManager wsm = getWsManager(wsParams);

        WspaceMeta meta = wsm.getMeta(wsParams.getRelPath(), WspaceMeta.Includes.ALL);

        return meta;
    }
    public List<WspaceMeta> createParent(WsServerParams wsParams) throws WsException {
        WorkspaceManager wsm = getWsManager(wsParams);

        WsResponse wsResp = wsm.createParent(wsParams.getNewPath());

        return wsResp.getWspaceMeta();
    }

    /**
     * Gets the credentials from session cookie on the browser (see requestOwner) used to connect to Workspace
     *
     * @return credentials
     */
    public WsCredentials getCredentials() {
        RequestOwner ro = ServerContext.getRequestOwner();
        Map<String, String> cookies = null;
        String loginUid = null;
        if (ro != null) {
            cookies = ro.getCookieMap();
            loginUid = ro.getUserKey();
        }
        return new WsCredentials(loginUid, cookies);
    }

    public static JSONArray toJson(List<WspaceMeta> childNodes) {

        JSONArray arr = new JSONArray();
        addToJson(arr, childNodes);

        return arr;
    }


    static void addToJson(JSONArray arr, List<WspaceMeta> childNodes) {
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

    private static WsJson serializeWsMeta(WspaceMeta next) {
        WsJson map = new WsJson(next);
        return map;
    }

    private WorkspaceManager getWsManager(WsServerParams wsParams) {
        WsCredentials cred = getCredentials();

        WorkspaceManager wsm = WorkspaceFactory.getWorkspaceHandler().withCredentials(cred);

        return wsm;
    }

    public String upload(WsServerParams wsParams) throws IOException, FailedRequestException {

        WspaceMeta meta = getMeta(wsParams);
        File file = getTempUploadFile();
        URLConnection conn = URLDownload.makeConnection(new URL(meta.getUrl()), getCredentials().getCookies());
        conn.setRequestProperty("Accept", "*/*");

        File f = getTempUploadFile();
        URLDownload.getDataToFile(conn, f);

        String rPathInfo = ServerContext.replaceWithPrefix(f);

        return rPathInfo;
    }

    public File getTempUploadFile() throws IOException {
        return File.createTempFile("ws-upload", ".tmp", ServerContext.getTempWorkDir());
    }

    public static class WsJson {
        private final WspaceMeta meta;

        public WsJson(WspaceMeta next) {
            this.meta = next;
        }

        JSONArray toJson() {
            JSONArray arr = new JSONArray();
            JSONObject map = new JSONObject();
            map.put(FILE_PATH.getKey(), meta.getRelPath());
            map.put(FILE_SIZE_BYTES.getKey(), meta.getSize());
            map.put(FILE_MOD_DATE.getKey(), meta.getLastModified());
            map.put(FILE_CREATED_DATE.getKey(), meta.getCreationDate());
            map.put(FILE_CONTENT_TYPE.getKey(), meta.isFile() ? meta.getContentType() : "folder");
            map.put(FILE_URL.getKey(), meta.getUrl());

            JSONObject propObj = new JSONObject();
            Map<String, String> props = meta.getProperties();
            if (props != null) {
                for (String key : props.keySet()) {
                    if (props.get(key) != null)
                        propObj.put(key, props.get(key));
                }
                map.put(FILE_PROPS_MAP.getKey(), propObj);
            }

            arr.add(map);

            return arr;
        }

        public void to(JSONArray arr) {
            arr.add(this.toJson());
        }
    }

    String resolveExt(String fileName) {
        String ext = StringUtils.isEmpty(fileName) ? "" : FileUtil.getExtension(fileName);
        ext = StringUtils.isEmpty(ext) ? ".tmp" : "." + ext;
        return ext;
    }
}
