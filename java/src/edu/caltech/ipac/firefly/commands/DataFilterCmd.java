/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;


public class DataFilterCmd extends BaseVisCmd{
    public final MiniPlotWidget mpw;
    public final boolean in;

    public DataFilterCmd(String cmdName, MiniPlotWidget mpw, boolean in) {
        super(cmdName, mpw.getPlotView());
        this.mpw = mpw;
        this.in= in;
        computeVisible();
        getPlotView().addListener(Name.AREA_SELECTION, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                computeEnabled();
                computeVisible();
            }
        });
        getPlotView().addListener(Name.LAYER_ITEM_ADDED, new WebEventListener() {
            public void eventNotify(WebEvent ev) { computeVisible();  }
        });
        getPlotView().addListener(Name.LAYER_ITEM_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) { computeVisible();  }
        });
    }

    protected void doExecute() {
        RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
        if (sel==null) {
            WebAssert.tst(false, "no RecSelection found in plot");
            //TODO: what should I do here instead of an assert
        }
        else {
            filter(getPlotView(), sel);
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

    protected void computeVisible() {
        RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
        WebPlot plot= getPlotView().getPrimaryPlot();
        boolean visible= false;
        if (sel!=null && plot!=null) {
            for(WebLayerItem item : getPlotView().getUserDrawerLayerSet())  {
                DrawingManager dMan= item.getDrawingManager();
                visible= dMan!=null &&
                        dMan.getSupportsAreaSelect()!= DataConnection.SelectSupport.NO &&
                        dMan.getSupportsFilter() &&
                        dMan.isDataInSelection(sel);
                if (visible) break;
            }
        }
        setHidden(!visible);
    }

    private void filter(WebPlotView pv, RecSelection sel) {
        if (sel!=null) {
            for(WebLayerItem item : pv.getUserDrawerLayerSet())  {
                DrawingManager dMan= item.getDrawingManager();
                if (dMan!=null && dMan.isDataInSelection(sel)) {
                    dMan.filter(sel,in);
                }
            }
            SelectAreaCmd cmd= (SelectAreaCmd) AllPlots.getInstance().getCommand(SelectAreaCmd.CommandName);
            if (cmd!=null) cmd.clearSelect();
        }
    }

}