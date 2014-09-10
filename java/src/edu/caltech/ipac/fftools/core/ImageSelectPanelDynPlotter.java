package edu.caltech.ipac.fftools.core;
/**
 * User: roby
 * Date: 9/4/14
 * Time: 3:54 PM
 */


import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.fuse.data.PlotData;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectPanelPlotter;
import edu.caltech.ipac.firefly.visualize.ui.PlotTypeUI;

import java.util.Arrays;

/**
 * @author Trey Roby
 */
public class ImageSelectPanelDynPlotter implements ImageSelectPanelPlotter {



    private static final String USER_ADDED= "UserAddImage-";
    private static int userAddedCnt= 0;
    private final PlotData defDynPlotData;

    private String currID= null;





    public ImageSelectPanelDynPlotter(PlotData defDynPlotData) {
        this.defDynPlotData = defDynPlotData;
    }

    public void doPlot(PlotWidgetOps ops, PlotTypeUI ptype, boolean createNew, boolean threeColor, Band threeColorBand) {
        if (createNew || !isCreateNewVisible()) {
            currID= makeID();
            plotUsingID(ptype, threeColor, threeColorBand);
        }
        else {
            currID= getCurrentPlotID();
            if (currID!=null) {
                plotUsingID(ptype, threeColor, threeColorBand);
            }
            else {
                plotManual(AllPlots.getInstance().getMiniPlotWidget(), ptype);
            }
        }
    }


    public void remove3ColorIDBand(Band removeBand) {
        defDynPlotData.remove3ColorIDBand(getCurrentPlotID(), removeBand);
    }

    public void plotUsingID(PlotTypeUI ptype, boolean threeColor, Band band) {
        if (ptype.isThreeColor()) {
            WebPlotRequest request[]= ptype.createThreeColorRequest();
            defDynPlotData.set3ColorIDRequest(currID, Arrays.asList(request));
        }
        else {
            WebPlotRequest request= ptype.createRequest();
            if (threeColor) {
                WebPlotRequest reqAry[]= new WebPlotRequest[] {null, null, null};
                reqAry[band.getIdx()]= request;

                if (useAddBand(band)) {
                    defDynPlotData.set3ColorIDBand(currID, band, request);
                }
                else {
                    defDynPlotData.set3ColorIDRequest(currID, Arrays.asList(reqAry));
                }
            }
            else {
                defDynPlotData.setID(currID, request);
            }
        }
    }

    private String makeID() {
        String id= USER_ADDED+userAddedCnt;
        userAddedCnt++;
        return id;
    }

    public void plotManual(MiniPlotWidget mpw, PlotTypeUI ptype) {
        Vis.init(mpw, new Vis.InitComplete() {
            public void done() {
                // todo: add plotting code
            }
        });

    }

    private String getCurrentPlotID() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        return pv!=null ? (String)pv.getAttribute(WebPlotView.GRID_ID) : null;
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
        return defDynPlotData.hasPlotsDefined();
    }

    public void showing() {
        ((FFToolsStandaloneCreator)Application.getInstance().getCreator()).getStandaloneUI().ensureDynImageTabShowing();
    }

    public PlotTypeUI[] getExtraPanels() { return new PlotTypeUI[0]; }
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
