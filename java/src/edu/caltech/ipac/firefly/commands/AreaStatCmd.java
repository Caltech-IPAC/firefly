/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.visualize.plot.ImagePt;


public class AreaStatCmd extends BaseVisCmd{
    public static final String CommandName= "AreaStat";
    public final MiniPlotWidget _plotWidget;

    public AreaStatCmd(MiniPlotWidget plotWidget) {
        super(CommandName,plotWidget.getPlotView());
        _plotWidget= plotWidget;
    }

    protected void doExecute() {
        WebPlot plot= getPlotView().getPrimaryPlot();
        RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
        if (sel==null) {
            WebAssert.tst(false, "no RecSelection found in plot");
            //TODO: what should I do here instead of an assert
        }
        else {
            computeStatistics(plot, sel);
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
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals("AreaStat.Icon"))  {
                return new Image(ic.getStatistics());
            }
        }
        return null;
    }



    private void computeStatistics(WebPlot plot, RecSelection sel) {
        ImagePt pt0= plot.getImageCoords(sel.getPt0());
        ImagePt pt2= plot.getImageCoords(sel.getPt1());
        if (pt0!=null && pt2!=null) {
            ImagePt pt1= new ImagePt(pt2.getX(), pt0.getY());
            ImagePt pt3= new ImagePt(pt0.getX(), pt2.getY());
            _plotWidget.getOps().getAreaStatistics(pt0,pt1,pt2,pt3);
        }
        else {
            PopupUtil.showError("Can't compute Stats", "Stats failed for this selection");
        }
    }

}