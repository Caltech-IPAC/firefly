/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 8/26/15
 * Time: 1:12 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.visualize.OverlayPlotView;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.task.PlotMaskTask;

/**
 * @author Trey Roby
 */
public class MaskAdjust {

    public static void addMask(final WebPlotView pv,
                               int maskValue,
                               int imageNumber,
                               String color,
                               String bitDesc) {

        WebPlot primary= pv.getPrimaryPlot();
        if (primary==null) return;
        WebPlotRequest r= makeRequest(pv,maskValue,imageNumber, color);
        final OverlayPlotView opv= new OverlayPlotView(pv, maskValue,imageNumber, color,bitDesc);

        PlotMaskTask.plot(r, opv, new AsyncCallback<WebPlot>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(WebPlot result) {
                pv.addWebLayerItem(opv.getWebLayerItem());
                pv.addDrawingArea(opv,false);
            }
        });

    }

    public static void updateMask(final OverlayPlotView opv,
                                  final WebPlotView pv,
                                  int maskValue,
                                  int imageNumber,
                                  String color ) {

        WebPlot primary= pv.getPrimaryPlot();
        if (primary==null) return;

        WebPlotRequest r= makeRequest(pv,maskValue,imageNumber, color);


        PlotMaskTask.plot(r, opv, new AsyncCallback<WebPlot>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(WebPlot result) {
            }
        });

    }

    private static WebPlotRequest makeRequest(WebPlotView pv, int maskValue, int imageNumber, String color) {
        WebPlot primary= pv.getPrimaryPlot();
        PlotState state= primary.getPlotState();
        WebPlotRequest or= state.getPrimaryRequest();

        WebPlotRequest r= or.makeCopy();

        r.setMaskBits(maskValue);
        r.setPlotAsMask(true);
        r.setMaskColors(new String[]{ color, "#FF0000", "#FFFF00", "#FF00FF"});
        r.setMaskRequiredWidth(primary.getImageWidth());
        r.setMaskRequiredHeight(primary.getImageHeight());
        r.setMultiImageIdx(imageNumber);


        r.setZoomType(ZoomType.STANDARD);
        r.setInitialZoomLevel(state.getZoomLevel());
        r.setRotate(false);
        r.setRotateNorth(false);
        r.setRotationAngle(0);
        r.setFlipX(false);
        r.setFlipY(false);
        if (primary.isRotated()) {
            PlotState.RotateType rt= primary.getRotationType();
            if (rt== PlotState.RotateType.NORTH) {
                r.setRotateNorth(true);
            }
            else if (rt== PlotState.RotateType.ANGLE) {
                r.setRotate(true);
                r.setRotationAngle(primary.getRotationAngle());
            }
        }
        return r;
    }

}
