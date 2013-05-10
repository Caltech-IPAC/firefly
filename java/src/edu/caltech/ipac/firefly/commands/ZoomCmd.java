package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.ZoomUtil;
import edu.caltech.ipac.firefly.visualize.ui.ZoomOptionsPopup;


public abstract class ZoomCmd extends BaseGroupVisCmd {
    public static final long CLICK_TIME= 200;
    private static final float MAX_ZOOM= 63;

    private long _lastClick= 0;
    private final WebPlot.ZDir dir;
    private final ZoomUtil.FitFill fitFill;

    public ZoomCmd(String commandName,  WebPlot.ZDir dir) {
        super(commandName);
        WebAssert.argTst(dir!=null, "dir must be non-null");
        this.dir = dir;
        this.fitFill= null;
    }

    public ZoomCmd(String commandName,  ZoomUtil.FitFill fitFill) {
        super(commandName);
        WebAssert.argTst(fitFill!=null, "fitFill must be non-null");
        this.dir = null;
        this.fitFill= fitFill;
    }


    protected void doExecute() {
        long time= System.currentTimeMillis();
        long deltaClick= time-_lastClick;
        _lastClick= time;

        if (dir!=null) {
            if (isZoomMax()) {
                ZoomOptionsPopup.showZoomOps("You may not zoom beyond " + ZoomUtil.getZoomMax() + "x", true);
            }
            else if (isSizeMax()) {
                ZoomOptionsPopup.showZoomOps("You have reached the maximum size you can zoom this image",true);
            }
            else if (deltaClick < CLICK_TIME) {
                ZoomOptionsPopup.showZoomOps();
            }
            else {
                ZoomUtil.zoomGroup(dir);
                AllPlots.getInstance().fireEvent( new WebEvent<String>(this, Name.ZOOM_LEVEL_BUTTON_PUSHED,getName()));
            }
        }
        else if (fitFill!=null) {
            ZoomUtil.zoomGroup(fitFill);
        }

    }


    public boolean isZoomMax() {
        boolean retval= false;
        if (dir ==WebPlot.ZDir.UP || dir ==WebPlot.ZDir.ORIGINAL) {
            for(MiniPlotWidget mpw : getGroupActiveList()) {
                WebPlot p= mpw.getCurrentPlot();
                if (p!=null) {
                    int intZoomFact= (int)p.getZoomFact();
                    if (intZoomFact>1 && intZoomFact==ZoomUtil.getZoomMax()) {
                        retval= true;
                        break;
                    }
                }
            }
        }
        return retval;
    }


    public boolean isSizeMax() {
        boolean retval= false;
        float zf=1F;
        if (getMiniPlotWidget()!=null && getMiniPlotWidget().getCurrentPlot()!=null) {
            zf= getMiniPlotWidget().getCurrentPlot().getZoomFact();
        }

        if (dir ==WebPlot.ZDir.UP || (dir ==WebPlot.ZDir.ORIGINAL && 1>zf)) {
            for(MiniPlotWidget mpw : getGroupActiveList()) {
                WebPlot p= mpw.getCurrentPlot();
                if (p!=null) {
                    long w= p.getScreenWidth()*2;
                    long h= p.getScreenHeight()*2;
                    retval= (w*h > (15000L*15000L));
                    if (retval) break;
                }
            }
        }
        return retval;
    }



    @Override
    public boolean isIE6IconBundleSafe() { return true; }

    @Override
    protected abstract Image createCmdImage();

}