/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.core.JsonUtils;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotResultOverlay;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotResultParser;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: roby
 * Date: Feb 25, 2008
 * Time: 1:22:01 PM
 * @version $Id: PlotServiceJson.java,v 1.12 2013/01/07 21:33:39 tatianag Exp $
 */
public class PlotServiceJson implements PlotServiceAsync {

    private final boolean doJsonP;


    public PlotServiceJson(boolean doJsonP) {
        this.doJsonP = doJsonP;
    }


    public void getWebPlot(WebPlotRequest request, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.CREATE_PLOT, async,
                      new Param(ServerParams.NOBAND_REQUEST, request.toString()));

    }


    public void getWebPlotGroup(List<WebPlotRequest> requestList, String progressKey, AsyncCallback<WebPlotResult[]> async) {
//        doPlotService(ServerParams.CREATE_PLOT_GROUP, async,
//                      new Param(ServerParams.NOBAND_REQUEST, request.toString()));
    }

    public void getOneFileGroup(List<WebPlotRequest> requestList, String progressKey, AsyncCallback<WebPlotResult[]> async) {
//        doPlotService(ServerParams.CREATE_PLOT_GROUP, async,
//                      new Param(ServerParams.NOBAND_REQUEST, request.toString()));
    }

    public void getWebPlot(WebPlotRequest redRequest, WebPlotRequest greenRequest, WebPlotRequest blueRequest, AsyncCallback<WebPlotResult> async) {
        List<Param> paramList = new ArrayList<Param>(4);
        if (redRequest != null) paramList.add(new Param(ServerParams.RED_REQUEST, redRequest.toString()));
        if (greenRequest != null) paramList.add(new Param(ServerParams.GREEN_REQUEST, greenRequest.toString()));
        if (blueRequest != null) paramList.add(new Param(ServerParams.BLUE_REQUEST, blueRequest.toString()));
        doPlotService(ServerParams.CREATE_PLOT, async, paramList);
    }

    public void getTableData(WebPlotRequest request, final AsyncCallback<WebPlotResult> async) {
        TableServerRequest sreq = null;
        if (request.getRequestType().equals(RequestType.RAWDATASET_PROCESSOR)) {
            String reqStr = request.getParam(ServerParams.REQUEST);
            if (reqStr != null) {
                sreq = TableServerRequest.parse(reqStr);
            } else {
                WebPlotResult wpr = WebPlotResult.makeFail("Internal Error", "Missing "+ServerParams.REQUEST+" parameter.", "Failed to get data set");
                async.onSuccess(wpr);
            }
        } else if (request.getRequestType().equals(RequestType.URL))  {
            TableServerRequest dataReq= new TableServerRequest("IpacTableFromSource");
            dataReq.setParam("source", request.getURL());
        } else {
            // todo
            WebPlotResult wpr = WebPlotResult.makeFail("Internal Error", "Not yet implemented", "Failed to get data set");
            async.onSuccess(wpr);
            throw new IllegalArgumentException("Not yet implemented");
        }
        if (sreq != null) {
            SearchServices.App.getInstance().getRawDataSet(sreq,
                    new BaseCallback<RawDataSet>() {
                        @Override
                        public void doSuccess(RawDataSet result) {
                            WebPlotResult wpr = new WebPlotResult();
                            wpr.putResult(WebPlotResult.RAW_DATA_SET, new DataEntry.RawDataSetResult(result));
                            async.onSuccess(wpr);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            async.onFailure(caught);
                        }
                    });

        }
    }

    public void deleteColorBand(PlotState state, Band band, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.REMOVE_BAND, async, state,
                      new Param(ServerParams.BAND, band.toString()));
    }

    public void addColorBand(PlotState state, WebPlotRequest bandRequest, Band band, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.ADD_BAND, async, state,
                      new Param(ServerParams.BAND, band.toString()),
                      new Param(ServerParams.REQUEST, bandRequest.toString()));
    }

    public void getFileFlux(FileAndHeaderInfo[] fileAndHeader, ImagePt inIpt, final AsyncCallback<String[]> async) {
        List<Param> paramList = new ArrayList<Param>(4);
        for (int i = 0; (i < fileAndHeader.length && i < 3); i++) {
            paramList.add(new Param(ServerParams.FILE_AND_HEADER + i, fileAndHeader[i].toString()));
        }
        paramList.add(new Param(ServerParams.PT, inIpt.toString()));

        JsonUtils.doService(doJsonP, ServerParams.FILE_FLUX, paramList, async, new JsonUtils.Converter<String[]>() {
            public String[] convert(String s) {
                return s.split(",");
            }
        });
    }

    public void getFlux(PlotState request, ImagePt inIpt, AsyncCallback<WebPlotResult> async) {
        // todo
    }

    public void setZoomLevel(PlotState stateAry[], float level, boolean isFullScreen, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.ZOOM, async, stateAry,
                      new Param(ServerParams.LEVEL, level + ""),
                      new Param(ServerParams.FULL_SCREEN, isFullScreen + ""));
    }

    public void deletePlot(String ctxStr, AsyncCallback<Boolean> async) {
        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.CTXSTR, ctxStr));

        JsonUtils.doService(doJsonP, ServerParams.DELETE, paramList, async, new JsonUtils.Converter<Boolean>() {
            public Boolean convert(String s) {
                return true;
            }
        });

    }

    public void recomputeStretch(PlotState state, StretchData[] stretchData, AsyncCallback<WebPlotResult> async) {
        List<Param> paramList = new ArrayList<Param>(4);
        paramList.add(new Param(ServerParams.STATE, state.toString()));
        for (int i = 0; (i < stretchData.length && i < 3); i++) {
            paramList.add(new Param(ServerParams.STRETCH_DATA + i, stretchData[i].toString()));
        }
        doPlotService(ServerParams.STRETCH, async, paramList);
    }

    public void crop(PlotState stateAry[],
                     ImagePt corner1,
                     ImagePt corner2,
                     boolean cropMultiAll,
                     final AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.CROP, async, stateAry,
                      new Param(ServerParams.PT1, corner1.toString()),
                      new Param(ServerParams.PT2, corner2.toString()),
                      new Param(ServerParams.CRO_MULTI_ALL, cropMultiAll+"") );
    }

    public void rotateNorth(PlotState stateAry[],
                            boolean north,
                            float newZoomLevel,
                            AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.ROTATE_NORTH, async, stateAry,
                      new Param(ServerParams.NORTH, north + ""),
                      new Param(ServerParams.ZOOM, newZoomLevel + "") );
    }

    public void rotateToAngle(PlotState state[],
                              boolean rotate,
                              double angle,
                              float newZoomLevel,
                              AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.ROTATE_ANGLE, async, state,
                      new Param(ServerParams.ROTATE, rotate + ""),
                      new Param(ServerParams.ANGLE, angle + ""),
                      new Param(ServerParams.ZOOM, newZoomLevel + "") );
    }

    public void flipImageOnY(PlotState stateAry[], AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.FLIP_Y, async, stateAry);
    }

    public void changeColor(PlotState state, int colorTableId, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.CHANGE_COLOR, async, state,
                      new Param(ServerParams.COLOR_IDX, colorTableId + ""));

    }

    public void getFitsHeaderInfo(PlotState state, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.HEADER, async, state);
    }

    public void getAreaStatistics(PlotState state, ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.STAT, async, state,
                      new Param(ServerParams.PT1, pt1 + ""),
                      new Param(ServerParams.PT2, pt2 + ""),
                      new Param(ServerParams.PT3, pt3 + ""),
                      new Param(ServerParams.PT4, pt4 + ""));
    }

    public void getColorHistogram(PlotState state, Band band, int width, int height, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.HISTOGRAM, async, state,
                      new Param(ServerParams.WIDTH, width + ""),
                      new Param(ServerParams.HEIGHT, height + ""),
                      new Param(ServerParams.BAND, band.toString()));
    }

    public void getImagePng(PlotState state, ArrayList<StaticDrawInfo> drawInfoList, AsyncCallback<WebPlotResult> async) {
        Param pAry[]= new Param[drawInfoList.size()];
        int i= 0;
        for(StaticDrawInfo ptList : drawInfoList) {
            pAry[i++]= new Param(ServerParams.DRAW_INFO, ptList.serialize());
        }
        doPlotService(ServerParams.IMAGE_PNG, async, state, pAry);
    }


    public void checkPlotProgress(String progressKey, AsyncCallback<WebPlotResult> async)  {
        doPlotService(ServerParams.PROGRESS, async,
                      new Param(ServerParams.PROGRESS_KEY, progressKey));

    }

    public void getDS9Region(String key, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.DS9_REGION, async,
                      new Param(ServerParams.FILE_KEY, key)
        );
    }

    public void saveDS9RegionFile(String regionData, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.SAVE_DS9_REGION, async,
                      new Param(ServerParams.REGION_DATA, regionData)
        );
    }

    public void addSavedRequest(String saveKey, WebPlotRequest request, AsyncCallback<Boolean> async) {

        List<Param> paramList = new ArrayList<Param>(3);
        paramList.add(new Param(ServerParams.SAVE_KEY, saveKey));
        paramList.add(new Param(ServerParams.REQUEST, request.toString()));

        JsonUtils.doService(doJsonP, ServerParams.ADD_SAVED_REQUEST, paramList, async, new JsonUtils.Converter<Boolean>() {
            public Boolean convert(String s) {
                return true;
            }
        });
    }

    public void getAllSavedRequest(String saveKey, AsyncCallback<WebPlotResult> async) {
        doPlotService(ServerParams.GET_ALL_SAVED_REQUEST, async,
                      new Param(ServerParams.SAVE_KEY, saveKey)
        );
    }

    //===================================================================================
    //---------------------- Utility Routines -------------------------------------------
    //===================================================================================



    private void doPlotService(String cmd, AsyncCallback<WebPlotResult> async, PlotState stateAry[], Param... paramAry) {
        List<Param> paramList = new ArrayList<Param>(8);
        for (int i = 0; (i < stateAry.length); i++) {
            paramList.add(new Param(ServerParams.STATE + i, stateAry[i].serialize()));
        }
        paramList.addAll(Arrays.asList(paramAry));
        doPlotService(cmd, async, paramList);
    }


    private void doPlotService(String cmd, AsyncCallback<WebPlotResult> async, PlotState state, Param... paramAry) {
        List<Param> paramList = new ArrayList<Param>(8);
        paramList.add(new Param(ServerParams.STATE, state.toString()));
        paramList.addAll(Arrays.asList(paramAry));
        doPlotService(cmd, async, paramList);
    }

    private void doPlotService(String cmd, AsyncCallback<WebPlotResult> async, Param... paramAry) {
        List<Param> paramList = new ArrayList<Param>(8);
        paramList.addAll(Arrays.asList(paramAry));
        doPlotService(cmd, async, paramList);
    }

    private void doPlotService(String cmd, AsyncCallback<WebPlotResult> async, List<Param> paramList) {

        if (doJsonP) {
            JsonUtils.jsonpRequest(cmd, paramList, new Acb(async));
        } else {
            try {
                JsonUtils.jsonRequest(cmd, paramList, new Rcb(async));
            } catch (RequestException e) {
                async.onFailure(e);
            }
        }
    }

    private static class Acb implements AsyncCallback<JsArray<PlotResultOverlay>> {
        AsyncCallback<WebPlotResult> cb;

        public Acb(AsyncCallback<WebPlotResult> cb) {
            this.cb = cb;
        }

        public void onFailure(Throwable e) {
            cb.onFailure(e);
        }

        public void onSuccess(JsArray<PlotResultOverlay> resultAry) {
            try {
                cb.onSuccess(WebPlotResultParser.convert(resultAry.get(0)));
            } catch (Exception e) {
                cb.onFailure(e);
            }
        }
    }

    private static class Rcb implements RequestCallback {
        AsyncCallback<WebPlotResult> cb;

        public Rcb(AsyncCallback<WebPlotResult> cb) {
            this.cb = cb;
        }

        public void onError(Request request, Throwable e) {
            cb.onFailure(e);
        }

        public void onResponseReceived(Request request, Response response) {
            try {
                cb.onSuccess(WebPlotResultParser.convert(response.getText()));
            } catch (Exception e) {
                cb.onFailure(e);
            }
        }
    }


}
