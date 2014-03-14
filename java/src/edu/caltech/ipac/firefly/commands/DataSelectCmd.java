package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;
import edu.caltech.ipac.firefly.visualize.draw.DrawingManager;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;


public class DataSelectCmd extends BaseVisCmd{
    public static final String CommandName= "DataSelect";
    public final MiniPlotWidget mpw;

    public DataSelectCmd(MiniPlotWidget mpw) {
        super(CommandName, mpw.getPlotView());
        this.mpw = mpw;
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
            if (isSelectionTooBig(getPlotView(), sel) ) {
                PopupUtil.showInfo("Your data set is too large to select. You must filter it down first.");
            }
            else {
                doSelection(getPlotView(), sel);
            }
        }
    }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals("DataSelect.Icon"))  {
                return new Image(ic.getSelectRows());
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

    protected void computeVisible() {
        WebPlot plot= getPlotView().getPrimaryPlot();
        boolean visible= false;
        RecSelection sel= (RecSelection)getPlotView().getAttribute(WebPlot.SELECTION);
        if (sel!=null && plot!=null) {
            for(WebLayerItem item : getPlotView().getUserDrawerLayerSet())  {
                DrawingManager dMan= item.getDrawingManager();
                visible= dMan!=null &&
                        dMan.getSupportsAreaSelect()!= DataConnection.SelectSupport.NO &&
                        dMan.isDataInSelection(sel);
                if (visible) break;
            }
        }
        setHidden(!visible);
    }


    private boolean isSelectionTooBig(WebPlotView pv, RecSelection sel) {
        boolean retval= false;
        if (sel!=null) {
            for(WebLayerItem item : pv.getUserDrawerLayerSet())  {
                DrawingManager dMan= item.getDrawingManager();
                retval= dMan!=null &&
                        dMan.isDataInSelection(sel) &&
                        dMan.getSupportsAreaSelect()== DataConnection.SelectSupport.TOO_BIG;
                if (retval) break;
            }
        }
        return retval;
    }


    private void doSelection(WebPlotView pv, RecSelection sel) {
        if (sel!=null) {
            for(WebLayerItem item : pv.getUserDrawerLayerSet())  {
                DrawingManager dMan= item.getDrawingManager();
                if (dMan!=null && dMan.isDataInSelection(sel)) {
                    dMan.select(sel);
                }
            }
            pv.getEventManager().fireEvent(new WebEvent(this, Name.DATA_SELECTION_CHANGE));
        }
    }

}