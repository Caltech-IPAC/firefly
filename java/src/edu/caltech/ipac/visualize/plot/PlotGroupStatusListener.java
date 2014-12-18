package edu.caltech.ipac.visualize.plot;

import java.util.EventListener;

/**
 * A listener that is called when a PlotGroup status has a changed
 * @author Trey Roby
 */
public interface PlotGroupStatusListener extends EventListener {

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void plotAdded(PlotGroupStatusEvent ev);

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void plotRemoved(PlotGroupStatusEvent ev);

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void colorBandAdded(PlotGroupStatusEvent ev);

    /**
     * Called when a plot is added to this PlotGroup
     *
     * @param ev the event
     */
    public abstract void colorBandRemoved(PlotGroupStatusEvent ev);

    /**
     * Called when a color band in this PlotGroup's plot is shown
     *
     * @param ev the event
     */
    public abstract void colorBandShowing(PlotGroupStatusEvent ev);

    /**
     * Called when a color band in this PlotGroup's plot is hidden
     *
     * @param ev the event
     */
    public abstract void colorBandHidden(PlotGroupStatusEvent ev);


    /**
     * Called when a plot in this PlotGroup is shown
     *
     * @param ev the event
     */
    public abstract void plotShowing(PlotGroupStatusEvent ev);

    /**
     * Called when a plot in this PlotGroup is hidden
     *
     * @param ev the event
     */
    public abstract void plotHidden(PlotGroupStatusEvent ev);

}
