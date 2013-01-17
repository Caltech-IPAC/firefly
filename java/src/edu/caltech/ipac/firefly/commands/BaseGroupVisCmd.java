package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetGroup;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;

import java.util.List;


public abstract class BaseGroupVisCmd extends GeneralCommand {

    public BaseGroupVisCmd(String commandName) {
        super(commandName);
        plotViewListener();
        setEnabled(computeEnabled());
    }

    @Override
    public boolean hasIcon() { return true; }

    public WebPlotView getPlotView() { return AllPlots.getInstance().getPlotView(); }
    public MiniPlotWidget getMiniPlotWidget() { return AllPlots.getInstance().getMiniPlotWidget(); }
    public PlotWidgetGroup getGroup() { return AllPlots.getInstance().getActiveGroup(); }
    public List<MiniPlotWidget> getActiveList() {  return AllPlots.getInstance().getActiveList(); }


    protected boolean computeEnabled() {
        WebPlotView pv=getPlotView();
        WebPlot plot= (pv==null) ? null : pv.getPrimaryPlot();
        boolean retval= false;
        if (plot!=null) {
            retval= true;
        }
        return retval;
    }

    private void plotViewListener() {
        AllPlots.getInstance().getEventManager().addListener(new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                setEnabled(computeEnabled());
            }
        });
    }
}