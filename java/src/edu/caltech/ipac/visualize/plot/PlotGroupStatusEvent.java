/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.util.EventObject;

/**
 * The event that is passed when a PlotView status has a changed
 * @author Trey Roby
 */
public class PlotGroupStatusEvent extends EventObject {

    private Plot _plot;
    private int  _colorBand= -1;

    /**
     * Create a new PlotViewStatusEvent
     * @param plotGroup source of the event.
     * @param plot the plot added or removed
     */
    public PlotGroupStatusEvent (PlotGroup plotGroup, Plot plot) {
        this(plotGroup, plot, -1);
    }
    /**
     * Create a new PlotViewStatusEvent
     * @param plotGroup source of the event.
     * @param plot the plot added or removed
     */
    public PlotGroupStatusEvent (PlotGroup plotGroup, Plot plot, int band) {
        super(plotGroup);
        _plot= plot;
        _colorBand= band;
    }

    public Plot getPlot() {return _plot;}
    public int  getColorBand() {return _colorBand;}
}
