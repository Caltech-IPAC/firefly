/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.ws;


import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.FILE_PARAM;

/**
 * Handle the commands to manage ws
 *
 * @author ejoliet
 */
public class WsServerCommands {

    public static WsServerUtils utils = new WsServerUtils();

    enum RESPONSE {TRUE, FALSE};
    public enum WS_PARAM {
        FILE_PATH("relPath"),
        FILE_URL("url"),
        FILE_MOD_DATE("modifiedDate"),
        FILE_CREATED_DATE("createdDate"),
        FILE_SIZE_BYTES("sizeBytes"),
        FILE_PROPS_MAP("extraProperties"),
        FILE_CONTENT_TYPE("contentType"),
        FILE_IS_FOLDER("isFolder");

        private final String key;

        WS_PARAM(String key) {
            this.key = key;
        }

        String getKey() {
            return this.key;
        }
    }

    static WsServerUtils getWsUtils() {
        return utils;
    }

    public static class WsList extends ServCommand {

        /**
         * Get full list of file tree from relative path
         *
         * @param params see {@link WsServerParams}
         * @return
         * @throws Exception
         */
        public String doCommand(SrvParam params) throws Exception {

            WsServerParams wsParams = convertToWsServerParams(params);

            // Full list , depth = -1
            WsResponse resp = getWsUtils().getList(wsParams, -1);

            JSONObject resultJson = new JSONObject();

            if (resp.doContinue()) {
                JSONArray jsonOArray = WsServerUtils.toJson(resp.getWspaceMeta());

                resultJson.put("ok", RESPONSE.TRUE.name().toLowerCase());
                resultJson.put("result", jsonOArray);
            } else {
                resultJson.put("ok", RESPONSE.FALSE.name().toLowerCase());
                resultJson.put("status", resp.getStatusText());
                resultJson.put("statusCode", resp.getStatusCode());

            }

            return resultJson.toJSONString();

        }
    }

    /**
     * Use for uploading a file from workspace: get the file and return either
     * cache version of the file path location in firefly server or
     * the url to be used as POST from HTML (such as Gator)
     */
    public static class WsUploadFile extends ServCommand {

        /**
         * Will upload file to server and return the path where it is to be used
         *
         * @param sp see {@link WsServerParams}
         * @return the file path uploaded from workspace to firefly server
         * @throws Exception
         */
        public String doCommand(SrvParam sp) throws Exception {

            WsServerParams wsParams = convertToWsServerParams(sp);

            String upload = getWsUtils().upload(wsParams);

            return upload;
        }

    }

    /**
     * Use to get the file downaloded into local disk
     * TODO not implemented
     */
    public static class WsGetFile extends ServCommand {

        /**
         * TODO
         *
         * @param sp
         * @return
         * @throws Exception
         */
        public String doCommand(SrvParam sp) throws Exception {

            throw new IllegalArgumentException("Not implemented yet");
        }

    }

    private static String getResponseOnRelpath(WsResponse wsResp, WsServerParams wsParams) throws Exception {
        JSONObject resultJson = new JSONObject();

        if (wsResp.doContinue()) {
            WsResponse resp = getWsUtils().getList(wsParams, 0);
            JSONArray jsonOArray = WsServerUtils.toJson(resp.getWspaceMeta());

            resultJson.put("ok", RESPONSE.TRUE.name().toLowerCase());
            resultJson.put("result", jsonOArray);
        } else {
            resultJson.put("ok", RESPONSE.FALSE.name().toLowerCase());
            resultJson.put("status", wsResp.getStatusText());
        }
        return resultJson.toJSONString();

    }

    private static String getResultResponse(WsResponse wsResp) throws Exception {
        JSONObject resultJson = new JSONObject();

        if (wsResp.doContinue()) {
            resultJson.put("ok", RESPONSE.TRUE.name().toLowerCase());
        } else {
            resultJson.put("ok", RESPONSE.FALSE.name().toLowerCase());
            resultJson.put("status", wsResp.getStatusText());
        }
        return resultJson.toJSONString();

    }
    /**
     * Use for saving to workspace a table file download
     * Basically getting the stream/cache file from firefly server workarea.
     */
    public static class WsPutTableFile extends ServCommand {

