package edu.caltech.ipac.firefly.visualize.task;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotView;

import java.util.logging.Level;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:49:37 PM
 */


/**
 * @author Trey Roby
*/
public class RotateTaskHelper {

    private static final NumberFormat _nf = NumberFormat.getFormat("#.#");
    private final WebPlot _oldPlot;
    private final MiniPlotWidget _mpw;
    private final PlotState.RotateType _rotateType;
    private final double _angle;
    private final float _newZoomLevel;


    public RotateTaskHelper(WebPlot plot,
                            PlotState.RotateType rotateType,
                            float newZoomLevel,
                            double angle,
                            MiniPlotWidget mpw) {
        _oldPlot = plot;
        _mpw= mpw;
        _angle= angle;
        _newZoomLevel= newZoomLevel;
        _rotateType= rotateType;
    }

    public static String makeMessage(PlotState.RotateType rotateType, double angle) {
        String retval= "undefined";
        switch (rotateType) {
            case NORTH: retval= "Rotating North...";
                       break;
            case ANGLE: retval=  "Rotating to " + _nf.format(angle) + " degrees ...";
                       break;
            case UNROTATE: retval= "Returning to original rotation...";
                       break;
        }
        return retval;
    }

    public MiniPlotWidget getMiniPlotWidget() { return _mpw; }
    public PlotState getPlotState() { return _oldPlot.getPlotState(); }
    public PlotState.RotateType getType() { return _rotateType; }
    public double getAngle() { return _angle; }
    public float getNewZoomLevel() { return _newZoomLevel; }

    public void handleFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();

        }
        Window.alert("Rotate Failed: Server Error: " + extra);
    }

    public void handleSuccess(WebPlotResult result) {
        _mpw.hideMouseReadout();
        try {
            if (result.isSuccess()) {
                GwtUtil.getClientLogger().log(Level.INFO, "Request angle: "+ _angle+ ", before rotation angle: " + VisUtil.getRotationAngle(_oldPlot));
                CreatorResults cr=
                        (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);
                WebPlotInitializer wpInit= cr.getInitializers()[0];
                WebPlot rotPlot= new WebPlot(wpInit);
                TaskUtils.copyImportantAttributes(_oldPlot, rotPlot);

                WebPlotView pv= _mpw.getPlotView();
                WebPlot oldPlot= pv.getPrimaryPlot();
                int idx= pv.indexOf(oldPlot);
                pv.removePlot(oldPlot,true);

                rotPlot.setPlotDesc(_oldPlot.getPlotDesc());
                pv.addPlot(rotPlot,idx,false);
                pv.setPrimaryPlot(rotPlot);
                _mpw.postPlotTask(rotPlot, null);
                _mpw.forcePlotPrefUpdate();
                GwtUtil.getClientLogger().log(Level.INFO, "Request angle: "+ _angle+ ", after rotation angle: " + VisUtil.getRotationAngle(rotPlot));
            }
            else {
                PopupUtil.showError("Rotate", "Could not rotate: " + result.getUserFailReason());
            }
        } catch (Exception e) {
            PopupUtil.showError("Rotate", "Could not rotate: " + e.toString());
            GWT.log("Could not rotate", e);
        }
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

