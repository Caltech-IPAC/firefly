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

import java.util.Arrays;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ImageSelectPanelConverterPlotter implements ImageSelectPanelPlotter {



    private static final String USER_ADDED= "UserAddImage-";
    private static int userAddedCnt= 0;
    private final DatasetInfoConverter info;

    private String currID= null;


    public void showing() {
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

            }
            else {
//                plotManual(ptype,threeColor,band); //todo: enable plot manual
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
                    plotData.set3ColorIDRequest(currID,Arrays.asList(reqAry) );
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
