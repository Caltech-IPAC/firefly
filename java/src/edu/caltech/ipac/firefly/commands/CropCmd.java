package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.visualize.plot.ImagePt;


public class CropCmd extends BaseVisCmd {
    public static final String CommandName= "crop";
    public final MiniPlotWidget _plotWidget;

    public CropCmd(MiniPlotWidget plotWidget) {
        super(CommandName,plotWidget.getPlotView());
        _plotWidget= plotWidget;
    }


    protected void doExecute() {
        WebPlot plot= getPlotView().getPrimaryPlot();
        RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
        if (sel==null) {
            WebAssert.tst(false, "no RecSelection found in plot");
        }
        else {
            try {
                ImagePt pt0= plot.getImageCoords(sel.getPt0());
                ImagePt pt1= plot.getImageCoords(sel.getPt1());
                _plotWidget.getOps().crop(pt0,pt1);
            } catch (Exception e) {
                PopupUtil.showError("Can't Crop", "Crop failed for this selection");
            }
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