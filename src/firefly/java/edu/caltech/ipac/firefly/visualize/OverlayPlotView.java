/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.draw.LayerDrawer;

import static edu.caltech.ipac.firefly.visualize.ReplotDetails.Reason;

/**
 * *
 */
public class OverlayPlotView extends Composite implements WebEventListener, LayerDrawer {


    private WebPlotView pv;
    private AbsolutePanel rootPanel= new AbsolutePanel();
    private WebPlot      maskPlot;
    private WebPlotRequest maskRequest;
    private String defaultColor= "ff0000";

    /**
     *
     */
    public OverlayPlotView(WebPlotView pv) {
        this.pv= pv;
        initWidget(rootPanel);
        rootPanel.setStyleName("OverlayPlotView");
        initGraphics();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) maskPlot.refreshWidget();
    }

    @Override
    public boolean hasData() { return true; }

    @Override
    public String getDefaultColor() { return defaultColor; }

    @Override
    public void setDefaultColor(String c) { defaultColor= c;
    }

    @Override
    public boolean getSupportsRegions() { return false; }

    public boolean isImageOverlay() { return true; }

    public void freeResources() {
        rootPanel.clear();
        if (maskPlot!=null) {
            maskPlot.freeResources();
            maskPlot= null;
        }
    }

    public void setPixelSize(int width, int height) {
        rootPanel.setPixelSize(width,height);
        // todo- how to a respond now? do I need to do anything?
        super.setPixelSize(width, height);
    }

    private void initGraphics() {
        pv.addListener(Name.REPLOT, this);
//        pv.addListener(Name.VIEW_PORT_CHANGE, this);
        pv.addListener(Name.PRIMARY_PLOT_CHANGE, this);
        pv.addDrawingArea(this, false);

    }

    public void clear() {
        // todo
    }

    public void dispose() {
        pv.removeDrawingArea(this);
        pv.removeOverlayPlot(maskPlot);
    }

    public void setMaskPlot(WebPlot maskPlot) {
        if (maskPlot==null) return;
        if (this.maskPlot!=null) {
            this.maskPlot.freeResources();
            pv.removeOverlayPlot(this.maskPlot);
        }
        maskPlot.getPlotGroup().setPlotView(pv);
        rootPanel.clear();
        rootPanel.add(maskPlot.getWidget(),0,0);
        this.maskPlot= maskPlot;
        pv.addOverlayPlot(maskPlot);
    }

    public void replotMask() {
        WebPlot primary= pv.getPrimaryPlot();
        if (primary==null) return;
//        maskRequest.setZoomType(ZoomType.STANDARD);
//        maskRequest.setInitialZoomLevel(primary.getPlotState().getZoomLevel());
//        maskRequest.setRotate(false);
//        maskRequest.setRotateNorth(false);
//        maskRequest.setRotationAngle(0);
//        maskRequest.setFlipX(false);
//        maskRequest.setFlipY(false);
//        if (primary.isRotated()) {
//            PlotState.RotateType rt= primary.getRotationType();
//            if (rt== PlotState.RotateType.NORTH) {
//                maskRequest.setRotateNorth(true);
//            }
//            else if (rt== PlotState.RotateType.ANGLE) {
//                maskRequest.setRotate(true);
//                maskRequest.setRotationAngle(primary.getRotationAngle());
//            }
//        }
//        //todo need to test for flipped
//        PlotMaskTask.plot(maskRequest,null,this);
    }

    @Override
    public void eventNotify(WebEvent ev) {
        Name name= ev.getName();
        if (name.equals(Name.PRIMARY_PLOT_CHANGE)) {
            if (pv.getPrimaryPlot()==null) {
                clear();
            }
            else {
                maskPlot.refreshWidget();
            }
        }
//        else if (name.equals(Name.VIEW_PORT_CHANGE)) {
//            if (pv.getPrimaryPlot()!=null) maskPlot.refreshWidget();
//        }
        else if (name.equals(Name.REPLOT)) {
            ReplotDetails details= (ReplotDetails)ev.getData();
            ReplotDetails.Reason reason= details.getReplotReason();
            if (reason== Reason.IMAGE_RELOADED ||  reason== Reason.ZOOM ||
                    reason== Reason.ZOOM_COMPLETED ||  reason== Reason.REPARENT) {
                replotMask();
            }
        }
    }

}
