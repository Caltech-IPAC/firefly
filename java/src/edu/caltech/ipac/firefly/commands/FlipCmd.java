package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomUtil;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.util.ComparisonUtil;

import static edu.caltech.ipac.firefly.visualize.PlotState.RotateType;


public abstract class FlipCmd extends BaseVisCmd {

    public enum FlipDir { LEFT, RIGHT}
    private final MiniPlotWidget _mpw;
    final FlipDir _dir;

    public FlipCmd(String commandName, MiniPlotWidget mpw, FlipDir dir) {
        super(commandName,mpw.getPlotView());
        _mpw= mpw;
        _dir= dir;
    }


    protected void doExecute() {
        int pSize= _mpw.getPlotView().size();
        if (_mpw.getGroup().getLockRelated()) {
            for(MiniPlotWidget mpwItem : _mpw.getGroup().getAllActive()) {
                if (mpwItem==_mpw || mpwItem.getPlotView().size()==pSize) {
                    flip(mpwItem);
                }
            }

        }
        else {
            flip(_mpw);
        }
    }


    private void flip(MiniPlotWidget mpw) {
        WebPlotView pv= mpw.getPlotView();
        final WebPlot oldPlot= pv.getPrimaryPlot();
        int curr= pv.indexOf(oldPlot);
        int size= pv.size();
        int next= 0;
        if (_dir==FlipDir.RIGHT) {
            if (curr==size-1) next= 0;
            else next= curr+1;
        }
        else if (_dir==FlipDir.LEFT) {
            if (curr==0) next= size-1;
            else next= curr-1;
        }
        else {
            WebAssert.argTst(false, "should not happen");
        }
        final WebPlot newPlot= pv.getPlot(next);
        pv.setPrimaryPlot(pv.getPlot(next));

        doZoomAndRotation(mpw,pv, oldPlot, newPlot);

    }

    private void doZoomAndRotation(MiniPlotWidget mpw,WebPlotView pv, WebPlot oldPlot, WebPlot newPlot) {
        float oldArcsecPerPix= ZoomUtil.getArcSecPerPix(oldPlot);
        float newArcsecPerPix= ZoomUtil.getArcSecPerPix(newPlot);

        boolean byToFill= pv.containsAttributeKey(WebPlot.FLIP_ZOOM_TO_FILL);

        boolean byLevel= pv.containsAttributeKey(WebPlot.FLIP_ZOOM_BY_LEVEL) ||
                         Float.isNaN(oldArcsecPerPix) ||
                         Float.isNaN(newArcsecPerPix);

        if (byToFill) {
            Dimension dim= new Dimension(mpw.getOffsetWidth(), mpw.getOffsetHeight());
            float newLevel= ZoomUtil.getEstimatedFullZoomFactor(newPlot,dim);
            float currentNewLevel= newPlot.getZoomFact();
            if ( Math.abs(currentNewLevel-newLevel)>.01F) {
                enableZoomCallback(mpw,oldPlot,newPlot);
                pv.setZoomTo(newLevel,false);
            }
            else {
                doRotation(mpw,oldPlot,newPlot);
            }

        }
        else if (byLevel) {
            float oldLevel= oldPlot.getZoomFact();
            float newLevel= newPlot.getZoomFact();
            if (oldLevel!=newLevel) {
                enableZoomCallback(mpw,oldPlot,newPlot);
                pv.setZoomTo(oldLevel,false);
            }
            else {
                doRotation(mpw,oldPlot,newPlot);
            }
        }
        else if (Math.abs(oldArcsecPerPix-newArcsecPerPix)>.01) {
                enableZoomCallback(mpw,oldPlot,newPlot);
                pv.setZoomByArcsecPerScreenPix(oldArcsecPerPix,false);
        }
        else {
            doRotation(mpw,oldPlot,newPlot);
        }
    }

    private void enableZoomCallback(final MiniPlotWidget mpw,final WebPlot oldPlot, final WebPlot newPlot) {
        AllPlots.getInstance().addListener(Name.REPLOT,  new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                ReplotDetails details= (ReplotDetails)ev.getData();
                if (details.getReplotReason()== ReplotDetails.Reason.ZOOM_COMPLETED) {
                    AllPlots.getInstance().removeListener(Name.REPLOT, this);
                    doRotation(mpw,oldPlot,newPlot);
                }
            }
        });

    }

    private void doRotation(MiniPlotWidget mpw, WebPlot oldPlot, WebPlot newPlot) {

        RotateType rotType= oldPlot.getRotationType();
        double angle= oldPlot.getRotationAngle();
        boolean differentAngles= ComparisonUtil.equals(angle, newPlot.getRotationAngle());

        if (rotType!=newPlot.getRotationType() || (rotType==RotateType.ANGLE && differentAngles) ) {
            if (rotType==RotateType.ANGLE) {
                VisTask.getInstance().rotate(newPlot, true, angle, mpw);
            }
            else {
                mpw.setRotateNorth(rotType==RotateType.NORTH);
            }

        }
    }



    @Override
    public boolean isIE6IconBundleSafe() { return true; }

    @Override
    protected abstract Image createCmdImage();

}