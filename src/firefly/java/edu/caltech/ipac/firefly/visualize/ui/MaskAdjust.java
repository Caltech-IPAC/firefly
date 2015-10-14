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
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.OverlayPlotView;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.task.PlotMaskTask;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class MaskAdjust {



    public static void addMask(String maskId,
                               String plotId,
                               int bitNumber,
                               int imageNumber,
                               String color,
                               String bitDesc,
                               String fileKey) {
        if (plotId==null) return;
        MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidgetById(plotId);
        if (mpw==null || mpw.getPlotView()==null | mpw.getCurrentPlot()==null) return;

        if (bitDesc==null) bitDesc= "bit #"+bitNumber;
        if (color==null) color= "#FF0000";
        int maskValue= (int)Math.pow(2,bitNumber);
        addMask(maskId,mpw.getPlotView(),maskValue,imageNumber,color,bitDesc,fileKey);

    }



    public static void addMask(String maskId,
                               final WebPlotView pv,
                               int maskValue,
                               int imageNumber,
                               String color,
                               String bitDesc,
                               String fileKey) {

        WebPlot primary= pv.getPrimaryPlot();
        if (primary==null) return;
        PlotState state= primary.getPlotState();
        WebPlotRequest or= state.getPrimaryRequest();
        WebPlotRequest r= makeRequest(fileKey, pv,maskValue,imageNumber, color,or);
        final OverlayPlotView opv= new OverlayPlotView(maskId, pv, maskValue,imageNumber, color,bitDesc,r);

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

    public static void removeMask(String maskId) {
       OverlayPlotView.deleteById(maskId);
    }



    public static void updateMask(final OverlayPlotView opv,
                                  final WebPlotView pv,
                                  int maskValue,
                                  int imageNumber,
                                  String color,
                                  WebPlotRequest originalRequest) {

        WebPlot primary= pv.getPrimaryPlot();
        if (primary==null) return;

        WebPlotRequest r= makeRequest(null, pv,maskValue,imageNumber, color, originalRequest);


        PlotMaskTask.plot(r, opv, new AsyncCallback<WebPlot>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(WebPlot result) {
            }
        });

    }

    private static WebPlotRequest makeRequest(String fileKey,
                                              WebPlotView pv,
                                              int maskValue,
                                              int imageNumber,
                                              String color,
                                              WebPlotRequest or) {
        WebPlot primary= pv.getPrimaryPlot();
        PlotState state= primary.getPlotState();

        WebPlotRequest r= or.makeCopy();
        if (!StringUtils.isEmpty(fileKey)) {
            r.setRequestType(RequestType.FILE);
            r.setFileName(fileKey);
        }

        r.setMaskBits(maskValue);
        r.setPlotAsMask(true);
        r.setMaskColors(new String[]{color});
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
