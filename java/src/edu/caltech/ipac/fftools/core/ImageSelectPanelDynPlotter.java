/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.fftools.core;
/**
 * User: roby
 * Date: 9/4/14
 * Time: 3:54 PM
 */


import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.fuse.PlotData;
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
import java.util.List;

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

    public void showing(List<PlotTypeUI> plotTypeUIList) {
        ((FFToolsStandaloneCreator)Application.getInstance().getCreator()).getStandaloneUI().ensureDynImageTabShowing();
    }

    public PlotTypeUI[] getExtraPanels() { return new PlotTypeUI[0]; }
}

