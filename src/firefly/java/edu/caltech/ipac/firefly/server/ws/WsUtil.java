package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.ResponseMessage;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Created by ejoliet on 6/20/17.
 */
public class WsUtil {

    public static WsResponse error(int code, String status) {

        return new WsErrorResponse(code + "", status);
    }

    public static WsResponse error(HttpMethod meth) {

        return new WsErrorResponse(meth.getStatusCode() + "", meth.getStatusText());
    }

    public static WsResponse error(HttpMethod meth, String extraMessage) {

        WsResponse err = error(meth);
        err.setResponse(extraMessage);

        return err;
    }

    public static WsResponse error(int code, String status, String extraMessage) {

        WsResponse err = error(code, status);
        err.setResponse(extraMessage);

        return err;
    }

    public static WsResponse error(int code) {

        String httpResponseMessage = ResponseMessage.getHttpResponseMessage(code);

        return new WsErrorResponse(code + "", httpResponseMessage);
    }

    public static WsResponse success(int code, String status, String bodyString) {

        return new WsResponse(code + "", status, bodyString);
    }

    public static WsResponse error(Exception e) {
        WsResponse err = error(-1, e.toString());
        err.setResponse(e.getMessage());
        return err;
    }

    private static class WsErrorResponse extends WsResponse {
        public WsErrorResponse(String code, String status) {
            super(code, status, null);
        }

        @Override
        public boolean doContinue() {
            return false;
        }
    }

    public static String buildRemoteUri(WorkspaceManager m, String relFilePath) {
        String wsUrl = m.getProp(WorkspaceManager.PROPS.ROOT_URL);
        String wsHome = WspaceMeta.ensureWsHomePath(m.getWsHome());

        return wsUrl + wsHome + WspaceMeta.ensureRelPath(relFilePath);
    }

    /**
     * Adds "/" at the end if not present
     *
     * @param path
     * @return always with "/" at the end
     */
    public static String ensureUriFolderPath(String path) {
        String okUri = StringUtils.isEmpty(path) ? "" : path;
        if (!path.endsWith("/")) {
            okUri += "/";
        }
        return okUri;
    }
}
