package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/8/12
 * Time: 1:27 PM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.servlets.CommandService;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotResultParser;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class VisServerCommands {

    public static class FileFluxCmd extends ServerCommandAccess.ServCommand {
        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            List<FileAndHeaderInfo> list = new ArrayList<FileAndHeaderInfo>(3);
            FileAndHeaderInfo fh;
            fh = FileAndHeaderInfo.parse(sp.getOptional("fah0"));
            if (fh != null) list.add(fh);
            fh = FileAndHeaderInfo.parse(sp.getOptional("fah1"));
            if (fh != null) list.add(fh);
            fh = FileAndHeaderInfo.parse(sp.getOptional("fah2"));
            if (fh != null) list.add(fh);


            ImagePt pt = sp.getRequiredImagePt("pt");

            FileAndHeaderInfo fahAry[] = list.toArray(new FileAndHeaderInfo[list.size()]);

            String[] res = VisServerOps.getFileFlux(fahAry, pt);
            String tmpStr = Arrays.toString(res);
            if (tmpStr.startsWith("[") && tmpStr.endsWith("]")) {
                tmpStr = tmpStr.substring(1, tmpStr.length() - 1);
            }
            return tmpStr;
        }

        public boolean getCanCreateJson() {
            return false;
        }
    }

    public static class GetWebPlotCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {


            SrvParam sp= new SrvParam(paramMap);
            WebPlotRequest red  =     sp.getOptionalWebPlotRequest(ServerParams.RED_REQUEST);
            WebPlotRequest green=     sp.getOptionalWebPlotRequest(ServerParams.GREEN_REQUEST);
            WebPlotRequest blue =     sp.getOptionalWebPlotRequest(ServerParams.BLUE_REQUEST);
            WebPlotRequest nobandReq= sp.getOptionalWebPlotRequest(ServerParams.NOBAND_REQUEST);

            boolean threeColor;

            if (nobandReq != null) {
                if (CommandService.DEBUG) {
                    Logger.debug("noband req: " + (nobandReq!=null ? nobandReq.toString() : "null"));
                }
                threeColor = false;
            } else if (red != null || green != null || blue != null) {
                threeColor = true;
            } else {
                throw new IllegalArgumentException("No request specified");
            }

            WebPlotResult result = threeColor ? VisServerOps.create3ColorPlot(red, green, blue) :
                                                VisServerOps.createPlot(nobandReq);

            return WebPlotResultParser.createJS(result);
        }
    }

    public static class ZoomCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            float level= sp.getRequiredFloat(ServerParams.LEVEL);
            boolean isFull = sp.getOptionalBoolean(ServerParams.FULL_SCREEN, false);

            WebPlotResult result = VisServerOps.setZoomLevel(state, level, false, isFull);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class StretchCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            List<StretchData> list = new ArrayList<StretchData>(3);

            StretchData sd;
            sd = StretchData.parse(sp.getOptional(ServerParams.STRETCH_DATA + "0"));
            if (sd != null) list.add(sd);
            sd = StretchData.parse(sp.getOptional(ServerParams.STRETCH_DATA + "1"));
            if (sd != null) list.add(sd);
            sd = StretchData.parse(sp.getOptional(ServerParams.STRETCH_DATA + "2"));
            if (sd != null) list.add(sd);

            StretchData sdAry[] = list.toArray(new StretchData[list.size()]);


            if (sdAry.length == 0) {
                throw new IllegalArgumentException("missing parameters");
            }

            WebPlotResult result = VisServerOps.recomputeStretch(state, sdAry);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class AddBandCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            Band band = Band.parse(sp.getRequired(ServerParams.BAND));
            WebPlotRequest req= WebPlotRequest.parse(sp.getRequired(ServerParams.REQUEST));
            WebPlotResult result = VisServerOps.addColorBand(state, req, band);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class RemoveBandCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            Band band = Band.parse(sp.getRequired(ServerParams.BAND));
            WebPlotResult result = VisServerOps.deleteColorBand(state, band);
            return WebPlotResultParser.createJS(result);
        }
    }


    public static class ChangeColor extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            int idx= sp.getRequiredInt(ServerParams.COLOR_IDX);
            WebPlotResult result = VisServerOps.changeColor(state, idx);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class Crop extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            ImagePt pt1= sp.getRequiredImagePt(ServerParams.PT1);
            ImagePt pt2= sp.getRequiredImagePt(ServerParams.PT2);
            boolean cropMultiAll= sp.getOptionalBoolean(ServerParams.CRO_MULTI_ALL, false);
            WebPlotResult result = VisServerOps.crop(state, pt1, pt2, cropMultiAll);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class AreaStat extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            ImagePt pt1= sp.getRequiredImagePt(ServerParams.PT1);
            ImagePt pt2= sp.getRequiredImagePt(ServerParams.PT2);
            ImagePt pt3= sp.getRequiredImagePt(ServerParams.PT3);
            ImagePt pt4= sp.getRequiredImagePt(ServerParams.PT4);
            WebPlotResult result = VisServerOps.getAreaStatistics(state, pt1, pt2, pt3, pt4);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class Header extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            PlotState state= new SrvParam(paramMap).getState();
            return WebPlotResultParser.createJS(VisServerOps.getFitsHeaderInfo(state));
        }
    }


    public static class GetImagePng extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            String drawInfoStrAry[] = paramMap.get(ServerParams.DRAW_INFO);
            List<StaticDrawInfo> drawInfoList;
            try {
                if (drawInfoStrAry != null && drawInfoStrAry.length > 0) {
                    drawInfoList = new ArrayList<StaticDrawInfo>(drawInfoStrAry.length);
                    for (String s : drawInfoStrAry) {
                        StaticDrawInfo drawInfo = StaticDrawInfo.parse(s);
                        if (s != null) drawInfoList.add(drawInfo);
                    }
                } else {
                    throw new IllegalArgumentException("missing parameters");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("parameters in wrong format");
            }

            if (drawInfoList.size() == 0) {
                throw new IllegalArgumentException("parameters in wrong format");
            }

            WebPlotResult result = VisServerOps.getImagePng(state, drawInfoList);
            return WebPlotResultParser.createJS(result);
        }
    }


    public static class RotateNorth extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            boolean north= sp.getRequiredBoolean(ServerParams.NORTH);
            float zoomLevel= sp.getOptionalFloat(ServerParams.ZOOM, -1);
            WebPlotResult result = VisServerOps.rotateNorth(state, north,zoomLevel);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class RotateAngle extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            boolean rotate= sp.getRequiredBoolean(ServerParams.ROTATE);
            double angle= rotate ? sp.getRequiredDouble(ServerParams.ANGLE) : 0.0;
            WebPlotResult result = VisServerOps.rotateToAngle(state, rotate, angle);
            return WebPlotResultParser.createJS(result);
        }
    }


    public static class FlipImageOnY extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            WebPlotResult result = VisServerOps.flipImageOnY(new SrvParam(paramMap).getState());
            return WebPlotResultParser.createJS(result);
        }
    }



    public static class ColorHistogram extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            Band band= Band.parse(sp.getRequired(ServerParams.BAND));
            int width= sp.getRequiredInt(ServerParams.WIDTH);
            int height= sp.getRequiredInt(ServerParams.HEIGHT);
            WebPlotResult result = VisServerOps.getColorHistogram(state, band, width, height);

            return WebPlotResultParser.createJS(result);
        }
    }

    public static class DeletePlot extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            String ctxStr = new SrvParam(paramMap).getRequired(ServerParams.CTXSTR);
            VisServerOps.deletePlot(ctxStr);
            return "";
        }
    }



    public static class GetProgress extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            String key = new SrvParam(paramMap).getRequired(ServerParams.PROGRESS_KEY);
            WebPlotResult result= VisServerOps.checkPlotProgress(key);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class DS9Region extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            String fileKey = new SrvParam(paramMap).getRequired(ServerParams.FILE_KEY);
            WebPlotResult result= VisServerOps.getDS9Region(fileKey);
            return WebPlotResultParser.createJS(result);
        }
    }


    public static class SaveDS9Region extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            String data = new SrvParam(paramMap).getRequired(ServerParams.REGION_DATA);
            WebPlotResult result= VisServerOps.saveDS9RegionFile(data);
            return WebPlotResultParser.createJS(result);
        }
    }

    public static class AddSavedRequest extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            String saveKey = sp.getRequired(ServerParams.SAVE_KEY);
            WebPlotRequest req= WebPlotRequest.parse(sp.getRequired(ServerParams.REQUEST));
            VisServerOps.addSavedRequest(saveKey,req);
            return "";
        }
    }

    public static class GetAllSavedRequest extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            String saveKey = sp.getRequired(ServerParams.SAVE_KEY);
            WebPlotResult result= VisServerOps.getAllSavedRequest(saveKey);
            return WebPlotResultParser.createJS(result);
        }
    }




    //=============================================
    //-------------- Utility Methods --------------
    //=============================================


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
