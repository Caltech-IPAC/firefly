/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/8/12
 * Time: 1:27 PM
 */


import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.servlets.CommandService;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.JsonTableUtil;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.visualize.plot.ImagePt;
import nom.tam.fits.FitsException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.*;

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


    public static class FileFluxCmdJson extends ServerCommandAccess.ServCommand {
        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {


            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            ImagePt pt = sp.getRequiredImagePt("pt");

            FileAndHeaderInfo fahAry[];
            List<FileAndHeaderInfo> list = new ArrayList<FileAndHeaderInfo>(state.getBands().length);
            for(Band b : state.getBands()) {
                list.add(state.getFileAndHeaderInfo(b));
            }
            fahAry = list.toArray(new FileAndHeaderInfo[list.size()]);

            String[] res = VisServerOps.getFileFlux(fahAry, pt);


            JSONObject obj= new JSONObject();
            obj.put("JSON", true);
            obj.put("success", true);

            JSONObject data= new JSONObject();
            Band[] bands = state.getBands();
            for (int i=0; i<res.length; i++){
                data.put(bands[i].toString(), res[i]);
            }
            data.put("success", true);

            JSONArray wrapperAry= new JSONArray();
            obj.put("data", data);
            wrapperAry.add(obj);

            return wrapperAry.toJSONString();
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

            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class GetWebPlotGroupCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {


            SrvParam sp= new SrvParam(paramMap);
            String key = sp.getRequired(ServerParams.PROGRESS_KEY);
            List<WebPlotRequest> reqList= sp.getRequestList();
            WebPlotResult resultAry[] = VisServerOps.createPlotGroup(reqList,key);

            return WebPlotResultSerializer.createJson(resultAry, sp.isJsonDeep());
        }
    }

    public static class ZoomCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);

            PlotState stateAry[]= sp.getStateAry();
            float level= sp.getRequiredFloat(ServerParams.LEVEL);
            boolean isFull = sp.getOptionalBoolean(ServerParams.FULL_SCREEN, false);

            WebPlotResult result = VisServerOps.setZoomLevel(stateAry, level, false, isFull);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class StretchCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            boolean jsonDeep= sp.getOptionalBoolean(ServerParams.JSON_DEEP,false);
            List<StretchData> list = new ArrayList<StretchData>(3);

            StretchData sd;
//            sd = StretchData.parse(sp.getOptional(ServerParams.STRETCH_DATA + "0"));
            sd= VisJsonSerializer.deserializeStretchDataFromString(
                    sp.getOptional(ServerParams.STRETCH_DATA + "0"),jsonDeep);
            if (sd != null) list.add(sd);
            sd= VisJsonSerializer.deserializeStretchDataFromString(
                    sp.getOptional(ServerParams.STRETCH_DATA + "1"),jsonDeep);
//            sd = StretchData.parse(sp.getOptional(ServerParams.STRETCH_DATA + "1"));
            if (sd != null) list.add(sd);
