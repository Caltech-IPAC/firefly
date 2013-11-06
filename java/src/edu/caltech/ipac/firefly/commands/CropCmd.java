package edu.caltech.ipac.firefly.commands;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.visualize.plot.ImagePt;


public class CropCmd extends BaseVisCmd {
    public static final String CommandName= "crop";
    public final MiniPlotWidget _plotWidget;

    public CropCmd(MiniPlotWidget plotWidget) {
        super(CommandName,plotWidget.getPlotView());
        _plotWidget= plotWidget;
    }


    protected void doExecute() {
        final WebPlot plot= getPlotView().getPrimaryPlot();
        final RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
        if (sel==null) {
            WebAssert.tst(false, "no RecSelection found in plot");
        }
        if (plot.isCube()) {
            PopupUtil.showInfo(null, "Can't crop cube", "We do not yet support cropping cubes");
        }
        else if (plot.getPlotState().isMultiImageFile(plot.getFirstBand()) &&
                 plot.getPlotView().isMultiImageFitsWithSameArea()) {
            PopupUtil.showConfirmMsg(null, "Multi Image Crop",
                                     "Do you want to crop all the images in this FITS file together?",
                                     new ClickHandler() {
                                         public void onClick(ClickEvent event) { doCrop(plot, sel, true); }
                                     },
                                     new ClickHandler() {
                                         public void onClick(ClickEvent event) { doCrop(plot, sel, false); }
                                     });
        }
        else {
            doCrop(plot, sel, false);
        }
    }

    private void doCrop(WebPlot plot, RecSelection sel, boolean cropMultiAll) {
        try {
            ImagePt pt0= plot.getImageCoords(sel.getPt0());
            ImagePt pt1= plot.getImageCoords(sel.getPt1());
            if (pt0!=null && pt1!=null) {
                VisTask.getInstance().crop(getPlotView().getMiniPlotWidget(),pt0, pt1, cropMultiAll);
            }
            else {
                PopupUtil.showError("Can't Crop", "Crop failed for this selection");
            }
        } catch (Exception e) {
            PopupUtil.showError("Can't Crop", "Crop failed for this selection");
        }

    }

    protected boolean computeEnabled() {
        WebPlot plot= getPlotView().getPrimaryPlot();
        boolean retval= false;
        if (plot!=null) {
            RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
            retval= super.computeEnabled() && sel!=null;
        }
        return retval;
    }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null && iStr.equals("crop.Icon"))  {
                return new Image(ic.getCrop());
        }
        return null;
    }
}