package edu.caltech.ipac.firefly.commands;

import com.google.gwt.core.client.Scheduler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetGroup;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;

import java.util.List;


public abstract class BaseGroupVisCmd extends GeneralCommand {

    private static Scheduler scheduler= Scheduler.get();
    private DeferEnableCheck deferCheck= new DeferEnableCheck();

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
    public List<MiniPlotWidget> getGroupActiveList() {  return AllPlots.getInstance().getActiveList(); }
    public List<MiniPlotWidget> getAllActivePlots() {  return AllPlots.getInstance().getAll(true); }


    protected boolean computeEnabled() {
        WebPlotView pv=getPlotView();
        WebPlot plot= (pv==null) ? null : pv.getPrimaryPlot();
        boolean retval= false;
        if (plot!=null) {
            retval= true;
        }

        if (retval) {
            PopoutWidget pw= AllPlots.getInstance().getExpandedSingleWidget();
            retval= (pw==null || pw instanceof MiniPlotWidget);
        }
        return retval;
    }

    private void plotViewListener() {
        AllPlots.getInstance().addListener(new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                scheduler.scheduleDeferred(deferCheck);
            }
        });
    }

    private class DeferEnableCheck implements Scheduler.ScheduledCommand {
        public void execute() {
            setEnabled(computeEnabled());
        }
    }
}