//            sd = StretchData.parse(sp.getOptional(ServerParams.STRETCH_DATA + "2"));
            sd= VisJsonSerializer.deserializeStretchDataFromString(
                    sp.getOptional(ServerParams.STRETCH_DATA + "2"),jsonDeep);
            if (sd != null) list.add(sd);

            StretchData sdAry[] = list.toArray(new StretchData[list.size()]);


            if (sdAry.length == 0) {
                throw new IllegalArgumentException("missing parameters");
            }

            WebPlotResult result = VisServerOps.recomputeStretch(state, sdAry);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class AddBandCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            Band band = Band.parse(sp.getRequired(ServerParams.BAND));
            WebPlotRequest req= WebPlotRequest.parse(sp.getRequired(ServerParams.REQUEST));
            WebPlotResult result = VisServerOps.addColorBand(state, req, band);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class RemoveBandCmd extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            Band band = Band.parse(sp.getRequired(ServerParams.BAND));
            WebPlotResult result = VisServerOps.deleteColorBand(state, band);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }


    public static class ChangeColor extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            int idx= sp.getRequiredInt(ServerParams.COLOR_IDX);
            WebPlotResult result = VisServerOps.changeColor(state, idx);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class Crop extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            ImagePt pt1= sp.getRequiredImagePt(ServerParams.PT1);
            ImagePt pt2= sp.getRequiredImagePt(ServerParams.PT2);
            boolean cropMultiAll= sp.getOptionalBoolean(ServerParams.CRO_MULTI_ALL, false);
            if (paramMap.containsKey(ServerParams.STATE)) {
                PlotState state= sp.getState();
                WebPlotResult result = VisServerOps.crop(state, pt1, pt2, cropMultiAll);
                return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
            }
            else {
                PlotState stateAry[]= sp.getStateAry();
                WebPlotResult result = VisServerOps.crop(stateAry, pt1, pt2, cropMultiAll);
                return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
            }
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
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class Header extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            return WebPlotResultSerializer.createJson(VisServerOps.getFitsHeaderInfo(state), sp.isJsonDeep());
        }
    }


  /**
  * 03/20/16, LZ
  * DM-4494
   */

    public static class FitsHeader extends ServerCommandAccess.ServCommand  {

       public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException, FitsException, IOException {

            SrvParam sp= new SrvParam(paramMap);

            String tableID = paramMap.get("tableId")[0];


            //TableServerRequest req=TableServerRequest.parse(sp.getRequired(ServerParams.FITS_HEADER));
            PlotState state= sp.getState();

           Object[]  dataInfo = VisServerOps.getFitsHeader(state, tableID);
           HashMap<String, DataGroup> dataGroupMap= (HashMap<String, DataGroup> ) dataInfo[0];
           HashMap<String, TableMeta> metaMap = ( HashMap<String, TableMeta> ) dataInfo[1];

           TableServerRequest treq = new TableServerRequest("fitsHeaderTale");
           treq.setPageSize(Integer.MAX_VALUE);
           return JsonTableUtil.toJsonTableModelMap(dataGroupMap, metaMap, treq).toJSONString();


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
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }


    public static class RotateNorth extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {

            SrvParam sp= new SrvParam(paramMap);
//            PlotState state= sp.getState();
            PlotState stateAry[]= sp.getStateAry();
            boolean north= sp.getRequiredBoolean(ServerParams.NORTH);
            float zoomLevel= sp.getOptionalFloat(ServerParams.ZOOM, -1);
            WebPlotResult result = VisServerOps.rotateNorth(stateAry, north,zoomLevel);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class RotateAngle extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
//            PlotState state= sp.getState();
            PlotState stateAry[]= sp.getStateAry();
            boolean rotate= sp.getRequiredBoolean(ServerParams.ROTATE);
            double angle= rotate ? sp.getRequiredDouble(ServerParams.ANGLE) : 0.0;
            float zoomLevel= sp.getOptionalFloat(ServerParams.ZOOM, -1);
            WebPlotResult result = VisServerOps.rotateToAngle(stateAry, rotate, angle,zoomLevel);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }


    public static class FlipImageOnY extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            PlotState state= sp.getState();
            WebPlotResult result = VisServerOps.flipImageOnY(state);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
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

            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
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
            SrvParam sp= new SrvParam(paramMap);
            String key = sp.getRequired(ServerParams.PROGRESS_KEY);
            WebPlotResult result= VisServerOps.checkPlotProgress(key);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }

    public static class DS9Region extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            String fileKey = sp.getRequired(ServerParams.FILE_KEY);
            WebPlotResult result= VisServerOps.getDS9Region(fileKey);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }


    public static class SaveDS9Region extends ServerCommandAccess.ServCommand {

        public String doCommand(Map<String, String[]> paramMap) throws IllegalArgumentException {
            SrvParam sp= new SrvParam(paramMap);
            String data = sp.getRequired(ServerParams.REGION_DATA);
            WebPlotResult result= VisServerOps.saveDS9RegionFile(data);
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
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
            return WebPlotResultSerializer.createJson(result, sp.isJsonDeep());
        }
    }




    //=============================================
    //-------------- Utility Methods --------------
    //=============================================


}

