package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 9/4/14
 * Time: 3:54 PM
 */


import edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter;
import edu.caltech.ipac.firefly.fuse.data.PlotData;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;

/**
 * @author Trey Roby
 */
public class ImageSelectPanelConverterPlotter implements ImageSelectPanelPlotter {



    private static final String USER_ADDED= "UserAddImage-";
    private static int userAddedCnt= 0;
    private final DatasetInfoConverter info;

    private String currID= null;


    public void showing(List<PlotTypeUI> plotTypeUIList) {
         String id= getCurrentPlotID();
        for(PlotTypeUI pt : plotTypeUIList) {
            if (pt instanceof DataConvPlotTypeUI) {
                ((DataConvPlotTypeUI)pt).reinitUI(id);
            }
        }
    }

    public ImageSelectPanelConverterPlotter(DatasetInfoConverter info) {
        this.info= info;
    }

    public void doPlot(PlotWidgetOps ops, PlotTypeUI ptype, boolean createNew, boolean threeColor, Band threeColorBand) {
        currID= getCurrentPlotID();
        DatasetInfoConverter info= getCurrentConverter();
        if (currID!=null || info!=null) {
            plotUsingID(ptype, threeColor, threeColorBand);
        }
        else {
            plotManual(AllPlots.getInstance().getMiniPlotWidget(), ptype, threeColor,threeColorBand);
        }
    }


    public void remove3ColorIDBand(Band removeBand) {
        AllPlots.getInstance().getMiniPlotWidget().getOps().removeColorBand(removeBand);
    }

    public void plotUsingID(PlotTypeUI ptype, boolean threeColor, Band band) {
        PlotData plotData= info.getPlotData();
        if (ptype.isThreeColor()) {
            if (ptype instanceof DataConvPlotTypeUI) {
                List<String> idList= ((DataConvPlotTypeUI)ptype).getThreeColorIDs();
                plotData.set3ColorIDOfIDs(currID, idList);
                plotData.fireUpdate();
            }
        }
        else {
            WebPlotRequest request= ptype.createRequest();
            if (threeColor) {
                WebPlotRequest reqAry[]= new WebPlotRequest[] {null, null, null};
                reqAry[band.getIdx()]= request;

                if (useAddBand(band)) {
                    plotData.set3ColorIDBand(currID, band, request);
                }
                else {
                    if (ptype instanceof DataConvPlotTypeUI) {
                        List<String> idList= ((DataConvPlotTypeUI)ptype).getThreeColorIDs();
                        plotData.set3ColorIDOfIDs(currID,idList);
                    }
                    else {
                        plotManual(AllPlots.getInstance().getMiniPlotWidget(), ptype, threeColor,band);
                    }
                }
            }
            else {
                plotData.setID(currID, request);
            }
        }
    }

    public void plotManual(MiniPlotWidget mpw, PlotTypeUI ptype, boolean threeColor, Band threeColorBand) {
        PlotWidgetOps ops= mpw.getOps();
        if (threeColor) {
            WebPlotRequest request[]= ptype.createThreeColorRequest();
            for(WebPlotRequest r : request) {
                if (r!=null) {
                    r.setTitle(ptype.getDesc() + " 3 Color");
                    int width= mpw.getOffsetWidth();
                    if (r.getZoomType()== ZoomType.TO_WIDTH) {
                        if (width>50)  r.setZoomToWidth(width);
                        else  r.setZoomType(ZoomType.SMART);
                    }
                }
            }
            ops.plot3Color(request[0], request[1], request[2],false,null);
        }
        else {
            WebPlotRequest request= ptype.createRequest();
            if (StringUtils.isEmpty(request.getTitle())) request.setTitle(ptype.getDesc());
            if (threeColor) {
                if (useAddBand(threeColorBand)) {
                    ops.addColorBand(request,threeColorBand,null);
                }
                else {
                    ops.plot3Color(request, threeColorBand, false,false, null);
                }
            }
            else {
                ops.plot(request,false,false, null);
            }
        }
    }


    private String getCurrentPlotID() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        return pv!=null ? (String)pv.getAttribute(WebPlotView.GRID_ID) : null;
    }

    private DatasetInfoConverter getCurrentConverter() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        return pv!=null ? (DatasetInfoConverter)pv.getAttribute(WebPlotView.DATASET_INFO_CONVERTER) : null;
    }


    private boolean useAddBand(Band band) {
        WebPlot plot= getCurrentPlot();
        boolean retval= false;
        if (plot!=null) {
            int numBands= plot.getBands().length;
            retval=  (plot.isThreeColor() && numBands>0 &&
                    !(numBands==1 && band==plot.getFirstBand()));
        }
        return retval;
    }

    public WebPlot getCurrentPlot() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        return (pv==null) ? null : pv.getPrimaryPlot();
    }

    public boolean isCreateNewVisible() {
        return false;
    }

    public PlotTypeUI[] getExtraPanels() {
        return new PlotTypeUI[] {new DatasetPlotTypeUI(info) }; // todo reuse
    }
}

