package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;


public class DataUnselectCmd extends BaseVisCmd{
    public static final String CommandName= "DataUnselect";
    public final MiniPlotWidget mpw;

    public DataUnselectCmd(MiniPlotWidget mpw) {
        super(CommandName, mpw.getPlotView());
        this.mpw = mpw;
        computeVisible();
        getPlotView().addListener(Name.AREA_SELECTION, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                computeEnabled();
                computeVisible();
            }
        });
        getPlotView().addListener(Name.DATA_SELECTION_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) { computeVisible();  }
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
            doUnSelection(getPlotView());
        }
    }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals("DataUnselect.Icon"))  {
                return new Image(ic.getUnselectRows());
            }
        }
        return null;
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

    public void computeVisible() {
        WebPlot plot= getPlotView().getPrimaryPlot();
        boolean visible= false;
        if (plot!=null) {
            for(WebLayerItem item : getPlotView().getUserDrawerLayerSet())  {
                DrawingManager dMan= item.getDrawingManager();
                visible= dMan!=null && dMan.getSelectedCount()>0;
                if (visible) break;
            }
        }
        setHidden(!visible);
    }



    private void doUnSelection(WebPlotView pv) {
        for(WebLayerItem item : pv.getUserDrawerLayerSet())  {
            DrawingManager dMan= item.getDrawingManager();
            if (dMan!=null) dMan.select(null);
        }
    }

}