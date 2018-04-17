/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;
/**
 * User: roby
 * Date: 1/4/12
 * Time: 2:46 PM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.rpc.JsonDataCommands;
import edu.caltech.ipac.firefly.server.rpc.PushCommands;
import edu.caltech.ipac.firefly.server.rpc.ResolveServerCommands;
import edu.caltech.ipac.firefly.server.rpc.VisServerCommands;
import edu.caltech.ipac.firefly.server.servlets.HttpServCommands;
import edu.caltech.ipac.firefly.server.query.SearchServerCommands;
import edu.caltech.ipac.firefly.server.ws.WsServerCommands;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON or JSONP access to the server functionality. Each task is a ServCommand in the map. The parameters are parsed for
 * that command and it is called. New commands must be added to initCommand
 *
 * @author Trey Roby
 */

public class ServerCommandAccess {


    private static Map<String, HttpCommand> _cmdMap = new HashMap<>();

    static {
        initCommand();
    }

    private static void initCommand() {
        _cmdMap.put(ServerParams.FILE_FLUX_JSON, new VisServerCommands.FileFluxCmdJson());
        _cmdMap.put(ServerParams.CREATE_PLOT,  new VisServerCommands.GetWebPlotCmd());
        _cmdMap.put(ServerParams.CREATE_PLOT_GROUP,  new VisServerCommands.GetWebPlotGroupCmd());
        _cmdMap.put(ServerParams.ZOOM,         new VisServerCommands.ZoomCmd());
        _cmdMap.put(ServerParams.STRETCH,      new VisServerCommands.StretchCmd());
        _cmdMap.put(ServerParams.GET_BETA,     new VisServerCommands.GetBetaCmd());
        _cmdMap.put(ServerParams.REMOVE_BAND,  new VisServerCommands.RemoveBandCmd());
        _cmdMap.put(ServerParams.CHANGE_COLOR, new VisServerCommands.ChangeColor());
        _cmdMap.put(ServerParams.DELETE,       new VisServerCommands.DeletePlot());
        _cmdMap.put(ServerParams.CROP,         new VisServerCommands.Crop());
        _cmdMap.put(ServerParams.HISTOGRAM,    new VisServerCommands.ColorHistogram());
        _cmdMap.put(ServerParams.STAT,         new VisServerCommands.AreaStat());
        _cmdMap.put(ServerParams.FITS_HEADER,  new VisServerCommands.FitsHeader());   //LZ 3/21/16  DM-4494

        _cmdMap.put(ServerParams.IMAGE_PNG,    new VisServerCommands.GetImagePng());
        _cmdMap.put(ServerParams.IMAGE_PNG_REG,    new VisServerCommands.GetImagePngWithRegion());
        _cmdMap.put(ServerParams.PROGRESS,     new VisServerCommands.GetProgress());
        _cmdMap.put(ServerParams.DS9_REGION,   new VisServerCommands.DS9Region());
        _cmdMap.put(ServerParams.SAVE_DS9_REGION,      new VisServerCommands.SaveDS9Region());
        _cmdMap.put(ServerParams.ADD_SAVED_REQUEST,    new VisServerCommands.AddSavedRequest());
        _cmdMap.put(ServerParams.GET_IMAGE_MASTER_DATA, new VisServerCommands.GetMasterImageData());

        //Workspaces
        _cmdMap.put(ServerParams.WS_LIST,               new WsServerCommands.WsList());
        _cmdMap.put(ServerParams.WS_GET_FILE,           new WsServerCommands.WsGetFile());
        _cmdMap.put(ServerParams.WS_UPLOAD_FILE,           new WsServerCommands.WsUploadFile());
        _cmdMap.put(ServerParams.WS_DELETE_FILE,        new WsServerCommands.WsDeleteFile());
        _cmdMap.put(ServerParams.WS_MOVE_FILE,          new WsServerCommands.WsMoveFile());
        _cmdMap.put(ServerParams.WS_GET_METADATA,       new WsServerCommands.WsGetMeta());
        _cmdMap.put(ServerParams.WS_CREATE_FOLDER,      new WsServerCommands.WsCreateParent());
        _cmdMap.put(ServerParams.WS_PUT_TABLE_FILE,     new WsServerCommands.WsPutTableFile());
        _cmdMap.put(ServerParams.WS_PUT_IMAGE_FILE,     new WsServerCommands.WsPutImgFile());

        _cmdMap.put(ServerParams.JSON_DATA,              new SearchServerCommands.GetJSONData());
        _cmdMap.put(ServerParams.SUB_BACKGROUND_SEARCH,  new SearchServerCommands.SubmitBackgroundSearch());
        _cmdMap.put(ServerParams.GET_STATUS,             new SearchServerCommands.GetStatus());
        _cmdMap.put(ServerParams.ADD_JOB,                new SearchServerCommands.AddBgJob());
        _cmdMap.put(ServerParams.REMOVE_JOB,             new SearchServerCommands.RemoveBgJob());
        _cmdMap.put(ServerParams.CANCEL,                 new SearchServerCommands.Cancel());
        _cmdMap.put(ServerParams.CLEAN_UP,               new SearchServerCommands.CleanUp());
        _cmdMap.put(ServerParams.DOWNLOAD_PROGRESS,      new SearchServerCommands.DownloadProgress());
        _cmdMap.put(ServerParams.SET_EMAIL,              new SearchServerCommands.SetEmail());
        _cmdMap.put(ServerParams.SET_ATTR,               new SearchServerCommands.SetAttribute());
        _cmdMap.put(ServerParams.GET_EMAIL,              new SearchServerCommands.GetEmail());
        _cmdMap.put(ServerParams.RESEND_EMAIL,           new SearchServerCommands.ResendEmail());
        _cmdMap.put(ServerParams.CLEAR_PUSH_ENTRY,       new SearchServerCommands.ClearPushEntry());
        _cmdMap.put(ServerParams.REPORT_USER_ACTION,     new SearchServerCommands.ReportUserAction());
        _cmdMap.put(ServerParams.CREATE_DOWNLOAD_SCRIPT, new SearchServerCommands.CreateDownloadScript());
        _cmdMap.put(ServerParams.PACKAGE_REQUEST,        new SearchServerCommands.PackageRequest());
        _cmdMap.put(ServerParams.TABLE_SEARCH,           new SearchServerCommands.TableSearch());
        _cmdMap.put(ServerParams.QUERY_TABLE,         new SearchServerCommands.QueryTable());
        _cmdMap.put(ServerParams.SELECTED_VALUES,        new SearchServerCommands.SelectedValues());
        _cmdMap.put(ServerParams.JSON_SEARCH,            new SearchServerCommands.JsonSearch());

        _cmdMap.put(ServerParams.TABLE_SAVE,             new HttpServCommands.TableSave());
        _cmdMap.put(ServerParams.UPLOAD,                 new HttpServCommands.Upload());

        _cmdMap.put(ServerParams.RESOLVE_NAME,           new ResolveServerCommands.ResolveName());

        _cmdMap.put(ServerParams.VIS_PUSH_ALIVE_CHECK,   new PushCommands.PushAliveCheck());
        _cmdMap.put(ServerParams.VIS_PUSH_ALIVE_COUNT,   new PushCommands.PushAliveCount());
        _cmdMap.put(ServerParams.VIS_PUSH_ACTION,        new PushCommands.PushAction());

        _cmdMap.put(ServerParams.LOGOUT,        new AppServerCommands.Logout());
        _cmdMap.put(ServerParams.INIT_APP,      new AppServerCommands.InitApp());

        // maybe temporary
        _cmdMap.put(ServerParams.STATIC_JSON_DATA,           new JsonDataCommands.StaticJsonData());
    }

    public static HttpCommand getCommand(String cmd) {
        return _cmdMap.get(cmd);
    }

    public static abstract class HttpCommand {
        abstract public void processRequest(HttpServletRequest req, HttpServletResponse res, SrvParam sp) throws Exception;
    }


    /*
        // 1 hard one left
    public WebPlotResult getTableData(WebPlotRequest request) { }

    */
}

