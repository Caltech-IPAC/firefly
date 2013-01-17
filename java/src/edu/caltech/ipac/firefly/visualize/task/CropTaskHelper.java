package edu.caltech.ipac.firefly.visualize.task;

import com.google.gwt.user.client.Window;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.visualize.plot.ImagePt;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:49:37 PM
 */


/**
 * @author Trey Roby
*/
public class CropTaskHelper {
    private final WebPlot _oldPlot;
    private final MiniPlotWidget _mpw;
    private final ImagePt _pt1;
    private final ImagePt _pt2;
    private final String _newTitle;


    public CropTaskHelper(WebPlot plot,
                   String newTitle,
                   ImagePt pt1,
                   ImagePt pt2,
                   MiniPlotWidget mpw) {
        _oldPlot = plot;
        _mpw= mpw;
        _pt1= pt1;
        _pt2= pt2;
        _newTitle= newTitle;
    }

    public void handleFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();

        }
        Window.alert("Crop Failed: Server Error: " + extra);
    }

    public void handleSuccess(WebPlotResult result) {
        _mpw.hideMouseReadout();
        try {
            if (result.isSuccess()) {
                CreatorResults cr=
                        (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);
                WebPlotInitializer wpInit= cr.getInitializers()[0];

                WebPlot cropPlot= new WebPlot(wpInit);
                _mpw.getOps().removeCurrentPlot();
                TaskUtils.copyImportantAttributes(_oldPlot, cropPlot);
                _mpw.getPlotView().addPlot(cropPlot);
                _mpw.postPlotTask(_newTitle, cropPlot, null);
                _mpw.forcePlotPrefUpdate();
            }
            else {
                Window.alert("Plot Failed: "+ result.getUserFailReason());
            }
        } catch (Exception e) {
            Window.alert("exception: "+ e.toString());
        }
    }

    public MiniPlotWidget getMiniPlotWidget() { return _mpw; }
    public ImagePt getPt1() { return _pt1; }
    public ImagePt getPt2() { return _pt2; }
    public PlotState getPlotState() { return _oldPlot.getPlotState(); }

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