        /**
         * If table, it will get it and then put it into WS under relative path passed to {@link WsServerParams}
         *
         * @param sp parameters
         * @return response as JSON string
         * @throws Exception
         */
        public String doCommand(SrvParam sp) throws Exception {

            TableServerRequest request = sp.getTableServerRequest();
            if (request == null) throw new IllegalArgumentException("Invalid/Missing table request");

            TableUtil.Format tblFormat = sp.getTableFormat();
            String fileNameExt = tblFormat.getFileNameExt();
            TableUtil.Mode mode = sp.contains("mode") ? TableUtil.Mode.valueOf(sp.getOptional("mode")) : null;

            File file = File.createTempFile(request.getRequestId(), fileNameExt, QueryUtil.getTempDir(request));

            SearchManager am = new SearchManager();
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file), (int) (32 * FileUtil.K))) {
                am.save(out, request, tblFormat, mode);
            }

            WsServerParams wsParams = convertToWsServerParams(sp);
            String fileName = wsParams.getRelPath();
            if (!fileName.toLowerCase().endsWith(fileNameExt)) {
                fileName += fileNameExt;
                wsParams.set(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH, fileName);
            }
            WsResponse wsResponse = getWsUtils().putFile(wsParams, file);

            return getResponseOnRelpath(wsResponse, wsParams);
            //return wsResponse.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();
        }
    }

    /**
     * Use for saving FITS/png to workspace
     * TODO images are following {@link AnyFileDownload}, what about DS9 (region files?) , packaging?
     */
    public static class WsPutImgFile extends ServCommand {

        /**
         * If FITS/PNG, it will get the file and will put it into WS under relative path passed to {@link WsServerParams}
         *
         * @param sp see {@link WsServerParams}, need {@link edu.caltech.ipac.firefly.server.servlets.AnyFileDownload#FILE_PARAM} to be set
         * @return
         * @throws Exception
         */
        public String doCommand(SrvParam sp) throws Exception {
            WsServerParams wsParams = convertToWsServerParams(sp);
            String fname = sp.getRequired(FILE_PARAM); // Logic should follow from FitsDownloadDialog.jsx. file in the server
            File downloadFile = ServerContext.convertToFile(fname);

            WsResponse wsResponse = getWsUtils().putFile(wsParams, downloadFile);
            
            return getResponseOnRelpath(wsResponse, wsParams);

            //return resp.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();

        }
    }


    /**
     * Remove the file from workspace and return list of content of files structure
     */
    public static class WsDeleteFile extends ServCommand {

        /**
         * Delete file
         *
         * @param params should contain the relative path to file, see {@link WsServerParams}
         * @return
         * @throws Exception
         */
        public String doCommand(SrvParam params) throws Exception {
            WsServerParams wsParams = convertToWsServerParams(params);

            WsResponse resp = getWsUtils().deleteFile(wsParams);

            return getResultResponse(resp);
        }
    }


    /**
     * Move file from current path to new path
     */
    public static class WsMoveFile extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {

            WsServerParams wsParams = convertToWsServerParams(params);
            WsResponse resp = getWsUtils().move(wsParams);
            wsParams.set(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH, wsParams.getNewPath());

            return getResponseOnRelpath(resp, wsParams);
        }
    }

    /**
     * Gets children metadata props of relative path
     */
    public static class WsGetMeta extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {

            WsServerParams wsParams = convertToWsServerParams(params);

            List<WspaceMeta> childNodes = getWsUtils().getMeta(wsParams, WspaceMeta.Includes.CHILDREN_PROPS);

            return WsServerUtils.toJson(childNodes).toJSONString();
        }
    }

    /*
     * Sets metadata props of relative path
     */
    public static class WsSetMeta extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {

            WsServerParams wsParams = convertToWsServerParams(params);

            List<WspaceMeta> childNodes = getWsUtils().getMeta(wsParams, WspaceMeta.Includes.CHILDREN_PROPS);

            return WsServerUtils.toJson(childNodes).toJSONString();
        }
    }

    /**
     * Create folder
     */
    public static class WsCreateParent extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {
            WsServerParams wsParams = convertToWsServerParams(params);

            WsResponse resp = getWsUtils().createParent(wsParams);

            return getResponseOnRelpath(resp, wsParams);
            //return resp.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();
        }
    }

    public static WsServerParams convertToWsServerParams(SrvParam sp) {
        WsServerParams params1 = new WsServerParams();

        params1.set(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH, sp.getOptional(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH.getKey()));
        params1.set(WsServerParams.WS_SERVER_PARAMS.NEWPATH, sp.getOptional(WsServerParams.WS_SERVER_PARAMS.NEWPATH.getKey()));
        params1.set(WsServerParams.WS_SERVER_PARAMS.SHOULD_OVERWRITE, sp.getOptional(WsServerParams.WS_SERVER_PARAMS.SHOULD_OVERWRITE.getKey()));

        return params1;
    }
}

