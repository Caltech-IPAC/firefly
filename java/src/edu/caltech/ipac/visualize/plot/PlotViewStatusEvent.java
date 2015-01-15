/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.util.EventObject;

/**
 * The event that is passed when a PlotView status has a changed
 * @author Trey Roby
 */
public class PlotViewStatusEvent extends EventObject {

    Plot _plot;
    /**
     * Create a new PlotViewStatusEvent
     * @param source source of the event.
     * @param plot the plot added or removed
     */
    public PlotViewStatusEvent (Object source, Plot plot) {
        super(source);
        _plot= plot;
    }

    public Plot getPlot() {return _plot;}


    public Object getSource() {
        return super.getSource();
    }

}



