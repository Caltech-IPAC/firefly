/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.ws;


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import org.json.simple.JSONArray;

import java.io.File;
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
        FILE_CONTENT_TYPE("contentType");

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
            JSONArray jsonObject = WsServerUtils.toJson(resp.getWspaceMeta());

            return jsonObject.toJSONString();

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

    /**
     * Use for saving to workspace a table file download
     * Basically getting the stream/cache file from firefly server workarea.
     */
    public static class WsPutTableFile extends ServCommand {

        /**
         * If table, it will get it and then put it into WS under relative path passed to {@link WsServerParams}
         *
         * @param sp
         * @return
         * @throws Exception
         */
        public String doCommand(SrvParam sp) throws Exception {

            TableServerRequest request = sp.getTableServerRequest();
            if (request == null) throw new IllegalArgumentException("Invalid/Missing table request");
            FileInfo f = new SearchManager().getFileInfo(request);

            WsServerParams wsParams = convertToWsServerParams(sp);
            WsResponse wsResponse = getWsUtils().putFile(wsParams, f.getFile());

            return wsResponse.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();
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
            String fname = sp.getRequired(FILE_PARAM); // Logic should follow from FitsDownloadDialog.jsx
            File downloadFile = ServerContext.convertToFile(fname);

            getWsUtils().putFile(wsParams, downloadFile);

            WsResponse resp = getWsUtils().getList(wsParams, -1);

            return resp.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();

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

            getWsUtils().deleteFile(wsParams);

            WsResponse resp = getWsUtils().getList(wsParams, -1);

            return resp.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();
        }
    }

    /**
     * Move file from current path to new path
     */
    public static class WsMoveFile extends ServCommand {

        public String doCommand(SrvParam params) throws Exception {

            WsServerParams wsParams = convertToWsServerParams(params);

            getWsUtils().move(wsParams);

            WsResponse resp = getWsUtils().getList(wsParams, -1);

            return resp.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();
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

    /**
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

            return resp.doContinue()?RESPONSE.TRUE.name().toLowerCase():RESPONSE.FALSE.name().toLowerCase();
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

