package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.visualize.plot.ImagePt;


public class CatalogWithSelectionCmd extends BaseVisCmd {
    public static final String CommandName= "catalogWithSelection";
    public final MiniPlotWidget _plotWidget;

    public CatalogWithSelectionCmd(MiniPlotWidget plotWidget) {
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
                ImagePt pt2= plot.getImageCoords(sel.getPt1());
                if (pt0!=null && pt2!=null) {
                    ImagePt pt1= new ImagePt(pt2.getX(), pt0.getY());
                    ImagePt pt3= new ImagePt(pt0.getX(), pt2.getY());
                    ActiveTarget.getInstance().setImageCorners(sel.getPt0(),
                                                               plot.getWorldCoords(pt1),
                                                               sel.getPt1(),
                                                               plot.getWorldCoords(pt3) );
                    GeneralCommand cmd= Application.getInstance().getCommand(IrsaCatalogDropDownCmd.COMMAND_NAME);
                    if (cmd!=null) cmd.execute();
                }
                else {
                    PopupUtil.showError("Can't Crop", "Crop failed for this selection");
                }

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
        if (iStr!=null && iStr.equals("catalogWithSelection.Icon"))  {
                return new Image(ic.getCatalogWithSelection());
        }
        return null;
    }
}