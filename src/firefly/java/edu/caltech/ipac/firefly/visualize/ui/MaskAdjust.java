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
import edu.caltech.ipac.firefly.visualize.MaskPlotView;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.firefly.visualize.task.PlotMaskTask;

/**
 * @author Trey Roby
 */
public class MaskAdjust {

    public static void addOrUpdateMask(final WebPlotView pv, int maskValue, int imageNumber) {

        WebPlot primary= pv.getPrimaryPlot();
        if (primary==null) return;

        PlotState state= primary.getPlotState();
        WebPlotRequest or= state.getPrimaryRequest();

        WebPlotRequest r= or.makeCopy();

        r.setMaskBits(maskValue);
        r.setPlotAsMask(true);
        r.setMaskColors(new String[]{ "#FF0000", "#00FF00", "#FFFF00", "#FF00FF"});
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
        final MaskPlotView mpv= new MaskPlotView(pv);

        PlotMaskTask.plot(r, mpv, new AsyncCallback<WebPlot>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(WebPlot result) {
                WebPlotView.MouseAll ma= new WebPlotView.DefMouseAll();
                WebPlotView.MouseInfo mi= new WebPlotView.MouseInfo(ma,"put more help here");
                WebLayerItem item= new WebLayerItem("mask-overlay",null, "Mask", "Put Help with Mask Here",mpv,mi,null,null);
                pv.addWebLayerItem(item);
                pv.addDrawingArea(mpv,false);
            }
        });

    }


}
