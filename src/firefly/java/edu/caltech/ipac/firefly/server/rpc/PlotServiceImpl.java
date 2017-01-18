/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.server.ResourceManager;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.visualize.VisServerOps;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.ImageFileRetrieverFactory;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: roby
 * Date: Feb 25, 2008
 * Time: 1:22:01 PM
 */
public class PlotServiceImpl extends BaseRemoteService implements PlotService {



    public WebPlotResult getFlux(PlotState state, ImagePt inIpt)  {
        return VisServerOps.getFlux(state,inIpt);
    }


    public String[] getFileFlux(FileAndHeaderInfo fileAndHeader[],
                                ImagePt inIpt) {
        return VisServerOps.getFileFlux(fileAndHeader,inIpt);
    }

    public WebPlotResult changeColor(PlotState state, int colorTableId) {
        return VisServerOps.changeColor(state, colorTableId);
    }


    public WebPlotResult recomputeStretch(PlotState state,
                                          StretchData[] stretchData) {
        return VisServerOps.recomputeStretch(state, stretchData);
    }


    public WebPlotResult setZoomLevel(PlotState state[], float level, boolean isFullScreen) {
        return VisServerOps.setZoomLevel(state, level, isFullScreen);
    }

    public WebPlotResult crop(PlotState stateAry[],
                              ImagePt corner1,
                              ImagePt corner2,
                              boolean cropMultiAll) {
        return VisServerOps.crop(stateAry, corner1,corner2,cropMultiAll);
    }

    public WebPlotResult rotateNorth(PlotState state[], boolean north, float newZoomLevel) {
        return VisServerOps.rotateNorth(state,north,newZoomLevel);
    }

    public WebPlotResult rotateToAngle(PlotState state[], boolean rotate, double angle, float newZoomLevel) {
        return VisServerOps.rotateToAngle(state,rotate,angle, newZoomLevel);
    }

    public WebPlotResult flipImageOnY(PlotState state[]) {
        return VisServerOps.flipImageOnY(state);
    }

    public WebPlotResult getAreaStatistics(PlotState state,
                                           ImagePt pt1,
                                           ImagePt pt2,
                                           ImagePt pt3,
                                           ImagePt pt4){

        return VisServerOps.getAreaStatistics(state, pt1, pt2, pt3, pt4);
    }

    public WebPlotResult getFitsHeaderInfo(PlotState state){

        return VisServerOps.getFitsHeaderInfo(state);
    }


    public WebPlotResult getColorHistogram(PlotState state,
                                           Band band,
                                           int  width,
                                           int  height) {
        return VisServerOps.getColorHistogram(state, band,width,height);

    }


    public WebPlotResult getImagePng(PlotState state, ArrayList<StaticDrawInfo> drawInfoList) {
        return VisServerOps.getImagePng(state, drawInfoList);
    }


    public boolean deletePlot(String ctxStr) {
        return VisServerOps.deletePlot(ctxStr);
    }
    public WebPlotResult getTableData(WebPlotRequest request) {
        return getRawDataSetResult(request);
    }


//    public PlotCreationResult[] getWebPlotBatch(WebPlotRequest request[]) {
//        PlotCreationResult retval[]= new PlotCreationResult[request.length];
//        for(int i=0; (i<retval.length); i++) {
//            retval[i]= VisServerOps.createPlot(request[i]);
//        }
//        return retval;
//    }


    public WebPlotResult[] getWebPlotGroup(List<WebPlotRequest> requestList, String progressKey) {
        return VisServerOps.createPlotGroup(requestList,progressKey);
    }

    public WebPlotResult[] getOneFileGroup(List<WebPlotRequest> requestList, String progressKey) {
        return VisServerOps.createOneFileGroup(requestList);
    }

    public WebPlotResult getWebPlot(WebPlotRequest request){
        return VisServerOps.createPlot(request);
    }


    public WebPlotResult getWebPlot( WebPlotRequest redRequest,
                                    WebPlotRequest greenRequest,
                                    WebPlotRequest blueRequest) {
        return VisServerOps.create3ColorPlot(redRequest, greenRequest, blueRequest);
    }





    public WebPlotResult deleteColorBand(PlotState state, Band band) {
        return VisServerOps.deleteColorBand(state, band);
    }


    public WebPlotResult addColorBand(PlotState      state,
                                      WebPlotRequest bandRequest,
                                      Band band) {
        return VisServerOps.addColorBand(state, bandRequest, band);
    }


    public WebPlotResult checkPlotProgress(String progressKey) {
        return VisServerOps.checkPlotProgress(progressKey);
    }

    public WebPlotResult getDS9Region(String fileKey) {
        return VisServerOps.getDS9Region(fileKey);
    }

    public WebPlotResult saveDS9RegionFile(String regionData) {
        return VisServerOps.saveDS9RegionFile(regionData);
    }

    public boolean addSavedRequest(String saveKey, WebPlotRequest request) {
        return VisServerOps.addSavedRequest(saveKey,request);
    }

    public WebPlotResult getAllSavedRequest(String saveKey) {
        return VisServerOps.getAllSavedRequest(saveKey);
    }

    //=====================================
    //=========== New ends here
    //=====================================


    // wrapper method to return RawDataSet for XY plots
    private WebPlotResult getRawDataSetResult(WebPlotRequest request){
        WebPlotResult result;
        RawDataSet rds;
        try {
            if (request.getRequestType().equals(RequestType.RAWDATASET_PROCESSOR)) {
                String reqStr = request.getParam(ServerParams.REQUEST);
                if (reqStr != null) {
                    TableServerRequest sreq = TableServerRequest.parse(reqStr);
                    rds = (new SearchManager().getRawDataSet(sreq));
                } else {
                    result = WebPlotResult.makeFail("Internal Error", "Missing "+ServerParams.REQUEST+" parameter.", "Failed to get data set");
                    return result;
                }
            } else {
                File f= null;
                FileRetriever retrieve= ImageFileRetrieverFactory.getRetriever(request);
                if (retrieve!=null)  {
                    FileInfo retf= retrieve.getFile(request);
                    f= retf.getFile();
                }
                if (f==null) throw new IOException("file is null");
                rds = (new ResourceManager()).getIpacTableView(f);
                rds.getMeta().setSource(ServerContext.replaceWithPrefix(f));
            }
            result = new WebPlotResult();
            result.putResult(WebPlotResult.RAW_DATA_SET, new DataEntry.RawDataSetResult(rds));
        } catch (SecurityException e) {
            result= WebPlotResult.makeFail("No access", "You do not have access to this data,",e.getMessage());
        } catch (Throwable th) {
            result= WebPlotResult.makeFail("Server Error, Please Report", th.getMessage(), "Failed to get table from URL");
        }
        return result;
    }
}