package edu.caltech.ipac.firefly.server;
/**
 * User: roby
 * Date: 1/4/12
 * Time: 2:46 PM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.visualize.ResolveServerCommands;
import edu.caltech.ipac.firefly.server.visualize.ResourceServerCommands;
import edu.caltech.ipac.firefly.server.visualize.SearchServerCommands;
import edu.caltech.ipac.firefly.server.visualize.VisServerCommands;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON or JSONP access to the server functionality. Each task is a ServCommand in the map. The parameters are parsed for
 * that command and it is called. New commands must be added to initCommand
 *
 * @author Trey Roby
 */

public class ServerCommandAccess {


    public static Map<String, ServCommand> _cmdMap = new HashMap<String, ServCommand>(41);

    static {
        initCommand();
    }


    public String doCommand(Map<String, String[]> paramMap) throws Exception {
        String retval = "";
        String cmd[] = paramMap.get(ServerParams.COMMAND);
        if (cmd != null && _cmdMap.containsKey(cmd[0])) {
//            retval = doCommand(cmd[0], decode(paramMap));
            retval = doCommand(cmd[0], paramMap);
        }
        return retval;
    }

//    private Map<String, String[]> decode(Map<String, String[]> paramMap) {
//        Map<String,String[]> retMap= new HashMap<String, String[]>(30);
//        for(Map.Entry<String,String[]> entry : paramMap.entrySet())  {
//            String sAry[]= new String[entry.getValue().length];
//            for(int i= 0; (i<entry.getValue().length); i++) {
//                try {
//                    sAry[i]= URLDecoder.decode(entry.getValue()[i], "UTF-8");
//                } catch (UnsupportedEncodingException e) {
//                    sAry[i]= "";
//                }
//            }
//            retMap.put(entry.getKey(), sAry);
//        }
//        return retMap;
//    }

    public boolean getCanCreateJson(Map<String, String[]> paramMap) {
        boolean retval = false;
        String cmd[] = paramMap.get(ServerParams.COMMAND);
        if (cmd != null && _cmdMap.containsKey(cmd[0])) {
            retval = _cmdMap.get(cmd[0]).getCanCreateJson();
        }
        return retval;
    }

    public static String doCommand(String cmd, Map<String, String[]> paramMap) throws Exception {
        String retval = "";
        if (_cmdMap.containsKey(cmd)) {
            retval = _cmdMap.get(cmd).doCommand(paramMap);
        }
        return retval;
    }


    private static void initCommand() {
        _cmdMap.put(ServerParams.FILE_FLUX,    new VisServerCommands.FileFluxCmd());
        _cmdMap.put(ServerParams.CREATE_PLOT,  new VisServerCommands.GetWebPlotCmd());
        _cmdMap.put(ServerParams.ZOOM,         new VisServerCommands.ZoomCmd());
        _cmdMap.put(ServerParams.STRETCH,      new VisServerCommands.StretchCmd());
        _cmdMap.put(ServerParams.ADD_BAND,     new VisServerCommands.AddBandCmd());
        _cmdMap.put(ServerParams.REMOVE_BAND,  new VisServerCommands.RemoveBandCmd());
        _cmdMap.put(ServerParams.CHANGE_COLOR, new VisServerCommands.ChangeColor());
        _cmdMap.put(ServerParams.DELETE,       new VisServerCommands.DeletePlot());
        _cmdMap.put(ServerParams.CROP,         new VisServerCommands.Crop());
        _cmdMap.put(ServerParams.ROTATE_NORTH, new VisServerCommands.RotateNorth());
        _cmdMap.put(ServerParams.ROTATE_ANGLE, new VisServerCommands.RotateAngle());
        _cmdMap.put(ServerParams.FLIP_Y,       new VisServerCommands.FlipImageOnY());
        _cmdMap.put(ServerParams.HISTOGRAM,    new VisServerCommands.ColorHistogram());
        _cmdMap.put(ServerParams.STAT,         new VisServerCommands.AreaStat());
        _cmdMap.put(ServerParams.HEADER,       new VisServerCommands.Header());
        _cmdMap.put(ServerParams.IMAGE_PNG,    new VisServerCommands.GetImagePng());
        _cmdMap.put(ServerParams.PROGRESS,     new VisServerCommands.GetProgress());
        _cmdMap.put(ServerParams.DS9_REGION,   new VisServerCommands.DS9Region());
        _cmdMap.put(ServerParams.SAVE_DS9_REGION,   new VisServerCommands.SaveDS9Region());

        _cmdMap.put(ServerParams.SESSION_ID,   new ResourceServerCommands.SessionID());
        _cmdMap.put(ServerParams.VERSION,      new ResourceServerCommands.GetVersion());

        _cmdMap.put(ServerParams.RAW_DATA_SET,           new SearchServerCommands.GetRawDataSet());
        _cmdMap.put(ServerParams.CHK_FILE_STATUS,        new SearchServerCommands.ChkFileStatus());
        _cmdMap.put(ServerParams.SUB_BACKGROUND_SEARCH,  new SearchServerCommands.SubmitBackgroundSearch());
        _cmdMap.put(ServerParams.GET_STATUS,             new SearchServerCommands.GetStatus());
        _cmdMap.put(ServerParams.CANCEL,                 new SearchServerCommands.Cancel());
        _cmdMap.put(ServerParams.CLEAN_UP,               new SearchServerCommands.CleanUp());
        _cmdMap.put(ServerParams.DOWNLOAD_PROGRESS,      new SearchServerCommands.DownloadProgress());
        _cmdMap.put(ServerParams.SET_EMAIL,              new SearchServerCommands.SetEmail());
        _cmdMap.put(ServerParams.SET_ATTR,               new SearchServerCommands.SetAttribute());
        _cmdMap.put(ServerParams.GET_EMAIL,              new SearchServerCommands.GetEmail());
        _cmdMap.put(ServerParams.RESEND_EMAIL,           new SearchServerCommands.ResendEmail());
        _cmdMap.put(ServerParams.CREATE_DOWNLOAD_SCRIPT, new SearchServerCommands.CreateDownloadScript());

        _cmdMap.put(ServerParams.RESOLVE_NAME,           new ResolveServerCommands.ResolveName());
    }


    public static abstract class ServCommand {
        public abstract String doCommand(Map<String, String[]> paramMap) throws Exception;

        public boolean getCanCreateJson() { return true; }
    }


    /*
        // 1 hard one left
    public WebPlotResult getTableData(WebPlotRequest request) { }

    */
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
